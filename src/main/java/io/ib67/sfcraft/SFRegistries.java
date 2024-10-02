package io.ib67.sfcraft;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class SFRegistries {
    public static final Registry<Item> ITEMS = FabricRegistryBuilder
            .<Item>createSimple(RegistryKey.ofRegistry(Identifier.of(SFCraft.MOD_ID, "item")))
            .attribute(RegistryAttribute.MODDED)
            .buildAndRegister();
}
