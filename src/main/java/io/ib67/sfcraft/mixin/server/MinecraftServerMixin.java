package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.util.MixinHelper;
import io.ib67.sfcraft.util.SFConsts;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @ModifyConstant(method = "tickWorlds", constant = @Constant(intValue = 20))
    private int speedUpAtNight(int constant) {
        if (MixinHelper.spedUp) {
            return 2;
        }
        return constant;
    }

    @Inject(method = "getServerModName", at = @At("TAIL"), cancellable = true, remap = false)
    private void rebrandServerModName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("§bsfcraft " + SFConsts.VERSION + "§r");
    }

}
