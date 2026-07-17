package io.ib67.sfcraft.module.chat.decorator;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ChatSendLocModule extends ServerModule implements ChatDecorator {
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public Component decorate(@Nullable ServerPlayer sender, Component message) {
        if (!isEnabled()) return message;
        if (sender != null) {
            if (!SFConsts.USE_BROADCAST_LOCATION.hasPermission(sender)) {
                return message;
            }
        }
        return ReplaceHelper.replace(sender, message, ".xyz", ChatSendLocModule::generateLocText);
    }

    private static Component generateLocText(ServerPlayer sender) {
        if (sender == null) return Component.nullToEmpty(" (invalid position) ");
        sender.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15 * 20));
        var x = sender.blockPosition().getX();
        var y = sender.blockPosition().getY();
        var z = sender.blockPosition().getZ();
        var key = sender.level().dimension();
        var world = translate(key);
        return Component
                .literal(" " + x + ", " + y + ", " + z + world + " ")
                .withColor(Helper.fromRgb(63, 254, 254))
                .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/gps " + key + " " + x + " " + y + " " + z)));
    }

    private static String translate(ResourceKey<Level> registryKey) {
        if (registryKey == Level.END) {
            return " (末地)";
        } else if (registryKey == Level.NETHER) {
            return " (地狱)";
        } else if (registryKey == Level.OVERWORLD) {
            return "";
        } else {
            return " (未知)";
        }
    }
}