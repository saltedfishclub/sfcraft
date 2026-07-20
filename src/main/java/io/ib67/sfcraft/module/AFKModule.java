package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.chat.ChatPrefix;
import io.ib67.sfcraft.module.chat.ChatPrefixModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;

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
                Component.translatable("message.sfcraft.afk.prefix").withColor(CommonColors.LIGHT_GRAY),
                "afk",
                true,
                10
        );
    }


    private void onAFK(ServerPlayer player, boolean afk) {
        if (afk) {
            enAFK(player);
        } else {
            deAFK(player);
        }
    }

    private void enAFK(ServerPlayer player) {
        chatPrefixModule.applyPrefix(player, prefix);
        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.sfcraft.afk.now_afk", player.getName().tryCollapseToString()).withColor(CommonColors.LIGHT_GRAY),
                false
        );
        SFCallbacks.PLAYER_AFK.invoker().onAFKStatus(player, true);
    }

    public void deAFK(ServerPlayer player) {
        String playerName = player.getName().tryCollapseToString();
        chatPrefixModule.removePrefix(player, prefix);
        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.sfcraft.afk.back", playerName).withColor(CommonColors.LIGHT_GRAY),
                false
        );
        SFCallbacks.PLAYER_AFK.invoker().onAFKStatus(player, false);
    }
}
