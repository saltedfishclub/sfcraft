package io.ib67.sfcraft.module.chat.decorator;

import java.util.Locale;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

class ReplaceHelper {

    public static Component replace(ServerPlayer sender, Component message, String s, Function<ServerPlayer, Component> generator) {
        var text = message.tryCollapseToString();
        if (text == null || text.isEmpty()) return message;
        text = text.trim();
        var result = Component.literal("");
        var head = 0;
        var lowercase = text.toLowerCase(Locale.ROOT);
        var i = lowercase.indexOf(s);
        while (i != -1) {
            result.append(text.substring(0, i).trim());
            result.append(generator.apply(sender));
            head = i + s.length();
            i = lowercase.indexOf(s, head + 1);
        }
        result.append(text.substring(head).trim());
        return result;
    }
}
