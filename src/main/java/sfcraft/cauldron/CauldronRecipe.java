package sfcraft.cauldron;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A reaction inside the amethyst cauldron: all {@code ingredients} must be present
 * (thrown in as drops), then the {@code catalyst} item is applied by hand.
 */
public record CauldronRecipe(
        List<Predicate<ItemStack>> ingredients,
        Predicate<ItemStack> catalyst,
        boolean needsWater,
        boolean needsHeat,
        ParticleOptions reactionParticle,
        int reactionTicks,
        Function<List<ItemStack>, ItemStack> assembler
) {
    public boolean matches(List<ItemStack> contents, ItemStack catalystStack) {
        return catalyst.test(catalystStack) && matchesContents(contents);
    }

    private boolean matchesContents(List<ItemStack> contents) {
        if (contents.size() != ingredients.size()) return false;
        return matchRemaining(contents, new boolean[contents.size()], 0);
    }

    private boolean matchRemaining(List<ItemStack> contents, boolean[] used, int index) {
        if (index == ingredients.size()) return true;
        for (int i = 0; i < contents.size(); i++) {
            if (used[i] || !ingredients.get(index).test(contents.get(i))) continue;
            used[i] = true;
            if (matchRemaining(contents, used, index + 1)) return true;
            used[i] = false;
        }
        return false;
    }
}
