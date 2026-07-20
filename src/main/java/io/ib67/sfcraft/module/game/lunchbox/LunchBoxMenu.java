package io.ib67.sfcraft.module.game.lunchbox;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 午餐盒的 9 格容器菜单(GENERIC_9x1,原版客户端原生支持)。
 * 只允许放入带 food 组件的物品;每次变动立即回写到物品的 container 组件,防止刷物品。
 */
public class LunchBoxMenu extends AbstractContainerMenu {
    public static final int SIZE = 9;

    private final ItemStack lunchBox;
    private final SimpleContainer container;

    public LunchBoxMenu(int containerId, Inventory playerInventory, ItemStack lunchBox) {
        super(MenuType.GENERIC_9x1, containerId);
        this.lunchBox = lunchBox;
        this.container = new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                lunchBox.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
            }
        };
        NonNullList<ItemStack> loaded = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        lunchBox.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(loaded);
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, loaded.get(i));
        }

        for (int col = 0; col < SIZE; col++) {
            addSlot(new Slot(container, col, 8 + col * 18, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isFood(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 49 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 107));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stackInSlot = slot.getItem();
        var original = stackInSlot.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stackInSlot, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!isFood(stackInSlot)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stackInSlot, 0, SIZE, false)) return ItemStack.EMPTY;
        }
        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == lunchBox || player.getOffhandItem() == lunchBox;
    }

    // 午餐盒自身也带 food 组件(为了进食动画),要排除掉,防止套娃
    private static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && !(stack.getItem() instanceof LunchBoxItem);
    }
}
