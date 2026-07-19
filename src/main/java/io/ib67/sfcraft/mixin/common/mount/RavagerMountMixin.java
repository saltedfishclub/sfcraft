package io.ib67.sfcraft.mixin.common.mount;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sfcraft.mount.MountAccess;
import sfcraft.mount.MountLogic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(Ravager.class)
public abstract class RavagerMountMixin extends Raider implements MountAccess {
    @Unique
    private UUID sfcraft$owner;
    @Unique
    private boolean sfcraft$saddled;
    @Unique
    private int sfcraft$dashTicks;
    @Unique
    private final Set<UUID> sfcraft$dashHitEntities = new HashSet<>();

    protected RavagerMountMixin(EntityType<? extends Raider> type, Level level) {
        super(type, level);
    }

    @Override
    public UUID sfcraft$getOwner() {
        return sfcraft$owner;
    }

    @Override
    public void sfcraft$setOwner(UUID owner) {
        sfcraft$owner = owner;
    }

    @Override
    public boolean sfcraft$isSaddled() {
        return sfcraft$saddled;
    }

    @Override
    public void sfcraft$setSaddled(boolean saddled) {
        sfcraft$saddled = saddled;
    }

    @Override
    public int sfcraft$getDashTicks() {
        return sfcraft$dashTicks;
    }

    @Override
    public void sfcraft$setDashTicks(int ticks) {
        sfcraft$dashTicks = ticks;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        LivingEntity rider = MountLogic.controllingPassenger((Mob) (Object) this, this);
        return rider != null ? rider : super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return MountLogic.riddenInput(this, player);
    }

    @Override
    public boolean canSimulateMovement() {
        // 骑手是 Player 时会被判为客户端权威,但纯服务端坐骑客户端不会模拟 → 强制服务端模拟
        if (!level().isClientSide() && getControllingPassenger() != null) return true;
        return super.canSimulateMovement();
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return MountLogic.riddenSpeed((Mob) (Object) this, this);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        MountLogic.tickRidden((Mob) (Object) this, this, player);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void sfcraft$suppressAiWhenRidden(CallbackInfo ci) {
        MountLogic.tickAiSuppression((Mob) (Object) this, this);
        MountLogic.tickRavagerDashCollision((Ravager) (Object) this, this, sfcraft$dashHitEntities);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void sfcraft$noAttackWhenRidden(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (MountLogic.blocksAttack((Mob) (Object) this, this, target)) cir.setReturnValue(false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sfcraft$saveMount(ValueOutput out, CallbackInfo ci) {
        MountLogic.save(out, this);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sfcraft$loadMount(ValueInput in, CallbackInfo ci) {
        MountLogic.load(in, this);
    }
}
