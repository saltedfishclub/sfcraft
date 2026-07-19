package sfcraft.cauldron;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import sfcraft.SFItems;
import sfcraft.items.PearlTokenItem;
import sfcraft.items.ReversePearlTokenItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CauldronRecipes {
    private static final List<CauldronRecipe> RECIPES = new ArrayList<>();

    private CauldronRecipes() {
    }

    public static void register(CauldronRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static Optional<CauldronRecipe> match(List<ItemStack> contents) {
        return RECIPES.stream().filter(recipe -> recipe.matches(contents)).findFirst();
    }

    public static int indexOf(CauldronRecipe recipe) {
        return RECIPES.indexOf(recipe);
    }

    @Nullable
    public static CauldronRecipe byIndex(int index) {
        return index >= 0 && index < RECIPES.size() ? RECIPES.get(index) : null;
    }

    /** 重新构建配方表(/sfcraft reload 时调用,使 reactionTicks 等配置生效)。 */
    public static void rebuild() {
        RECIPES.clear();
        bootstrap();
    }

    public static void bootstrap() {
        // 锅里丢入: 已绑定的珍珠信物 + 回响碎片(催化剂) + 水 + 加热 → 反向珍珠信物
        register(new CauldronRecipe(
                List.of(
                        stack -> stack.is(SFItems.PEARL_TOKEN) && PearlTokenItem.getOwnerId(stack) != null,
                        stack -> stack.is(Items.ECHO_SHARD)),
                true,
                true,
                new DustColorTransitionOptions(0x006D6D, 0x000000, 1.0F),
                sfcraft.GameConfig.get().cauldron.reactionTicks,
                contents -> {
                    var token = contents.stream()
                            .filter(stack -> stack.is(SFItems.PEARL_TOKEN))
                            .findFirst()
                            .orElse(ItemStack.EMPTY);
                    var ownerId = PearlTokenItem.getOwnerId(token);
                    if (ownerId == null) return ItemStack.EMPTY;
                    return ReversePearlTokenItem.createBound(ownerId, PearlTokenItem.getOwnerName(token));
                }));
    }
}
