package io.ib67.sfcraft.module.game.lunchbox;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.ib67.sfcraft.config.GameConfigService;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 午餐盒:伪装成原版收纳袋,背包/容器 GUI 内通过原版袋式交互存取(左键整组装入、右键取出一组、
 * 滚轮/拖拽选中),世界里右键直接吃盒内食物。容量按"组数"计(见 gameplay.json 的 lunchBox 节)。
 */
public class LunchBoxItem extends Item implements PolymerItem {
    private final GameConfigService gameConfig;

    public LunchBoxItem(Properties properties, GameConfigService gameConfig) {
        super(properties);
        this.gameConfig = gameConfig;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        // 恒为原版收纳袋:tooltip 网格、拖拽/滚轮选中、GUI 点击预测全部原生生效
        return Items.BUNDLE;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipFlag tooltipFlag, PacketContext context, HolderLookup.Provider lookup) {
        var clientStack = PolymerItem.super.getPolymerItemStack(stack, tooltipFlag, context, lookup);
        // 客户端 tooltip 网格/选中靠 BUNDLE_CONTENTS 驱动,必须显式复制给伪装栈
        var contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) clientStack.set(DataComponents.BUNDLE_CONTENTS, contents);
        return clientStack;
    }

    // ---------- 收纳袋交互(镜像原版 BundleItem,容量规则替换为"组数"上限) ----------

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction clickAction, Player player) {
        if (self.get(DataComponents.BUNDLE_CONTENTS) == null) return false;
        var other = slot.getItem();
        if (clickAction == ClickAction.PRIMARY && !other.isEmpty()) {
            // 袋在手,左键槽位:整组移入
            if (transferFromSlot(self, slot, player) > 0) {
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            broadcastChanges(player);
            return true;
        }
        if (clickAction == ClickAction.SECONDARY && other.isEmpty()) {
            // 袋在手,右键空槽:取出一组放入槽位
            var removed = removeOne(self);
            if (removed != null) {
                var remainder = slot.safeInsert(removed);
                if (!remainder.isEmpty()) {
                    // 槽位没全吃掉,塞回盒内(盒内本就允许该堆存在)
                    insertBoxContents(self, remainder);
                } else {
                    playRemoveOneSound(player);
                }
            }
            broadcastChanges(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack carried, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedAccess) {
        if (clickAction == ClickAction.PRIMARY && carried.isEmpty()) {
            return false;
        }
        if (self.get(DataComponents.BUNDLE_CONTENTS) == null) return false;
        if (clickAction == ClickAction.PRIMARY) {
            // 袋在槽位,左键手上物品:整组装入
            if (!slot.allowModification(player) || insertBoxContents(self, carried) <= 0) {
                playInsertFailSound(player);
            } else {
                playInsertSound(player);
            }
            broadcastChanges(player);
            return true;
        }
        if (clickAction == ClickAction.SECONDARY && carried.isEmpty()) {
            // 袋在槽位,空手右键:取出一组到手上
            if (slot.allowModification(player)) {
                var removed = removeOne(self);
                if (removed != null) {
                    playRemoveOneSound(player);
                    carriedAccess.set(removed);
                }
            }
            broadcastChanges(player);
            return true;
        }
        return false;
    }

    /** 盒在手时左键槽位:先按容量算好可入盒数量再 safeTake,杜绝部分移动导致刷物品 */
    private int transferFromSlot(ItemStack box, Slot slot, Player player) {
        var other = slot.getItem();
        int canInsert = canInsertCount(box, other);
        if (canInsert <= 0) return 0;
        var taken = slot.safeTake(other.getCount(), canInsert, player);
        if (taken.isEmpty()) return 0;
        int inserted = insertBoxContents(box, taken);
        if (!taken.isEmpty()) {
            slot.safeInsert(taken); // 理论不可达,兜底放回
        }
        return inserted;
    }

    /** 不改任何状态,仅计算 stack 能塞进盒子多少个 */
    private int canInsertCount(ItemStack box, ItemStack stack) {
        if (!canBeInBox(stack)) return 0;
        int maxStacks = gameConfig.get().lunchBox.maxStacks;
        var items = loadItems(box);
        int room = 0;
        if (stack.isStackable()) {
            for (var existing : items) {
                if (existing.getCount() < existing.getMaxStackSize()
                        && ItemStack.isSameItemSameComponents(existing, stack)) {
                    room += existing.getMaxStackSize() - existing.getCount();
                }
            }
        }
        room += Math.max(0, maxStacks - items.size()) * stack.getMaxStackSize();
        return Math.min(stack.getCount(), room);
    }

    /** 把 stack 塞进盒子(并堆优先,新堆插最前以保持 LIFO 取出),返回实际塞入数量并 shrink stack */
    private int insertBoxContents(ItemStack box, ItemStack stack) {
        if (!canBeInBox(stack)) return 0;
        int maxStacks = gameConfig.get().lunchBox.maxStacks;
        var items = loadItems(box);
        int remaining = stack.getCount();
        if (stack.isStackable()) {
            for (int i = 0; i < items.size() && remaining > 0; i++) {
                var existing = items.get(i);
                if (existing.getCount() < existing.getMaxStackSize()
                        && ItemStack.isSameItemSameComponents(existing, stack)) {
                    int move = Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
                    items.set(i, existing.copyWithCount(existing.getCount() + move));
                    remaining -= move;
                }
            }
        }
        while (remaining > 0 && items.size() < maxStacks) {
            int move = Math.min(remaining, stack.getMaxStackSize());
            items.add(0, stack.copyWithCount(move));
            remaining -= move;
        }
        int inserted = stack.getCount() - remaining;
        if (inserted > 0) {
            stack.shrink(inserted);
            saveItems(box, items);
        }
        return inserted;
    }

    /** 取出选中(或最近放入)的一整组,selection 随之复位 */
    @Nullable
    private ItemStack removeOne(ItemStack box) {
        var contents = box.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty()) return null;
        var items = loadItems(box);
        int index = contents.getSelectedItemIndex();
        if (index < 0 || index >= items.size()) index = 0;
        var removed = items.remove(index);
        saveItems(box, items);
        return removed;
    }

    private boolean canBeInBox(ItemStack stack) {
        if (!BundleContents.canItemBeInBundle(stack)) return false;
        if (stack.getItem() instanceof LunchBoxItem) return false; // 禁止套娃
        if (gameConfig.get().lunchBox.foodOnly && !stack.has(DataComponents.FOOD)) return false;
        return true;
    }

    private static List<ItemStack> loadItems(ItemStack box) {
        return box.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                .itemCopyStream().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void saveItems(ItemStack box, List<ItemStack> items) {
        box.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items.stream()
                .filter(s -> !s.isEmpty())
                .map(ItemStackTemplate::fromNonEmptyStack)
                .toList()));
    }

    private void broadcastChanges(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) menu.slotsChanged(player.getInventory());
    }

    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private static void playRemoveOneSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    // ---------- 世界内右键进食(服务端权威,营养来自盒内食物) ----------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS_SERVER;
        var items = loadItems(stack);
        int slot = findEdibleSlot(serverPlayer, items);
        if (slot == NO_FOOD) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.lunch_box.empty"));
            return InteractionResult.FAIL;
        }
        if (slot == NOT_HUNGRY) {
            return InteractionResult.FAIL;
        }
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return InteractionResult.FAIL;
        return consumable.startConsuming(player, stack, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        var items = loadItems(stack);
        int slot = findEdibleSlot(player, items);
        if (slot >= 0) {
            // finishUsingItem 应用盒内那份食物的营养/饱和/效果/余留物(碗、瓶留回格子)
            var remainder = items.get(slot).finishUsingItem(level, player);
            items.set(slot, remainder);
            saveItems(stack, items);
        }
        return stack;
    }

    private static final int NO_FOOD = -1;
    private static final int NOT_HUNGRY = -2;

    // 午餐盒自身也带 food 组件(物品注册里为了进食流程),要排除掉,防止吃盒里套的盒
    private static int findEdibleSlot(ServerPlayer player, List<ItemStack> items) {
        boolean hasFood = false;
        for (int i = 0; i < items.size(); i++) {
            var food = items.get(i);
            if (food.isEmpty() || food.getItem() instanceof LunchBoxItem) continue;
            var foodProperties = food.get(DataComponents.FOOD);
            if (foodProperties == null) continue;
            hasFood = true;
            if (player.canEat(foodProperties.canAlwaysEat())) return i;
        }
        return hasFood ? NOT_HUNGRY : NO_FOOD;
    }

    /** 盒在世界中损毁(下界合金不保)时把内容物洒出来,与原版收纳袋一致 */
    @Override
    public void onDestroyed(ItemEntity entity) {
        var contents = entity.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            entity.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            ItemUtils.onContainerDestroyed(entity, contents.itemCopyStream());
        }
    }
}
