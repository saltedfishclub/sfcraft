package io.ib67.sfcraft.message;

import io.ib67.sfcraft.message.feature.PingPlayerFeature;
import io.ib67.sfcraft.message.feature.SendLocationFeature;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SFMessageDecorator extends SimpleMessageDecorator {
    public SFMessageDecorator() {
        registerDecorator(new SendLocationFeature());
        registerDecorator(new PingPlayerFeature());
    }

}
