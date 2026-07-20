package io.ib67.sfcraft.module.game.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;

/**
 * custom item 的 lore 行工厂。lore 渲染的基样式是紫色斜体(ItemLore.LORE_STYLE),
 * 不覆盖颜色/斜体会很难看,统一从这里出灰色非斜体的行。
 */
public final class ItemLores {
    private ItemLores() {
    }

    public static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args)
                .withColor(CommonColors.GRAY)
                .withStyle(style -> style.withItalic(false));
    }
}
