package io.ib67.sfcraft;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class SFRegistries {
    public static final Registry<Item> ITEMS = FabricRegistryBuilder
            .<Item>create(ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "item")))
            .attribute(RegistryAttribute.MODDED)
            .buildAndRegister();
}
