package io.ib67.sfcraft.mixin.common;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setShiftKeyDown", at = @At("HEAD"))
    public void onSneaking(boolean sneaking, CallbackInfo ci) {
        // the client resends its whole input state whenever any key changes, so this is
        // called repeatedly while the player merely walks around sneaking. report edges only.
        if ($this() instanceof Player pe && sneaking != pe.isShiftKeyDown()) {
            SFCallbacks.PLAYER_SNEAKING.invoker().onSneaking(pe, sneaking);
        }
    }

    @Shadow
    public abstract UUID getUUID();

    @Inject(at = @At("HEAD"), method = "teleport", cancellable = true)
    private void sf$redirectTeleport(TeleportTransition teleportTarget, CallbackInfoReturnable<Entity> cir) {
        if (RoomModule.isVirtual(this.getUUID())) {
            cir.setReturnValue((Entity) (Object) this);
        }
    }

    @Inject(at = @At("HEAD"), method = "canUsePortal", cancellable = true)
    private void sf$disablePortal(boolean allowVehicles, CallbackInfoReturnable<Boolean> cir) {
        var registry = SFCraft.getInjector().getInstance(RoomRegistry.class);
        var $this = (Entity) (Object) this;
        if (registry.isRoomWorld($this.level().dimension())) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private Entity $this() {
        return (Entity) (Object) this;
    }
}
