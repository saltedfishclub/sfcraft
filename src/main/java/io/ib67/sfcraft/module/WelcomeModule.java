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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

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

    public void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender
            packetSender, MinecraftServer minecraftServer) {
        if (!isEnabled()) return;
        var player = serverPlayNetworkHandler.getPlayer();
        if (!player.getCommandTags().contains("sf_unlock_recipe")) {
            player.addCommandTag("sf_unlock_recipe");
            unlockRecipe(player);
        }
        for (String s : announcements.keySet()) {
            if (!player.getCommandTags().contains(s)) {
                player.addCommandTag(s);
                announcements.get(s).lines().map(Text::of).forEach(player::sendMessage);
            }
        }
    }


    private void unlockRecipe(ServerPlayerEntity player) {
        var recipe = player.getRecipeBook();
        recipe.unlockRecipes(player.getServer().getRecipeManager().values(), player);
    }
}
