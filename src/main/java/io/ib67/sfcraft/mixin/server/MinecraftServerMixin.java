package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.module.randomevt.longnight.LongNightEvent;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @ModifyConstant(method = "tickWorlds", constant = @Constant(intValue = 20))
    private int speedUpAtNight(int constant) {
        if (LongNightEvent.isRunning()) {
            return 5;
        }
        return constant;
    }
}
