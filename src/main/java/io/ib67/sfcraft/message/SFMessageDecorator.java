package io.ib67.sfcraft.message;

import io.ib67.sfcraft.message.feature.SendLocationFeature;

public class SFMessageDecorator extends SimpleMessageDecorator {
    public SFMessageDecorator() {
        registerDecorator(new SendLocationFeature());
    }
}
