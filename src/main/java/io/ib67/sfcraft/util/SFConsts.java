package io.ib67.sfcraft.util;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.util.Permission;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

@UtilityClass
public class SFConsts {
    public static final String VERSION = FabricLoader.getInstance().getModContainer(SFCraft.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
    public static final String SPECIAL_SUDO = "sfcraft.special.state.sudo";
    public static final Permission<Player> USE_AT = ofSFCPermission("chat.at", true);
    public static final Permission<Player> USE_BROADCAST_LOCATION = ofSFCPermission("chat.xyz", true);
    public static final Permission<Player> COMMAND_BACK = ofSFCPermission("command.back", true);
    public static final Permission<Player> UNLIMITED_COMMAND_BACK = ofSFCPermission("command.back.unlimited", false);
    public static final Permission<Player> WORLDEDIT_AT_PLAYGROUND = ofSFCPermission("worldedit", true);

    public static final Permission<Player> COMMAND_ADDWL = ofSFCPermission("command.addwl", false);
    public static final Permission<Player> COMMAND_UPLOAD_SCHEMATIC = ofSFCPermission("command.upload.schematic", false);
    public static final Permission<Player> COMMAND_LISTOFFLINE = ofSFCPermission("command.listoffline", false);
    public static final Permission<Player> COMMAND_LISTPERM = ofSFCPermission("command.listperm", false);
    public static final Permission<Player> COMMAND_LISTGEO = ofSFCPermission("command.listgeo", false);
    public static final Permission<Player> COMMAND_PLAYGROUND = ofSFCPermission("command.playground", true);
    public static final Permission<Player> COMMAND_PLAYGROUND_GAMEMODE = ofSFCPermission("command.playground.gamemode", true);
    public static final Permission<Player> COMMAND_PLAYGROUND_TELEPORT = ofSFCPermission("command.playground.teleport", true);
    public static final Permission<Player> COMMAND_PLAYGROUND_TIME = ofSFCPermission("command.playground.time", true);
    public static final Permission<Player> COMMAND_RECO = ofSFCPermission("command.reco", true);

    public static Permission<Player> ofSFCPermission(String key, boolean byDefault) {
        return new Permission<>("sfcraft." + key, byDefault);
    }
}
