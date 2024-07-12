package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class PlayLimitModule extends ServerModule {
    private final Set<String> motdRequestedIps = new HashSet<>();
    @Inject
    GeoIPService geoIPService;
    @Inject
    SFConfig config;

    @Override
    public void onInitialize() {
        SFCallbacks.PRE_LOGIN.register(this::onPreLogin);
        SFCallbacks.MOTD.register(this::onMotd);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("unblockserver")
                .requires(i -> isEnabled())
                .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_UNBLOCKSERVER.hasPermission(it.getPlayer()))
                .executes(this::unblockServer));
    }

    public int unblockServer(CommandContext<ServerCommandSource> ctx) {
        var state = config.disableTemporarily;
        config.disableTemporarily = !state;
        ctx.getSource().sendMessage(Text.of("Current state: " + !state));
        return 0;
    }

    private ServerMetadata onMotd(MinecraftServer server, ClientConnection connection) {
        var ip = connection.getAddressAsString(false);
        var cached = motdRequestedIps.contains(ip);
        if (!cached) {
            motdRequestedIps.add(ip);
        }
        var originalMetadata = server.getServerMetadata();
        if (originalMetadata == null) return null;
        String version = null;
        if (connection.getAddress() instanceof InetSocketAddress address) {
            version = Helper.getVersionString(address);
        }
        if (version != null && isEnabled()) {
            return new ServerMetadata(
                    originalMetadata.description(),
                    originalMetadata.players(),
                    Optional.of(new ServerMetadata.Version(version, 0)),
                    cached ? Optional.empty() : originalMetadata.favicon(),
                    originalMetadata.secureChatEnforced()
            );
        } else {
            return originalMetadata;
        }

    }

    private boolean onPreLogin(String currentPlayer, ClientConnection connection, Consumer<Text> disconnect) {
        if (isEnabled() && connection.getAddress() instanceof InetSocketAddress address) {
            try {
                var clock = geoIPService.clockOf(address.getAddress());
                if (config.isClosed(clock) && !"iceBear67".equals(currentPlayer)) {
                    final var msg = "服务器尚未开放\n服务器维护时间： %1$d~%2$d\n你的下午不值得浪费在游戏上，晚点再来吧！";
                    disconnect.accept(Text.of(String.format(msg, config.maintainceStartHour, config.maintainceEndHour)));
                }
            } catch (Exception ignored) {
                System.err.println("Failed to locate " + address.getAddress());
            }
        }
        return true;
    }
}
