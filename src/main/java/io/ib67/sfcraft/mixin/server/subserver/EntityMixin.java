package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.util.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract UUID getUuid();

    @Inject(at = @At("HEAD"), method = "teleportTo", cancellable = true)
    private void sf$redirectTeleport(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {
        if (RoomModule.isVirtual(this.getUuid())) {
            cir.setReturnValue((Entity) (Object) this);
        }
    }

    @Inject(at = @At("HEAD"), method = "canUsePortals", cancellable = true)
    private void sf$disablePortal(boolean allowVehicles, CallbackInfoReturnable<Boolean> cir) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var $this = (Entity) (Object) this;
        if (registry.isRoomWorld($this.getWorld().getRegistryKey())) {
            cir.setReturnValue(false);
        }
    }
}
