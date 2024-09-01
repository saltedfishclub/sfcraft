package io.ib67.sfcraft;

public interface Lifecycle {
    /**
     * Invoke at ModInit (registries are open, etc.)
     */
    void onInitialize();

    void onStateChange(State state);

    enum State {
        ENABLED, DISABLED,ERROR
    }
}
