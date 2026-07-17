package io.ib67.sfcraft.module.chat.decorator;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;

public class ChatPingModule extends ServerModule implements ChatDecorator {
    private static final Pattern PING = Pattern.compile("(@[\\w]+)?"); //todo remove the need of prefix
    @Inject
    MinecraftServerSupplier serverSupplier;
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public Component decorate(@Nullable ServerPlayer sender, Component message) {
        if (!isEnabled()) {
            return message;
        }
        if (sender != null) {
            if (!SFConsts.USE_AT.hasPermission(sender)) {
                return message;
            }
        }
        var text = message.tryCollapseToString();
        if (text == null) return message;
        var match = PING.matcher(text);
        if (!match.find()) return message;
        var foundPlayers = new HashSet<Player>();
        var names = List.of(serverSupplier.get().getPlayerNames());
        var r = Component.literal(match.replaceAll(it -> this.matchPlayer(it, names, foundPlayers)));
        for (Player foundPlayer : foundPlayers) {
            if (sender != null) {
                foundPlayer.sendOverlayMessage(Component.literal(sender.getName().tryCollapseToString() + " 正在叫你。").withColor(CommonColors.LIGHT_GRAY));
            }
            Helper.playNotifySound(
                    (ServerPlayer) foundPlayer,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.8f,
                    0.5f
            );
        }
        return r;
    }

    private String matchPlayer(MatchResult match, List<String> playerNames, Set<Player> foundPlayers) {
        for (int i = 0; i < match.groupCount(); i++) {
            var r = match.group();
            if (r.isEmpty()) continue;
            final var d = r.substring(1);
            return playerNames.stream()
                    .filter(it -> it.toLowerCase().startsWith(d))
                    .peek(it -> foundPlayers.add(serverSupplier.get().getPlayerList().getPlayerByName(it)))
                    .findFirst()
                    .orElse(r);
        }
        return "";
    }
}
