package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ClientboundPlayerInfoUpdatePacket.Entry.class)
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

    @Redirect(method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getGameProfile()Lcom/mojang/authlib/GameProfile;"))
    private static GameProfile sf$redirectGameProfileForRoom(ServerPlayer instance) {
        if (!RoomModule.isVirtual(instance.getUUID())) {
            return instance.getGameProfile();
        }
        return getModule().devirtualize(instance.getUUID());
    }
}
