package io.ib67.sfcraft.module.game;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.ib67.sfcraft.SFCraft;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Registration shorthands for game content. Only call these from a module's
 * {@code onInitialize()} — static registries are frozen after ModInit.
 */
public final class RegistryHelper {
    private RegistryHelper() {
    }

    public static <B extends Block> B registerBlock(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static <I extends Item> I registerItem(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> factory,
            Block... blocks
    ) {
        var id = Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name);
        var type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
        PolymerBlockUtils.registerBlockEntity(type);
        return type;
    }

    public static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder) {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
}
