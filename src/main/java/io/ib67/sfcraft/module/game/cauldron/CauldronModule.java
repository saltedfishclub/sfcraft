package io.ib67.sfcraft.module.game.cauldron;

import com.google.inject.Inject;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.module.game.item.SimplePolymerItem;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.ItemGroupService;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 紫水晶炼药锅:吸入锅内的物品凑齐配方并满足水/加热条件后自动反应。
 * 配方由其它特性模块注册到 {@link CauldronRecipeRegistry}。
 */
@Getter
public class CauldronModule extends ServerModule {
    @Inject
    private GameConfigService config;
    @Inject
    private CauldronRecipeRegistry recipes;
    @Inject
    private ItemGroupService itemGroups;

    private AmethystCauldronBlock cauldronBlock;
    private BlockEntityType<AmethystCauldronBlockEntity> cauldronBlockEntity;
    private Item cauldronItem;
    private Item cauldronBlankItem;

    @Override
    public void onInitialize() {
        cauldronBlock = RegistryHelper.registerBlock(
                "amethyst_cauldron",
                AmethystCauldronBlock::new,
                BlockBehaviour.Properties.of()
                        .strength(2.0F)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                        .lightLevel(state -> 10)
        );
        cauldronBlockEntity = RegistryHelper.registerBlockEntity(
                "amethyst_cauldron",
                (pos, state) -> new AmethystCauldronBlockEntity(cauldronBlockEntity, recipes, config, pos, state),
                cauldronBlock
        );
        cauldronBlock.wireBlockEntity(cauldronBlockEntity,
                (pos, state) -> new AmethystCauldronBlockEntity(cauldronBlockEntity, recipes, config, pos, state));

        cauldronBlankItem = RegistryHelper.registerItem(
                "amethyst_cauldron_blank",
                properties -> new SimplePolymerItem(properties, Items.RAW_IRON),
                new Item.Properties()
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/amethyst_cauldron"))
                        .stacksTo(16)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.amethyst_cauldron_blank"))
        );
        cauldronItem = RegistryHelper.registerItem(
                "amethyst_cauldron",
                properties -> new PolymerBlockItem(cauldronBlock, properties, Items.CAULDRON),
                new Item.Properties()
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.amethyst_cauldron"))
        );
        itemGroups.add(cauldronItem);
        itemGroups.add(cauldronBlankItem);
    }
}
