package io.ib67.sfcraft.util;

import io.ib67.sfcraft.util.Permission;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

@UtilityClass
public class SFConsts {
    public static final String VERSION = FabricLoader.getInstance().getModContainer("sfcraft").get().getMetadata().getVersion().getFriendlyString();
    public static final String SPECIAL_SUDO = "sfcraft.special.state.sudo";
    public static final Permission<PlayerEntity> USE_AT = ofSFCPermission("chat.at", true);
    public static final Permission<PlayerEntity> USE_BROADCAST_LOCATION = ofSFCPermission("chat.xyz", true);
    public static final Permission<PlayerEntity> COMMAND_BACK = ofSFCPermission("command.back", true);
    public static final Permission<PlayerEntity> UNLIMITED_COMMAND_BACK = ofSFCPermission("command.back.unlimited", false);

    public static final Permission<PlayerEntity> COMMAND_ADDWL = ofSFCPermission("command.addwl", false);
    public static final Permission<PlayerEntity> COMMAND_UNBLOCKSERVER = ofSFCPermission("command.unblockserver", false);
    public static final Permission<PlayerEntity> COMMAND_LISTOFFLINE = ofSFCPermission("command.listoffline", false);
    public static final Permission<PlayerEntity> COMMAND_LISTPERM = ofSFCPermission("command.listperm", false);

    public static Permission<PlayerEntity> ofSFCPermission(String key, boolean byDefault) {
        return new Permission<>("sfcraft." + key, byDefault);
    }
}