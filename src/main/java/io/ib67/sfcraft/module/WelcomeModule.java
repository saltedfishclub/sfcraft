package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.ConfigResources;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.inject.ConfigResource;
import io.ib67.sfcraft.inject.ConfigRoot;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.ib67.sfcraft.util.Helper.getConfigResource;

public class WelcomeModule extends ServerModule {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    @Inject
    private SFConfig config;
    @Inject
    @ConfigRoot
    protected Path configRoot;
    protected Map<String, String> announcements;


    @SneakyThrows
    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        var announcementDir = configRoot.resolve("announcements");
        if (Files.notExists(announcementDir)) {
            Files.createDirectory(announcementDir);
        }
        announcements = new HashMap<>();
        try(var lines = Files.list(announcementDir)){
            for (var ele : lines.toList()) {
                if (!ele.toString().endsWith("txt")) continue;
                announcements.put("sfwelcome_"+ele.getFileName().toString().replace(".txt", ""), Files.readString(ele).replaceAll("&", "§"));
            }
        }
    }

    public void onPlayerJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender
            packetSender, MinecraftServer minecraftServer) {
        if (!isEnabled()) return;
        var player = serverPlayNetworkHandler.getPlayer();
        if (!player.getTags().contains("sf_unlock_recipe")) {
            player.addTag("sf_unlock_recipe");
            unlockRecipe(player);
        }
        for (String s : announcements.keySet()) {
            if (!player.getTags().contains(s)) {
                player.addTag(s);
                announcements.get(s).lines().map(Component::nullToEmpty).forEach(player::sendSystemMessage);
            }
        }
    }


    private void unlockRecipe(ServerPlayer player) {
        var recipe = player.getRecipeBook();
        recipe.addRecipes(player.getServer().getRecipeManager().getRecipes(), player);
    }
}
