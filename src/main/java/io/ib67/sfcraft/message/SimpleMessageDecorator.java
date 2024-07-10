package io.ib67.sfcraft.message;

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

    protected void registerDecorator(MessageDecorator decorator) {
        decorators.add(decorator);
    }

    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
        for (MessageDecorator decorator : decorators) {
            message = decorator.decorate(sender, message);
        }
        return message;
    }
}
