package io.ib67.sfcraft.module.chat;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ChatSendLocModule extends ServerModule implements MessageDecorator {
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
        if (!isEnabled()) return message;
        if (sender != null) {
            if (!SFConsts.USE_BROADCAST_LOCATION.hasPermission(sender)) {
                return message;
            }
        }
        return ReplaceHelper.replace(sender, message, ".xyz", ChatSendLocModule::generateLocText);
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