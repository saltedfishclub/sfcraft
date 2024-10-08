package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.RoomRegistry;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(at = @At("HEAD"), method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FF)Z", cancellable = true)
    public void sf$interceptTeleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        var room = roomRegistry().getRoomBy($this().getUuid());
        if (room != null) {
            if (world.getRegistryKey() != room.getSpawnPosition().dimension()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V", cancellable = true)
    public void sf$interceptTeleport(ServerWorld targetWorld, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        var room = roomRegistry().getRoomBy($this().getUuid());
        if (room != null) {
            if (targetWorld.getRegistryKey() != room.getSpawnPosition().dimension()) {
                ci.cancel();
            }
        }
    }

    private RoomRegistry roomRegistry() {
        return SFCraft.getInjector().getInstance(RoomRegistry.class);
    }

    private ServerPlayerEntity $this() {
        return (ServerPlayerEntity) (Object) this;
    }
}
