package io.ib67.sfcraft.module.game.crystal;

import com.google.inject.Inject;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * 重力水晶:用紫水晶碎片充能,加速范围内生物的移动。
 */
@Getter
public class GravityCrystalModule extends ServerModule {
    @Inject
    private GameConfigService config;
    @Inject
    private CauldronRecipeRegistry cauldronRecipes;

    private GravityCrystalBlock crystalBlock;
    private BlockEntityType<GravityCrystalBlockEntity> crystalBlockEntity;
    private Item crystalItem;

    private final List<CauldronRecipe> registeredRecipes = new ArrayList<>();

    @Override
    public void onInitialize() {
        crystalBlock = RegistryHelper.registerBlock(
                "gravity_crystal",
                GravityCrystalBlock::new,
                BlockBehaviour.Properties.of()
                        .strength(50.0F, 1200.0F)
                        .sound(SoundType.AMETHYST)
                        .lightLevel(state -> Math.min(15, state.getValue(GravityCrystalBlock.CHARGE) * 4))
        );
        crystalBlockEntity = RegistryHelper.registerBlockEntity(
                "gravity_crystal",
                (pos, state) -> new GravityCrystalBlockEntity(crystalBlockEntity, config, pos, state),
                crystalBlock
        );
        crystalBlock.wireBlockEntity(crystalBlockEntity,
                (pos, state) -> new GravityCrystalBlockEntity(crystalBlockEntity, config, pos, state));

        crystalItem = RegistryHelper.registerItem(
                "gravity_crystal",
                properties -> new PolymerBlockItem(crystalBlock, properties, Items.RESPAWN_ANCHOR),
                new Item.Properties()
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.gravity_crystal"))
        );

        registerRecipes();
        // /sfcraft reload 后重建配方,应用最新的 reactionTicks
        SFConfigReload.EVENT.register(() -> {
            registeredRecipes.forEach(cauldronRecipes::unregister);
            registeredRecipes.clear();
            registerRecipes();
        });
    }

    private void registerRecipes() {
        // 紫水晶炼药锅里丢入: 羽毛 + 重生锚 + 回响碎片(催化剂),加热熔炼 → 重力水晶
        register(new CauldronRecipe(
                "gravity_crystal",
                List.of(
                        stack -> stack.is(Items.FEATHER),
                        stack -> stack.is(Items.RESPAWN_ANCHOR),
                        stack -> stack.is(Items.ECHO_SHARD)),
                false,
                true,
                ParticleTypes.REVERSE_PORTAL,
                config.get().cauldron.reactionTicks,
                contents -> new ItemStack(crystalItem)));
    }

    private void register(CauldronRecipe recipe) {
        registeredRecipes.add(recipe);
        cauldronRecipes.register(recipe);
    }
}
