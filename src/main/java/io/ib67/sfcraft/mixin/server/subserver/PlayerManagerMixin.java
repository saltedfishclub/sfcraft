package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.RoomRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PlayerManager.class)

public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void sf$updateSession(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(player.getUuid());
        if (room != null) {
            var spawn = room.getSpawnPosition();
            teleportToRoom(player, spawn);

            LOGGER.info("Room player joined as " + player.getName().getLiteralString() + "/" + player.getUuid());
            room.onPlayerJoin(player);
        }
    }

    @Inject(method = "respawnPlayer", at = @At("TAIL"))
    public void sf$updateSession(ServerPlayerEntity player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(player.getUuid());
        if (!alive && room != null) {
            var spawn = room.getSpawnPosition();
            teleportToRoom(cir.getReturnValue(), spawn);
        }
    }

    private void teleportToRoom(ServerPlayerEntity player, GlobalPos spawn) {
        var world = player.getServer().getWorld(spawn.dimension());
        var pos = spawn.pos();
        world.getChunkManager().chunkLoadingManager.getTicketManager().handleChunkEnter(player.getWatchedSection(), player);
        player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), 0, 0);
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
