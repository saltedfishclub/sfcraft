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
import lombok.Setter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.CommonColors;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OfflineExemptModule extends ServerModule {
    @Inject
    private SFConfig config;
    private boolean state;
    @Setter
    private boolean temporallyState;

    @Override
    public void onInitialize() {
        state = config.enableOfflineExempt;
        SFCallbacks.PRE_LOGIN.register(this::onPreLogin);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection registrationEnvironment) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("addwl")
                        .requires(i -> isEnabled())
                        .requires(it -> Commands.LEVEL_GAMEMASTERS.check(it.permissions()) || SFConsts.COMMAND_ADDWL.hasPermission(it.getPlayer()))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.string())
                                .executes(it -> this.addPlayerOffline(it.getSource(), it.getArgument("player", String.class))))

        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("listoffline")
                .requires(i -> isEnabled())
                .requires(it -> Commands.LEVEL_GAMEMASTERS.check(it.permissions()) || SFConsts.COMMAND_LISTOFFLINE.hasPermission(it.getPlayer()))
                .executes(this::listOffline)
        );
    }

    public int addPlayerOffline(CommandSourceStack source, String player) {
        player = player.trim();
        var pm = source.getServer().getPlayerList();
        var wl = pm.getWhiteList();
        if (List.of(wl.getUserList()).contains(player)) {
            source.sendFailure(Component.nullToEmpty(player + " is already whitelisted or this id is conflict with a online user."));
            return 0;
        }
        var profile = NameAndId.createOffline(player);
        wl.add(new UserWhiteListEntry(profile));
        var uc = source.getServer().services().nameToIdCache();
        if (uc != null) uc.add(profile);
        source.sendSystemMessage(Component.nullToEmpty("[" + profile.name() + "/" + profile.id() + "]" + " is added!"));
        return 0;
    }

    public int listOffline(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var server = source.getServer();
        var wl = server.getPlayerList().getWhiteList();
        var names = wl.getUserList();
        source.sendSystemMessage(Component.nullToEmpty("Offline Users:").copy().withColor(CommonColors.GREEN));
        var r = Stream.of(names).filter(name -> wl.isWhiteListed(NameAndId.createOffline(name))).collect(Collectors.joining(", "));
        source.sendSystemMessage(Component.nullToEmpty(r.isEmpty() ? "No offline users found in whitelist." : r));
        return 0;
    }

    @Override
    public void onDisable() {
        state = false;
    }

    @Override
    public void onEnable() {
        state = temporallyState || config.enableOfflineExempt;
    }

    private void onPreLogin(String s, Connection connection, Consumer<Component> textConsumer, boolean offline) {
        if (!state && offline) {
            textConsumer.accept(Component.nullToEmpty("§c盗版赦免现已关闭"));
        }
    }
}
