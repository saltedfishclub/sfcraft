package io.ib67.sfcraft.module.chat;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import java.util.*;

//todo rework
public class ChatPrefixModule extends ServerModule {
    @Inject
    private MinecraftServerSupplier serverSupplier;
    private volatile ChatPrefix empty;
    private final Map<UUID, SortedSet<ChatPrefix>> playerPrefixes = new HashMap<>();
    private final Map<String, PlayerTeam> virtualTeams = new HashMap<>();
    private final Map<UUID, PlayerTeam> players = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(this::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    @Override
    public void onEnable() {
        empty = new ChatPrefix(
                Component.empty(),
                "EMPTY",
                true,
                1000
        );
        virtualTeams.put(empty.id(), createTeamFrom(empty));
    }

    private void onDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        var player = serverPlayNetworkHandler.getPlayer();
        var current = getCurrentPrefix(player);
        if (current != null && current.temporary()) {
            removeFromTeam(player, players.get(player.getUUID()));
        }
    }

    private void onJoin(ServerGamePacketListenerImpl networkHandler, PacketSender sender, MinecraftServer minecraftServer) {
        applyPrefix(networkHandler.getPlayer(), empty);
        for (Map.Entry<UUID, PlayerTeam> uuidTeamEntry : players.entrySet()) {
            var team = uuidTeamEntry.getValue();
            sender.sendPacket(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        }
    }

    private void onPrefixUpdate(Player player) {
        var lastTeam = players.get(player.getUUID());
        var current = virtualTeams.get(playerPrefixes.get(player.getUUID()).getFirst().id());
        if (lastTeam == current) return;
        if (lastTeam != null) removeFromTeam(player, lastTeam);
        if (current == null) {
            return;
        }
        addToTeam(player, current);
    }

    private void addToTeam(Player player, PlayerTeam team) {
        var playerName = player.getName().tryCollapseToString();
        if(team == null) return;
        team.getPlayers().add(playerName);
        players.put(player.getUUID(), team);
        sendToAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
    }

    private void removeFromTeam(Player player, PlayerTeam team) {
        var playerName = player.getName().tryCollapseToString();
        if(team == null) return;
        team.getPlayers().remove(playerName);
        sendToAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        var current = getCurrentPrefix(player);
        var newTeam = virtualTeams.get(current.id());
        addToTeam(player, newTeam);
    }

    private void sendToAll(Packet<?> packet) {
        serverSupplier.get().getPlayerList().broadcastAll(packet);
    }

    public void applyPrefix(Player player, ChatPrefix prefix) {
        if (hasPrefix(player.getUUID(), prefix)) {
            return;
        }
        playerPrefixes.get(player.getUUID()).add(prefix);
        onPrefixUpdate(player);
    }

    public void removePrefix(Player player, ChatPrefix prefix) {
        if (!hasPrefix(player.getUUID(), prefix)) {
            return;
        }
        playerPrefixes.get(player.getUUID()).remove(prefix);
        onPrefixUpdate(player);
    }

    public ChatPrefix getCurrentPrefix(Player player) {
        if (!playerPrefixes.containsKey(player.getUUID())) {
            return null;
        }
        return playerPrefixes.get(player.getUUID()).first();

    }

    public boolean hasPrefix(UUID player, ChatPrefix prefix) {
        if (!virtualTeams.containsKey(prefix.id())) {
            virtualTeams.put(prefix.id(), createTeamFrom(prefix));
        }
        return playerPrefixes.computeIfAbsent(player, ignored -> new TreeSet<>(List.of(empty))).contains(prefix);
    }

    private PlayerTeam createTeamFrom(ChatPrefix prefix) {
        var t = new PlayerTeam(
                serverSupplier.get().getScoreboard(),
                prefix.id()
        );
        t.setPlayerPrefix(prefix.prefix());
        return t;
    }
}
