package io.ib67.sfcraft;

public interface Lifecycle {
    void onInitialize();

    void onStateChange(State state);

    enum State {
        ENABLED, DISABLED,ERROR;
    }
}
