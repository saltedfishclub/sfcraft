package io.ib67.sfcraft.module.game.crystal;

import com.google.inject.Inject;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 重力水晶:用紫水晶碎片充能,加速范围内生物的移动。
 */
@Getter
public class GravityCrystalModule extends ServerModule {
    @Inject
    private GameConfigService config;

    private GravityCrystalBlock crystalBlock;
    private BlockEntityType<GravityCrystalBlockEntity> crystalBlockEntity;
    private Item crystalItem;

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
    }
}
