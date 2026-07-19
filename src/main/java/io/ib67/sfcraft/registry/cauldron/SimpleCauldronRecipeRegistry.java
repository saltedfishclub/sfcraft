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
    public int indexOf(CauldronRecipe recipe) {
        return recipes.indexOf(recipe);
    }

    @Override
    @Nullable
    public CauldronRecipe byIndex(int index) {
        return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
    }

}
