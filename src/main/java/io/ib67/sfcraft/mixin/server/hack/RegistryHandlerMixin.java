package io.ib67.sfcraft.mixin.server.hack;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.packet.DirectRegistryPacketHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mixin(DirectRegistryPacketHandler.class)
public abstract class RegistryHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), remap = false)
    public void sf$removeRegistry(Consumer<DirectRegistryPacketHandler.Payload> sender, Map<Identifier, Object2IntMap<Identifier>> registryMap, CallbackInfo ci) {
        registryMap.clear();
    }
}
