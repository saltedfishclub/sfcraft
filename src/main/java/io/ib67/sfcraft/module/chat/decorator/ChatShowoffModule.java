package io.ib67.sfcraft.module.chat.decorator;

import com.google.inject.Inject;
import io.ib67.kiwi.routine.Fail;
import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Some;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

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
        if (player == null) {
            return message;
        }

        var comp = generateShowoffText(player.getMainHandItem(), player)
                .orElseGet(generateShowoffText(player.getOffhandItem(), player)::result);

        if (comp == null) return message;
        return ReplaceHelper.replace(player, message, ".item", p -> comp);
    }

    private Result<Component> generateShowoffText(@Nullable ItemStack item, ServerPlayer serverPlayer) {
        if (item == null || item.is(Items.AIR)) return Fail.none();
        return new Some<>(item.count() > 1 ?
                Component.literal(" " + item.count() + "x ").append(item.getDisplayName())
                : Component.literal(" ").append(item.getDisplayName()));
    }
}
