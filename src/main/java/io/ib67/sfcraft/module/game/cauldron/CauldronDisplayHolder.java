package io.ib67.sfcraft.module.game.cauldron;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Floating item displays showing what's inside the amethyst cauldron.
 */
public class CauldronDisplayHolder extends ElementHolder {
    private static final Vec3[] OFFSETS = {
            new Vec3(0.0, 0.0, 0.0),
            new Vec3(0.18, 0.03, 0.0),
            new Vec3(-0.09, 0.06, 0.16),
            new Vec3(-0.09, 0.09, -0.16),
    };
    private static final int SYNC_INTERVAL = 10;

    private final ServerLevel level;
    private final BlockPos pos;
    private final ItemDisplayElement[] elements = new ItemDisplayElement[OFFSETS.length];
    private final ItemStack[] shownStacks = new ItemStack[OFFSETS.length];

    public CauldronDisplayHolder(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (level.getGameTime() % SYNC_INTERVAL != 0) return;
        List<ItemStack> contents = level.getBlockEntity(pos) instanceof AmethystCauldronBlockEntity cauldron
                ? cauldron.getContents()
                : List.of();
        for (int i = 0; i < elements.length; i++) {
            var stack = i < contents.size() ? contents.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                if (elements[i] != null) {
                    removeElement(elements[i]);
                    elements[i] = null;
                    shownStacks[i] = null;
                }
                continue;
            }
            if (elements[i] == null) {
                var element = new ItemDisplayElement();
                element.setOffset(OFFSETS[i]);
                element.setScale(new Vector3f(0.35f, 0.35f, 0.35f));
                element.setItemDisplayContext(ItemDisplayContext.GROUND);
                elements[i] = addElement(element);
            }
            if (shownStacks[i] == null || !ItemStack.matches(shownStacks[i], stack)) {
                shownStacks[i] = stack.copy();
                elements[i].setItem(stack.copyWithCount(1));
            }
        }
    }
}
