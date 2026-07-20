package io.ib67.sfcraft.registry.cauldron;

import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleCauldronRecipeRegistry implements CauldronRecipeRegistry {
    private final List<CauldronRecipe> recipes = new CopyOnWriteArrayList<>();

    @Override
    public void register(CauldronRecipe recipe) {
        recipes.add(recipe);
    }

    @Override
    public void unregister(CauldronRecipe recipe) {
        recipes.remove(recipe);
    }

    @Override
    public Optional<CauldronRecipe> match(List<ItemStack> contents) {
        return recipes.stream().filter(recipe -> recipe.matches(contents)).findFirst();
    }

    @Override
    @Nullable
    public CauldronRecipe byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (CauldronRecipe recipe : recipes) {
            if (recipe.id().equals(id)) return recipe;
        }
        return null;
    }
}
