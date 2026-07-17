package io.ib67.sfcraft.registry.chat;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SimpleMessageDecorator implements ChatDecorator {
    protected final List<ChatDecorator> decorators = new ArrayList<>();

    protected SimpleMessageDecorator() {

    }

    public void registerDecorator(ChatDecorator decorator) {
        decorators.add(decorator);
    }

    @Override
    public Component decorate(@Nullable ServerPlayer sender, Component message) {
        var targets = new ArrayList<Component>();
        targets.add(message);
        for (ChatDecorator decorator : decorators) {
            var newRd = new ArrayList<Component>();
            for (Component target : targets) {
                var r = decorator.decorate(sender, target);
                var copy = r.copy();
                copy.getSiblings().clear();
                newRd.add(copy);
                newRd.addAll(r.getSiblings());
            }
            targets = newRd;
        }
        var result = Component.empty();
        targets.forEach(result::append);
        return result;
    }
}
