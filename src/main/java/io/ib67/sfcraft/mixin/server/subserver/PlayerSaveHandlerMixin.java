package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.datafixers.DataFixer;
import io.ib67.sfcraft.util.Helper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(PlayerSaveHandler.class)
public class PlayerSaveHandlerMixin {
    @Shadow
    @Final
    private File playerDataDir;
    @Unique
    private File virtualPlayerFolder;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void sf$createVirtualPlayerDataFolder(LevelStorage.Session session, DataFixer dataFixer, CallbackInfo ci) {
        this.virtualPlayerFolder = session.getDirectory(WorldSavePath.PLAYERDATA).resolve("in_rooms").toFile();
        this.virtualPlayerFolder.mkdirs();
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/world/PlayerSaveHandler;playerDataDir:Ljava/io/File;"), method = "savePlayerData")
    private File sf$getPlayerDataFolder(PlayerSaveHandler instance, PlayerEntity entity) {
        if (Helper.isVirtual(entity.getUuid())) {
            return virtualPlayerFolder;
        } else {
            return this.playerDataDir;
        }
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/world/PlayerSaveHandler;playerDataDir:Ljava/io/File;"), method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;Ljava/lang/String;)Ljava/util/Optional;")
    private File sf$saveGetPlayerFolder(PlayerSaveHandler instance, PlayerEntity player) {
        if (Helper.isVirtual(player.getUuid())) {
            return virtualPlayerFolder;
        } else {
            return this.playerDataDir;
        }
    }
}
