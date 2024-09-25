package io.ib67.sfcraft.module.chat.decorator;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.util.SFConsts;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ChatPingModule extends ServerModule implements MessageDecorator {
    private static final Pattern PING = Pattern.compile("(@[\\w]+)?");
    @Inject
    MinecraftServerSupplier serverSupplier;
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
        if (!isEnabled()) {
            return message;
        }
        if (sender != null) {
            if (!SFConsts.USE_AT.hasPermission(sender)) {
                return message;
            }
        }
        var text = message.getLiteralString();
        if (text == null) return message;
        var match = PING.matcher(text);
        if (!match.find()) return message;
        var foundPlayers = new HashSet<PlayerEntity>();
        var names = List.of(serverSupplier.get().getPlayerNames());
        var r = Text.literal(match.replaceAll(it -> this.matchPlayer(it, names, foundPlayers)));
        for (PlayerEntity foundPlayer : foundPlayers) {
            if (sender != null) {
                foundPlayer.sendMessage(Text.literal(sender.getName().getLiteralString() + " 正在叫你。").withColor(Colors.LIGHT_GRAY));
            }
            foundPlayer.playSoundToPlayer(
                    SoundEvents.ENTITY_ITEM_PICKUP,
                    SoundCategory.PLAYERS,
                    0.8f,
                    0.5f
            );
        }
        return r;
    }

    private String matchPlayer(MatchResult match, List<String> playerNames, Set<PlayerEntity> foundPlayers) {
        for (int i = 0; i < match.groupCount(); i++) {
            var r = match.group();
            if (r.isEmpty()) continue;
            final var d = r.substring(1);
            return playerNames.stream()
                    .filter(it -> it.toLowerCase().startsWith(d))
                    .peek(it -> foundPlayers.add(serverSupplier.get().getPlayerManager().getPlayer(it)))
                    .findFirst()
                    .orElse(r);
        }
        return "";
    }
}
