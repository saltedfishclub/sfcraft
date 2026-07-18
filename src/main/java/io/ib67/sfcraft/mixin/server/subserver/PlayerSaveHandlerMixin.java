package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.datafixers.DataFixer;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.util.Helper;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;

@Mixin(PlayerDataStorage.class)
public class PlayerSaveHandlerMixin {
    @Shadow
    @Final
    private File playerDir;
    @Unique
    private File virtualPlayerFolder;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void sf$createVirtualPlayerDataFolder(LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, CallbackInfo ci) {
        this.virtualPlayerFolder = session.getLevelPath(LevelResource.PLAYER_DATA_DIR).resolve("in_rooms").toFile();
        this.virtualPlayerFolder.mkdirs();
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/storage/PlayerDataStorage;playerDir:Ljava/io/File;"), method = "save")
    private File sf$getPlayerDataFolder(PlayerDataStorage instance, Player entity) {
        if (RoomModule.isVirtual(entity.getUUID())) {
            return virtualPlayerFolder;
        } else {
            return this.playerDir;
        }
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/storage/PlayerDataStorage;playerDir:Ljava/io/File;"), method = "Lnet/minecraft/world/level/storage/PlayerDataStorage;load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;")
    private File sf$saveGetPlayerFolder(PlayerDataStorage instance, NameAndId player) {
        if (RoomModule.isVirtual(player.id())) {
            return virtualPlayerFolder;
        } else {
            return this.playerDir;
        }
    }
}
