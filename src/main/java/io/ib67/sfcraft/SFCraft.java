package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.ib67.sfcraft.access.AccessController;
import io.ib67.sfcraft.access.GrantedEntry;
import io.ib67.sfcraft.access.impl.SimpleAccessController;
import io.ib67.sfcraft.geoip.GeoIPService;
import lombok.Getter;
import lombok.SneakyThrows;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class SFCraft implements ModInitializer {
    @Getter
    private SFConfig config;
    @Getter
    private static Listener listener;
    @Getter
    private static SFCraft instance;
    @Getter
    private GeoIPService geoIPService;
    @Getter
    private AccessController accessController;
    @Getter
    private Path root;

    @Override
    @SneakyThrows
    public void onInitialize() {
        instance = this;
        listener = new Listener(this);
        root = Path.of("sfcraft");
        if (Files.notExists(root)) {
            Files.createDirectory(root);
        }
        loadConfig();
        geoIPService = new GeoIPService();
        accessController = loadAccessController();
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    @SneakyThrows
    private void loadConfig() {
        var gson = new Gson();
        var cfgFile = root.resolve("config.json");
        if (Files.notExists(cfgFile)) {
            Files.writeString(cfgFile, gson.toJson(new SFConfig()));
            config = new SFConfig();
            return;
        }
        config = gson.fromJson(Files.readString(cfgFile), SFConfig.class);
    }

    @SneakyThrows
    private AccessController loadAccessController() {
        var file = root.resolve("access.txt");
        ServerLifecycleEvents.SERVER_STOPPING.register(this::saveAccessRules);
        if (Files.notExists(file)) {
            return new SimpleAccessController(List.of());
        }
        return new SimpleAccessController(Files.readAllLines(file).stream().map(it -> it.split(":")).filter(it -> it.length == 2).map(it -> new GrantedEntry(it[0].trim(), it[1].trim())).toList());
    }

    @SneakyThrows
    private void saveAccessRules(MinecraftServer minecraftServer) {
        var file = root.resolve("access.txt");
        Files.writeString(file, accessController.getEntries().stream().map(i -> i.name() + ":" + i.condition()).collect(Collectors.joining("\n")));
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        var accessControl = new Commands.AccessControl(accessController);
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("access")
                        .requires(it -> it.hasPermissionLevel(2))
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("grant")
                                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", StringArgumentType.string()).executes(it -> {
                                    accessControl.grantAccess(it.getSource(), it.getArgument("name", String.class), null);
                                    return 0;
                                }).then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("ip", StringArgumentType.string()).executes(it -> {
                                    accessControl.grantAccess(it.getSource(), it.getArgument("name", String.class), it.getArgument("ip", String.class));
                                    return 0;
                                }))))
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                                .executes(it -> {
                                    accessControl.listAccesses(it.getSource());
                                    return 0;
                                })
                        ).then(LiteralArgumentBuilder.<ServerCommandSource>literal("revoke")
                                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", StringArgumentType.string())
                                        .executes(it -> {
                                            accessControl.revokeAccess(it.getSource(), it.getArgument("name", String.class));
                                            return 0;
                                        }))
                        )

        );
    }

}
