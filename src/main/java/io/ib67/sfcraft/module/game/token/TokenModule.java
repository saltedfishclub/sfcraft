package io.ib67.sfcraft.module.game.token;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 珍珠信物(召唤绑定者)与反向珍珠信物(传送到绑定者)。
 * 反向信物由紫水晶炼药锅炼成,配方注册到 {@link CauldronRecipeRegistry},
 * 并在 /sfcraft reload 时重建以应用最新的 reactionTicks。
 */
@Getter
public class TokenModule extends ServerModule {
    @Inject
    private GameConfigService config;
    @Inject
    private CauldronRecipeRegistry cauldronRecipes;

    private Item pearlToken;
    private ReversePearlTokenItem reversePearlToken;

    private final List<CauldronRecipe> registeredRecipes = new ArrayList<>();

    @Override
    public void onInitialize() {
        pearlToken = RegistryHelper.registerItem(
                "pearl_token",
                PearlTokenItem::new,
                new Item.Properties()
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/pearl_token"))
                        .stacksTo(16)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.pearl_token"))
        );
        reversePearlToken = RegistryHelper.registerItem(
                "reverse_pearl_token",
                properties -> new ReversePearlTokenItem(properties, config),
                new Item.Properties()
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/pearl_token"))
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .durability(config.get().reverseToken.maxUses)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.reverse_pearl_token"))
        );
        registerRecipes();
        SFConfigReload.EVENT.register(() -> {
            registeredRecipes.forEach(cauldronRecipes::unregister);
            registeredRecipes.clear();
            registerRecipes();
        });
    }

    private void registerRecipes() {
        // 锅里丢入: 已绑定的珍珠信物 + 回响碎片(催化剂) + 水 + 加热 → 反向珍珠信物
        register(new CauldronRecipe(
                List.of(
                        stack -> stack.is(pearlToken) && PearlTokenItem.getOwnerId(stack) != null,
                        stack -> stack.is(Items.ECHO_SHARD)),
                true,
                true,
                new DustColorTransitionOptions(0x006D6D, 0x000000, 1.0F),
                config.get().cauldron.reactionTicks,
                contents -> {
                    var token = contents.stream()
                            .filter(stack -> stack.is(pearlToken))
                            .findFirst()
                            .orElse(ItemStack.EMPTY);
                    var ownerId = PearlTokenItem.getOwnerId(token);
                    if (ownerId == null) return ItemStack.EMPTY;
                    return reversePearlToken.createBound(ownerId, PearlTokenItem.getOwnerName(token));
                }));
    }

    private void register(CauldronRecipe recipe) {
        registeredRecipes.add(recipe);
        cauldronRecipes.register(recipe);
    }
}
