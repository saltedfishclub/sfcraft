package io.ib67.sfcraft.module.chat.decorator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Function;

class ReplaceHelper {

    public static Text replace(ServerPlayerEntity sender, Text message, String s, Function<ServerPlayerEntity, Text> generator) {
        var text = message.getLiteralString();
        if (text == null || text.isEmpty()) return message;
        text = text.trim();
        var result = Text.literal("");
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
