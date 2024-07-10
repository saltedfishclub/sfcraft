package io.ib67.sfcraft.message.feature;

import io.ib67.sfcraft.Helper;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SendLocationFeature implements MessageDecorator {
    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
        var text = message.getLiteralString().trim();
        if (text == null) {
            return message;
        }
        if (text.trim().equals(".xyz")) {
            return generateLocText(sender);
        }
        var result = Text.literal("");
        var head = 0;
        var i = text.indexOf(".xyz");
        while (i != -1) {
            result.append(text.substring(0, i).trim());
            result.append(generateLocText(sender));
            head = i + 4;
            i = text.indexOf(".xyz", head + 1);
        }
        result.append(text.substring(head).trim());
        return result;
    }

    private static Text generateLocText(ServerPlayerEntity sender) {
        if (sender == null) return Text.of(" (invalid position) ");
        var x = sender.getBlockPos().getX();
        var y = sender.getBlockPos().getY();
        var z = sender.getBlockPos().getZ();
        var key = sender.getServerWorld().getRegistryKey();
        var world = translate(key);
        return Text
                .literal(" " + x + ", " + y + ", " + z + world + " ")
                .withColor(Helper.fromRgb(63, 254, 254))
                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gps " + key + " " + x + " " + y + " " + z)));
    }

    private static String translate(RegistryKey<World> registryKey) {
        if (registryKey == World.END) {
            return " (末地)";
        } else if (registryKey == World.NETHER) {
            return " (地狱)";
        } else if (registryKey == World.OVERWORLD) {
            return "";
        } else {
            return " (未知)";
        }
    }
}
