package io.ib67.sfcraft.mixin.server.fixes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {
    @Shadow
    private @Nullable Entity owner;

    @Shadow
    private @Nullable UUID ownerUuid;

    @Unique
    private boolean isPlayer;

    @Inject(at = @At("HEAD"), method = "setOwner")
    private void setOwner(@Nullable Entity owner, CallbackInfo ci) {
        isPlayer = owner instanceof PlayerEntity;
    }

    /**
     * @reason Implement the logic correctly.
     * @author icybear
     */
    @Overwrite
    public @Nullable Entity getOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        } else if (this.ownerUuid != null) {
            var server = $().getWorld().getServer();
            if (server == null) {
                return null;
            }
            if (isPlayer) {
                return server.getPlayerManager().getPlayer(this.ownerUuid);
            } else {
                for (ServerWorld world : server.getWorlds()) {
                    var entity = world.getEntity(this.ownerUuid);
                    if (entity != null) return entity;
                }
            }
        }
        return null;
    }

    @Unique
    private ProjectileEntity $() {
        return (ProjectileEntity) (Object) this;
    }
}
