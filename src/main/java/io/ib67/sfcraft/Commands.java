package io.ib67.sfcraft;

import com.mojang.brigadier.context.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Colors;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Commands {

    public Commands() {
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        serverPlayNetworkHandler.getPlayer().removeCommandTag(SFConsts.SPECIAL_SUDO);
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

    public int onBack(CommandContext<ServerCommandSource> it) {
        var player = it.getSource().getPlayer();
        var pos = player.getLastDeathPos().get();
        var wld = player.server.getWorld(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return 0;
        var nearby = wld.getClosestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 20, true);
        if (nearby != null) {
            _pos = nearby.getBlockPos();
            player.teleport(wld, _pos.getX(), _pos.getY(), _pos.getZ(), 0, 0);
        } else {
            if (SFConsts.UNLIMITED_COMMAND_BACK.hasPermission(player)) {
                player.teleport(wld, pos.pos().getX(), pos.pos().getY(), pos.pos().getZ(), 0, 0);
                return 0;
            }
            player.sendMessage(Text.of("周围没有玩家。").copy().withColor(Colors.RED));
        }
        return 0;
    }

    public int unblockServer(CommandContext<ServerCommandSource> ctx) {
        var state = SFCraft.getInstance().getConfig().disableTemporarily;
        SFCraft.getInstance().getConfig().disableTemporarily = !state;
        ctx.getSource().sendMessage(Text.of("Current state: " + !state));
        return 0;
    }

    public int enterSudoMode(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player.getCommandTags().contains(SFConsts.SPECIAL_SUDO)) {
            player.removeCommandTag(SFConsts.SPECIAL_SUDO);
            player.sendMessage(Text.of("Sudo is off.").copy().withColor(Colors.GREEN));
        } else {
            player.addCommandTag(SFConsts.SPECIAL_SUDO);
            player.sendMessage(Text.of("Sudo is on.").copy().withColor(Colors.GREEN));
        }
        return 0;
    }

    @SneakyThrows
    public int listPerms(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        var src = serverCommandSourceCommandContext.getSource();
        src.sendMessage(Text.of("Permissions: ").copy().withColor(Colors.GREEN));
        for (Field declaredField : SFConsts.class.getDeclaredFields()) {
            if (Permission.class.isAssignableFrom(declaredField.getType())) {
                var permission = (Permission<?>) declaredField.get(null);
                src.sendMessage(Text.of(" - " + permission.key() + " " + (permission.byDefault() ? "(default)" : "")));
            }
        }
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
}
