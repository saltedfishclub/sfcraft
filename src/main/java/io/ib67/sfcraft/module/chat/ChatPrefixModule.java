package io.ib67.sfcraft.module.chat;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.*;

public class ChatPrefixModule extends ServerModule {
    @Inject
    private MinecraftServerSupplier serverSupplier;
    private volatile ChatPrefix empty;
    private final Map<UUID, SortedSet<ChatPrefix>> playerPrefixes = new HashMap<>();
    private final Map<String, Team> virtualTeams = new HashMap<>();
    private final Map<UUID, Team> players = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(this::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    @Override
    public void onEnable() {
        empty = new ChatPrefix(
                Text.empty(),
                "EMPTY",
                true,
                1000
        );
        virtualTeams.put(empty.id(), createTeamFrom(empty));
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        var player = serverPlayNetworkHandler.getPlayer();
        var current = getCurrentPrefix(player);
        if (current != null && current.temporary()) {
            removeFromTeam(player, players.get(player.getUuid()));
        }
    }

    private void onJoin(ServerPlayNetworkHandler networkHandler, PacketSender sender, MinecraftServer minecraftServer) {
        applyPrefix(networkHandler.getPlayer(), empty);
        for (Map.Entry<UUID, Team> uuidTeamEntry : players.entrySet()) {
            var team = uuidTeamEntry.getValue();
            sender.sendPacket(TeamS2CPacket.updateTeam(team, true));
        }
    }

    private void onPrefixUpdate(PlayerEntity player) {
        var lastTeam = players.get(player.getUuid());
        var current = virtualTeams.get(playerPrefixes.get(player.getUuid()).getFirst().id());
        if (lastTeam == current) return;
        if (lastTeam != null) removeFromTeam(player, lastTeam);
        if (current == null) {
            return;
        }
        addToTeam(player, current);
    }

    private void addToTeam(PlayerEntity player, Team team) {
        var playerName = player.getName().getLiteralString();
        team.getPlayerList().add(playerName);
        sendToAll(TeamS2CPacket.updateTeam(team, true));
        players.put(player.getUuid(), team);
    }

    private void removeFromTeam(PlayerEntity player, Team team) {
        var playerName = player.getName().getLiteralString();
        team.getPlayerList().remove(playerName);
        sendToAll(TeamS2CPacket.updateTeam(team, true));
        var current = getCurrentPrefix(player);
        var newTeam = virtualTeams.get(current.id());
        addToTeam(player, newTeam);
    }

    private void sendToAll(Packet<?> packet) {
        serverSupplier.get().getPlayerManager().sendToAll(packet);
    }

    public void applyPrefix(PlayerEntity player, ChatPrefix prefix) {
        if (hasPrefix(player.getUuid(), prefix)) {
            return;
        }
        playerPrefixes.get(player.getUuid()).add(prefix);
        onPrefixUpdate(player);
    }

    public void removePrefix(PlayerEntity player, ChatPrefix prefix) {
        if (!hasPrefix(player.getUuid(), prefix)) {
            return;
        }
        playerPrefixes.get(player.getUuid()).remove(prefix);
        onPrefixUpdate(player);
    }

    public ChatPrefix getCurrentPrefix(PlayerEntity player) {
        if (!playerPrefixes.containsKey(player.getUuid())) {
            return null;
        }
        return playerPrefixes.get(player.getUuid()).first();

    }

    public boolean hasPrefix(UUID player, ChatPrefix prefix) {
        if (!virtualTeams.containsKey(prefix.id())) {
            virtualTeams.put(prefix.id(), createTeamFrom(prefix));
        }
        return playerPrefixes.computeIfAbsent(player, ignored -> new TreeSet<>(List.of(empty))).contains(prefix);
    }

    private Team createTeamFrom(ChatPrefix prefix) {
        var t = new Team(
                serverSupplier.get().getScoreboard(),
                prefix.id()
        );
        t.setPrefix(prefix.prefix());
        return t;
    }
}
