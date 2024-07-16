package io.ib67.sfcraft.mixin.server;

import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.util.MixinHelper;
import io.ib67.sfcraft.util.SFConsts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Unique
    private static final ServerLinks sf$serverLinks = new ServerLinks(
            List.of(
                    new ServerLinks.Entry(Either.left(ServerLinks.Known.ANNOUNCEMENTS), URI.create("https://wiki.sfclub.cc?anno")),
                    new ServerLinks.Entry(Either.left(ServerLinks.Known.BUG_REPORT), URI.create("https://wiki.sfclub.cc/?bug")),
                    new ServerLinks.Entry(Either.left(ServerLinks.Known.STATUS), URI.create("https://wiki.sfclub.cc/?bug"))
            )
    );

    @Inject(method = "getServerLinks", at = @At("HEAD"),cancellable = true)
    private void getServerLinks(CallbackInfoReturnable<ServerLinks> cir) {
        cir.setReturnValue(sf$serverLinks);
    }

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
