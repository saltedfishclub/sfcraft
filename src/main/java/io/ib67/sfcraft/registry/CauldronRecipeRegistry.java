package io.ib67.sfcraft.registry;

import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Registry of amethyst cauldron reactions. Feature modules contribute their recipes in
 * {@code onInitialize()} and re-register them on {@link io.ib67.sfcraft.callback.SFConfigReload}.
 */
public interface CauldronRecipeRegistry {
    void register(CauldronRecipe recipe);

    void unregister(CauldronRecipe recipe);

    Optional<CauldronRecipe> match(List<ItemStack> contents);

    /** Stable index used to persist the running reaction in the block entity. */
    int indexOf(CauldronRecipe recipe);

    @Nullable
    CauldronRecipe byIndex(int index);
}
