package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.config.SFConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OfflineExemptModule extends ServerModule {
    @Inject
    private SFConfig config;
    private boolean state;

    @Override
    public void onInitialize() {
        state = config.enableOfflineExempt;
        SFCallbacks.PRE_LOGIN.register(this::onPreLogin);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("addwl")
                        .requires(i -> isEnabled())
                        .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_ADDWL.hasPermission(it.getPlayer()))
                        .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("player", StringArgumentType.string())
                                .executes(it -> this.addPlayerOffline(it.getSource(), it.getArgument("player", String.class))))

        );
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("listoffline")
                .requires(i -> isEnabled())
                .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_LISTOFFLINE.hasPermission(it.getPlayer()))
                .executes(this::listOffline)
        );
    }

    public int addPlayerOffline(ServerCommandSource source, String player) {
        player = player.trim();
        var pm = source.getServer().getPlayerManager();
        var wl = pm.getWhitelist();
        if (List.of(wl.getNames()).contains(player)) {
            source.sendError(Text.of(player + " is already whitelisted or this id is conflict with a online user."));
            return 0;
        }
        var profile = Uuids.getOfflinePlayerProfile(player);
        wl.add(new WhitelistEntry(profile));
        source.sendMessage(Text.of("[" + profile.getName() + "/" + profile.getId() + "]" + " is added!"));
        return 0;
    }

    public int listOffline(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        var server = source.getServer();
        var wl = server.getPlayerManager().getWhitelist();
        var names = wl.getNames();
        source.sendMessage(Text.of("Offline Users:").copy().withColor(Colors.GREEN));
        var r = Stream.of(names).filter(name -> wl.isAllowed(Uuids.getOfflinePlayerProfile(name))).collect(Collectors.joining(", "));
        source.sendMessage(Text.of(r.isEmpty() ? "No offline users found in whitelist." : r));
        return 0;
    }

    @Override
    public void onDisable() {
        state = false;
    }

    @Override
    public void onEnable() {
        state = true;
    }

    private boolean onPreLogin(String s, ClientConnection connection, Consumer<Text> textConsumer) {
        return state;
    }
}
