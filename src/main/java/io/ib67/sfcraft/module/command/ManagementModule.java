package io.ib67.sfcraft.module.command;

import com.google.inject.Inject;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.Permission;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.CommonColors;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ManagementModule extends ServerModule {
    @Inject
    private GeoIPService geoIPService;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        serverPlayNetworkHandler.getPlayer().removeTag(SFConsts.SPECIAL_SUDO);
    }

    private void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection env) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("sudo")
                .requires(it -> this.isEnabled())
                .requires(it -> Commands.LEVEL_GAMEMASTERS.check(it.permissions()) && it.getPlayer() != null)
                .executes(this::enterSudoMode)
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("listperm")
                .requires(it -> this.isEnabled())
                .requires(it -> Commands.LEVEL_GAMEMASTERS.check(it.permissions()) || SFConsts.COMMAND_LISTPERM.hasPermission(it.getPlayer()))
                .executes(this::listPerms)
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("listgeo")
                .requires(it -> this.isEnabled())
                .requires(it -> Commands.LEVEL_GAMEMASTERS.check(it.permissions()) || SFConsts.COMMAND_LISTGEO.hasPermission(it.getPlayer()))
                .executes(this::listGeo)
        );
    }

    private int listGeo(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var server = src.getServer();
        src.sendSystemMessage(Component.translatable("message.sfcraft.management.cities_header"));
        for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers()) {
            try {
                var addr = InetAddress.getByName(serverPlayerEntity.getIpAddress());
                var city = geoIPService.cityOf(addr);
                src.sendSystemMessage(
                        Component.literal(" - ")
                                .append(Component.literal("[" + city.name() + "] ").withColor(CommonColors.BLUE))
                                .append(Component.literal(serverPlayerEntity.getName().tryCollapseToString()))
                                .append(Component.literal(" (" + Helper.hideIp(addr) + ")").withColor(CommonColors.LIGHT_GRAY))
                );
            } catch (GeoIp2Exception | UnknownHostException e) {
                src.sendSystemMessage(Component.translatable("message.sfcraft.management.fetch_failed", serverPlayerEntity.getName().tryCollapseToString()).withColor(CommonColors.SOFT_RED));
            }
        }
        return 0;
    }

    public int enterSudoMode(CommandContext<CommandSourceStack> ctx) {
        var player = ctx.getSource().getPlayer();
        if (player.entityTags().contains(SFConsts.SPECIAL_SUDO)) {
            player.removeTag(SFConsts.SPECIAL_SUDO);
            player.sendSystemMessage(Component.translatable("message.sfcraft.management.sudo_off").withColor(CommonColors.GREEN));
        } else {
            player.addTag(SFConsts.SPECIAL_SUDO);
            player.sendSystemMessage(Component.translatable("message.sfcraft.management.sudo_on").withColor(CommonColors.GREEN));
        }
        return 0;
    }

    @SneakyThrows
    public int listPerms(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.translatable("message.sfcraft.management.perms_header").withColor(CommonColors.GREEN));
        for (Field declaredField : SFConsts.class.getDeclaredFields()) {
            if (Permission.class.isAssignableFrom(declaredField.getType())) {
                var permission = (Permission<?>) declaredField.get(null);
                src.sendSystemMessage(Component.literal(" - " + permission.key() + " ")
                        .append(permission.byDefault() ? Component.translatable("message.sfcraft.management.perm_default") : Component.empty()));
            }
        }
        return 0;
    }
}
