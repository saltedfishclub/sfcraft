package io.ib67.sfcraft.registry.chat;

import net.minecraft.network.message.MessageDecorator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleMessageDecorator implements MessageDecorator {
    protected final List<MessageDecorator> decorators = new ArrayList<>();

    protected SimpleMessageDecorator() {

    }

    public void registerDecorator(MessageDecorator decorator) {
        decorators.add(decorator);
    }

    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
        var targets = new ArrayList<Text>();
        targets.add(message);
        for (MessageDecorator decorator : decorators) {
            var newRd = new ArrayList<Text>();
            for (Text target : targets) {
                var r = decorator.decorate(sender, target);
                var copy = r.copy();
                copy.getSiblings().clear();
                newRd.add(copy);
                newRd.addAll(r.getSiblings());
            }
            targets = newRd;
        }
        var result = Text.empty();
        targets.forEach(result::append);
        return result;
    }
}
