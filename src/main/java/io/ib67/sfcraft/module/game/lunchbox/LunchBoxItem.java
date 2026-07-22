package io.ib67.sfcraft.module.game.lunchbox;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public class LunchBoxItem extends Item implements PolymerItem {
    private static final int NO_FOOD = -1;
    private static final int NOT_HUNGRY = -2;

    public LunchBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        // 碗:非方块(不会出现放置鬼影),且没有自定义 use 行为,
        // 客户端右键会走 consumable 组件路径,进食动画预测完整保留
        return Items.BOWL;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipFlag tooltipFlag, PacketContext context, HolderLookup.Provider lookup) {
        var clientStack = PolymerItem.super.getPolymerItemStack(stack, tooltipFlag, context, lookup);
        // 把 food/consumable 组件带给原版客户端,右键时就会播放原版进食动画
        var food = stack.get(DataComponents.FOOD);
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (food != null) clientStack.set(DataComponents.FOOD, food);
        if (consumable != null) clientStack.set(DataComponents.CONSUMABLE, consumable);
        return clientStack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS_SERVER;
        if (player.isShiftKeyDown()) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new LunchBoxMenu(containerId, inventory, stack),
                    Component.translatable("container.sfcraft.lunch_box")));
            return InteractionResult.SUCCESS_SERVER;
        }
        int slot = findEdibleSlot(serverPlayer, loadContents(stack));
        if (slot == NO_FOOD) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.lunch_box.empty"));
            return InteractionResult.FAIL;
        }
        if (slot == NOT_HUNGRY) {
            return InteractionResult.FAIL;
        }
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return InteractionResult.FAIL;
        // 走原版进食流程(动画+咀嚼音效),结束后进入下方 finishUsingItem
        return consumable.startConsuming(player, stack, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        var items = loadContents(stack);
        int slot = findEdibleSlot(player, items);
        if (slot >= 0) {
            // finishUsingItem 应用盒内那份食物的营养/饱和/效果/余留物(碗、瓶留回格子)
            var remainder = items.get(slot).finishUsingItem(level, player);
            items.set(slot, remainder);
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
        return stack;
    }

    private static NonNullList<ItemStack> loadContents(ItemStack lunchBox) {
        NonNullList<ItemStack> items = NonNullList.withSize(LunchBoxMenu.SIZE, ItemStack.EMPTY);
        lunchBox.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    private static int findEdibleSlot(ServerPlayer player, NonNullList<ItemStack> items) {
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
}
