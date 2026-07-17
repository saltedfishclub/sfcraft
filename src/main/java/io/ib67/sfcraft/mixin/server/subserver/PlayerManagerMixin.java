package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.mixin.common.bridge.ServerChunkManagerBridge;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

@Mixin(PlayerList.class)

public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    public void sf$updateSession(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(player.getUUID());
        if (room != null) {
            var spawn = room.getSpawnPosition();
            if(player.level().dimension() != spawn.dimension()){
                teleportToRoom(player, spawn);
            }

            LOGGER.info("Room player joined as " + player.getName().tryCollapseToString() + "/" + player.getUUID());
            room.onPlayerJoin(player);
        }
    }

    @Inject(method = "respawn", at = @At("TAIL"))
    public void sf$updateSession(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(player.getUUID());
        if (!alive && room != null) {
            var spawn = room.getSpawnPosition();
            teleportToRoom(cir.getReturnValue(), spawn);
        }
    }

    @Inject(method = "canPlayerLogin", cancellable = true, at = @At("HEAD"))
    private void sf$bypassJoinLimit(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
        if (RoomModule.isVirtual(profile.getId())) cir.setReturnValue(null);
    }

    @Unique
    private void teleportToRoom(ServerPlayer player, GlobalPos spawn) {
        var world = player.getServer().getLevel(spawn.dimension());
        var pos = spawn.pos();
        ((ServerChunkManagerBridge)world.getChunkSource()).getLevelManager().addPlayer(player.getLastSectionPos(), player);
        player.teleportTo(world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), 0, 0, true);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    public void sf$removePlayers(ServerPlayer instance, CallbackInfo ci) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var room = registry.getRoomBy(instance.getUUID());
        if (room != null) {
            room.onPlayerQuit(instance);
        }
    }
}
