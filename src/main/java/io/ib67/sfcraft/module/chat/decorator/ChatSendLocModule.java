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
import org.jspecify.annotations.NonNull;

import java.net.URI;

public class ChatSendLocModule extends ServerModule implements ChatDecorator {
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public @NonNull Component decorate(@Nullable ServerPlayer sender, @NonNull Component message) {
        if (!isEnabled()) return message;
        if (sender != null) {
            if (!SFConsts.USE_BROADCAST_LOCATION.hasPermission(sender)) {
                return message;
            }
        }
        return ReplaceHelper.replace(sender, message, ".xyz", ChatSendLocModule::generateLocText);
    }

    private static Component generateLocText(ServerPlayer sender) {
        if (sender == null) return Component.translatable("message.sfcraft.chat.invalid_position");
        sender.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15 * 20));
        var x = sender.blockPosition().getX();
        var y = sender.blockPosition().getY();
        var z = sender.blockPosition().getZ();
        var key = sender.level().dimension();
        return Component
                .literal(" " + x + ", " + y + ", " + z)
                .append(dimensionLabel(key))
                .append(" ")
                .withColor(Helper.fromRgb(63, 254, 254))
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(
                        "https://mc.sfclub.cc/map/?world=" + key.identifier().toString().replace(':', '_') + "&zoom=5&x=" + x + "&z=" + z
                ))));
    }

    private static Component dimensionLabel(ResourceKey<Level> registryKey) {
        if (registryKey == Level.END) {
            return Component.translatable("message.sfcraft.dimension.end");
        } else if (registryKey == Level.NETHER) {
            return Component.translatable("message.sfcraft.dimension.nether");
        } else if (registryKey == Level.OVERWORLD) {
            return Component.empty();
        } else {
            return Component.translatable("message.sfcraft.dimension.unknown");
        }
    }
}