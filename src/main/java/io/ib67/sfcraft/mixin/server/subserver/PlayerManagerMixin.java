package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.subserver.Room;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerManager.class)

public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setServerWorld(Lnet/minecraft/server/world/ServerWorld;)V"))
    public void sf$modifyToRoomWorld(ServerPlayerEntity instance, ServerWorld world) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(instance.getUuid());
        if (room != null) {
            var pos = room.getSpawnPosition();
            var nWorld = world.getServer().getWorld(pos.dimension());
            instance.setServerWorld(nWorld);
            var _pos = pos.pos();
            instance.setPos(_pos.getX(), _pos.getY(), _pos.getZ());
        } else {
            instance.setServerWorld(world);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void sf$updateSession(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(player.getUuid());
        if (room != null) {
            LOGGER.info("Room player joined as " + player.getName().getLiteralString() + "/" + player.getUuid());
            room.onPlayerJoin(player);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    public void sf$removePlayers(ServerPlayerEntity instance, CallbackInfo ci) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(instance.getUuid());
        if (room != null) {
            room.onPlayerQuit(instance);
        }
    }
}
