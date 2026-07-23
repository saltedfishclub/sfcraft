package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.mixin.common.bridge.ServerWorldBridge;
import io.ib67.sfcraft.util.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ServerLevelData;setGameTime(J)V"))
    public long setTimeOfDay(long timeOfDay) {
        var _this = ((ServerLevel) (Object) this);
        int i = _this.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) this).getSleepStatus().areEnoughSleeping(i)) {
            var dt = _this.dimensionTypeRegistration();
            var clock = dt.value().defaultClock();
            if(clock.isEmpty()) return timeOfDay;
            // Day/night acceleration is driven solely by the clock below. Do NOT inflate
            // gameTime here: ServerLevel.tick feeds getGameTime() into blockTicks/fluidTicks,
            // so advancing it would fast-forward scheduled ticks (liquids flow faster, etc.).
            _this.clockManager().addTicks(clock.get(), 20);
            MixinHelper.spedUp = true;
            return timeOfDay;
        }
        MixinHelper.spedUp = false;
        return timeOfDay;
    }
}
