package io.ib67.sfcraft.util;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.util.Permission;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

@UtilityClass
public class SFConsts {
    public static final String VERSION = FabricLoader.getInstance().getModContainer(SFCraft.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
    public static final String SPECIAL_SUDO = "sfcraft.special.state.sudo";
    public static final Permission<PlayerEntity> USE_AT = ofSFCPermission("chat.at", true);
    public static final Permission<PlayerEntity> USE_BROADCAST_LOCATION = ofSFCPermission("chat.xyz", true);
    public static final Permission<PlayerEntity> COMMAND_BACK = ofSFCPermission("command.back", true);
    public static final Permission<PlayerEntity> UNLIMITED_COMMAND_BACK = ofSFCPermission("command.back.unlimited", false);
    public static final Permission<PlayerEntity> WORLDEDIT_AT_PLAYGROUND = ofSFCPermission("worldedit", true);

    public static final Permission<PlayerEntity> COMMAND_ADDWL = ofSFCPermission("command.addwl", false);
    public static final Permission<PlayerEntity> COMMAND_UPLOAD_SCHEMATIC = ofSFCPermission("command.upload.schematic", false);
    public static final Permission<PlayerEntity> COMMAND_LISTOFFLINE = ofSFCPermission("command.listoffline", false);
    public static final Permission<PlayerEntity> COMMAND_LISTPERM = ofSFCPermission("command.listperm", false);
    public static final Permission<PlayerEntity> COMMAND_LISTGEO = ofSFCPermission("command.listgeo", false);
    public static final Permission<PlayerEntity> COMMAND_PLAYGROUND = ofSFCPermission("command.playground", true);
    public static final Permission<PlayerEntity> COMMAND_RECO = ofSFCPermission("command.reco", true);

    public static Permission<PlayerEntity> ofSFCPermission(String key, boolean byDefault) {
        return new Permission<>("sfcraft." + key, byDefault);
    }
}
