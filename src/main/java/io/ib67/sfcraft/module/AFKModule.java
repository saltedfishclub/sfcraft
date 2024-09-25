package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.chat.ChatPrefix;
import io.ib67.sfcraft.module.chat.ChatPrefixModule;
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
    @Inject
    private ChatPrefixModule chatPrefixModule;
    private ChatPrefix prefix;

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_IDLE.register(this::onAFK);
        prefix = new ChatPrefix(
                Text.literal("[挂机] ").withColor(Colors.LIGHT_GRAY),
                "afk",
                true,
                10
        );
    }


    private void onAFK(ServerPlayerEntity player, boolean afk) {
        if (afk) {
            enAFK(player);
        } else {
            deAFK(player);
        }
    }

    private void enAFK(ServerPlayerEntity player) {
        chatPrefixModule.applyPrefix(player, prefix);
        player.server.getPlayerManager().broadcast(
                Text.literal(" * " + player.getName().getLiteralString() + " 正在挂机.").withColor(Colors.LIGHT_GRAY),
                false
        );
        SFCallbacks.PLAYER_AFK.invoker().onAFKStatus(player, true);
    }

    public void deAFK(ServerPlayerEntity player) {
        String playerName = player.getName().getLiteralString();
        chatPrefixModule.removePrefix(player, prefix);
        player.server.getPlayerManager().broadcast(
                Text.literal(" * " + playerName + " 回来了.").withColor(Colors.LIGHT_GRAY),
                false
        );
        SFCallbacks.PLAYER_AFK.invoker().onAFKStatus(player, false);
    }
}
