package io.ib67.sfcraft.mixin.server.item;

import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.item.Item;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Redirect(
            method ="<init>",
            at= @At(value = "INVOKE", target = "Lnet/minecraft/registry/DefaultedRegistry;createEntry(Ljava/lang/Object;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;")
    )
    public RegistryEntry.Reference sf$redirectCreateEntry(DefaultedRegistry instance, Object o){
        if(this instanceof SFItem) {
            return null;
        }
        return instance.createEntry(o);
    }
}
