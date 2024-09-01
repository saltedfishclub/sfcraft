package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(PlayerListS2CPacket.Entry.class)
public class PlayerListS2CPacket$EntryMixin {
    @Unique
    private static final byte[] lock = new byte[0];
    @Unique
    private static volatile RoomModule module;

    @Unique
    private static RoomModule getModule() {
        if (module == null) {
            synchronized (lock) {
                if (module == null) {
                    module = SFCraft.getInjector().getInstance(RoomModule.class);
                }
            }
        }
        return module;
    }

    @Redirect(method = "<init>(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getGameProfile()Lcom/mojang/authlib/GameProfile;"))
    private static GameProfile sf$redirectGameProfileForRoom(ServerPlayerEntity instance) {
        if (!RoomModule.isVirtual(instance.getUuid())) {
            return instance.getGameProfile();
        }
        return getModule().devirtualize(instance.getUuid());
    }
}
