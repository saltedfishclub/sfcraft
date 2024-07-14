package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.Objects;

public class AFKModule extends ServerModule {
    @Inject
    private MinecraftServerSupplier serverSupplier;
    private Team team;

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_IDLE.register(this::onAFK);
        ServerPlayConnectionEvents.JOIN.register(this::onJoin);
    }

    @Override
    public void onEnable() {
        if (team == null) {
            team = new Team(serverSupplier.get().getScoreboard(), "afk");
            team.setPrefix(Text.literal("[挂机] ").withColor(Colors.LIGHT_GRAY));
        }
    }

    private void onAFK(ServerPlayerEntity player, boolean afk) {
        String playerName = player.getName().getLiteralString();
        Objects.requireNonNull(playerName);
        if (afk) {
            team.getPlayerList().add(playerName);
            player.server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
            player.server.getPlayerManager().broadcast(
                    Text.literal(" * " + playerName + " 正在挂机.").withColor(Colors.LIGHT_GRAY),
                    false
            );
        } else {
            team.getPlayerList().remove(playerName);
            player.server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
            player.server.getPlayerManager().broadcast(
                    Text.literal(" * " + playerName + " 回来了.").withColor(Colors.LIGHT_GRAY),
                    false
            );
        }
    }

    private void onJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        if (team != null) {
            packetSender.sendPacket(TeamS2CPacket.updateTeam(team, true));
        }
    }
}
