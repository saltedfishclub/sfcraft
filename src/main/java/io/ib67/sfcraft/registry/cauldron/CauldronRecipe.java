package io.ib67.sfcraft.registry.cauldron;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A reaction inside the amethyst cauldron: all {@code inputs} (ingredients and the
 * catalyst alike) must be thrown into the cauldron. Once the contents match and the
 * water/heat conditions are met, the reaction starts automatically.
 *
 * <p>{@code id} is a stable identifier used to persist an in-progress reaction across
 * save/reload — it must be unique and stable, unlike list position, so that
 * re-registering recipes (e.g. on {@code /sfcraft reload}) or changing the recipe set
 * never makes a saved cauldron resume the wrong reaction.
 */
public record CauldronRecipe(
        String id,
        List<Predicate<ItemStack>> inputs,
        boolean needsWater,
        boolean needsHeat,
        ParticleOptions reactionParticle,
        int reactionTicks,
        Function<List<ItemStack>, ItemStack> assembler
) {
    public boolean matches(List<ItemStack> contents) {
        if (contents.size() != inputs.size()) return false;
        return matchRemaining(contents, new boolean[contents.size()], 0);
    }

    private boolean matchRemaining(List<ItemStack> contents, boolean[] used, int index) {
        if (index == inputs.size()) return true;
        for (int i = 0; i < contents.size(); i++) {
            if (used[i] || !inputs.get(index).test(contents.get(i))) continue;
            used[i] = true;
            if (matchRemaining(contents, used, index + 1)) return true;
            used[i] = false;
        }
        return false;
    }
}
