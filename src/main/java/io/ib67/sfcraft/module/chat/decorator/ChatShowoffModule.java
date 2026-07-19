package io.ib67.sfcraft.module.chat.decorator;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ChatShowoffModule extends ServerModule implements ChatDecorator {
    @Inject
    SimpleMessageDecorator messageDecorator;

    @Override
    public void onInitialize() {
        messageDecorator.registerDecorator(this);
    }

    @Override
    public @NonNull Component decorate(@Nullable ServerPlayer player, Component message) {
        if (!isEnabled()) {
            return message;
        }
        if(player == null || player.getMainHandItem().is(Items.AIR)) { return message; }

        return ReplaceHelper.replace(player, message, ".item", this::generateShowoffText);
    }

    private Component generateShowoffText(ServerPlayer serverPlayer) {
        var item = serverPlayer.getMainHandItem();
        return item.count() > 1 ?
                Component.literal(" "+item.count()+"x ").append(item.getDisplayName())
                : Component.literal(" ").append(item.getDisplayName());
    }
}
