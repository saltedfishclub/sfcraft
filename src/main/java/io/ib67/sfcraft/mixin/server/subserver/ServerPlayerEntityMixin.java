package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.RoomRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {
    //todo test this
    //todo fix playground operate players in main world
    @Inject(at=@At("HEAD"), method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z")
    public void sf$interceptTeleport(ServerLevel world, double destX, double destY, double destZ, Set<Relative> flags, float yaw, float pitch, boolean resetCamera, CallbackInfoReturnable<Boolean> cir) {
        var room = roomRegistry().getRoomBy($this().getUUID());
        if (room != null) {
            if (world.dimension() != room.getSpawnPosition().dimension()) {
                cir.setReturnValue(false);
            }
        }
    }

    private RoomRegistry roomRegistry() {
        return SFCraft.getInjector().getInstance(RoomRegistry.class);
    }

    private ServerPlayer $this() {
        return (ServerPlayer) (Object) this;
    }
}
