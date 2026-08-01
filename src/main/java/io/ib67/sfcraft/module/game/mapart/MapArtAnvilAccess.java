package io.ib67.sfcraft.module.game.mapart;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@code AnvilMenuMixin} 实现的鸭子接口,把铁砧菜单的内部输入/名称/结果槽暴露给 {@link MapArtModule}。
 */
public interface MapArtAnvilAccess {
    /** 输入槽 0 的物品。 */
    ItemStack sfcraft$getInput();

    /** 当前输入的重命名文本(null = 未输入)。 */
    @Nullable String sfcraft$getItemName();

    /** 覆写结果槽与所需经验等级,并同步客户端。 */
    void sfcraft$setResult(ItemStack stack, int xpCost);
}
