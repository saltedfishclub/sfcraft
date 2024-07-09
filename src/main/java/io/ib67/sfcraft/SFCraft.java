package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.registry.event.RandomEventRegistry;
import io.ib67.sfcraft.event.SFRandomEventRegistry;
import lombok.Getter;
import lombok.SneakyThrows;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.nio.file.Files;
import java.nio.file.Path;

public class SFCraft implements ModInitializer {
    @Getter
    private SFConfig config;
    @Getter
    private Listener listener;
    @Getter
    private static SFCraft instance;
    @Getter
    private GeoIPService geoIPService;
    @Getter
    private Path root;
    @Getter
    private MinecraftServer server;
    @Getter
    private String motd;
    @Getter
    private String updateLog;
    @Getter
    private RandomEventRegistry randomEventRegistry;

    @Override
    @SneakyThrows
    public void onInitialize() {
        instance = this;
        root = Path.of("sfcraft");
        if (Files.notExists(root)) {
            Files.createDirectory(root);
        }
        listener = new Listener(this);
        config = loadConfig();
        motd = loadMotd();
        updateLog = loadUpdateLog();
        geoIPService = new GeoIPService();
        this.setupRandomEvents();

        ServerPlayConnectionEvents.JOIN.register(listener::onPlayerJoin);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
    }

    private void setupRandomEvents() {
        randomEventRegistry = new SFRandomEventRegistry();
    }

    private void onServerStarted(MinecraftServer minecraftServer) {
        server = minecraftServer;
        server.setMotd(motd);
    }

    @SneakyThrows
    private String loadUpdateLog() {
        var log = root.resolve("update.log");
        if (Files.notExists(log)) {
            return "他没写呢";
        }
        return Files.readString(log);
    }

    @SneakyThrows
    private String loadMotd() {
        var motd = root.resolve("motd.txt");
        if (Files.notExists(motd)) {
            Files.writeString(motd, "§bSaltedFish Club Server");
        }
        return Files.readString(motd);
    }

    @SneakyThrows
    private SFConfig loadConfig() {
        var gson = new Gson();
        var cfgFile = root.resolve("config.json");
        if (Files.notExists(cfgFile)) {
            Files.writeString(cfgFile, gson.toJson(new SFConfig()));
            return new SFConfig();
        }
        return gson.fromJson(Files.readString(cfgFile), SFConfig.class);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        var handler = new Commands();

        // 一般非玩家实体都有 >= 2 的 PermissionLevel，因此不需要额外检查。
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("addwl")
                        .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_ADDWL.hasPermission(it.getPlayer()))
                        .then(
                                RequiredArgumentBuilder.<ServerCommandSource, String>argument("player", StringArgumentType.string())
                                        .executes(it -> handler.addPlayerOffline(it.getSource(), it.getArgument("player", String.class)))
                        )

        );
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("back")
                .requires(it -> it.getPlayer() != null)
                .requires(it -> it.getPlayer().getLastDeathPos().isPresent())
                .requires(it -> SFConsts.COMMAND_BACK.hasPermission(it.getPlayer()))
                .executes(handler::onBack));
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("unblockserver")
                .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_UNBLOCKSERVER.hasPermission(it.getPlayer()))
                .executes(handler::unblockServer));
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sudo")
                .requires(it -> it.hasPermissionLevel(2) && it.getPlayer() != null)
                .executes(handler::enterSudoMode)
        );
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("listperm")
                .requires(it->it.hasPermissionLevel(2) || SFConsts.COMMAND_LISTPERM.hasPermission(it.getPlayer()))
                .executes(handler::listPerms)
        );
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("listoffline")
                .requires(it->it.hasPermissionLevel(2) || SFConsts.COMMAND_LISTOFFLINE.hasPermission(it.getPlayer()))
                .executes(handler::listOffline)
        );
    }

}
