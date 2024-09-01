package io.ib67.sfcraft;

import com.google.inject.AbstractModule;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public abstract class ServerModule extends AbstractModule implements Lifecycle {
    private State state;

    @Override
    public final void onStateChange(State state) {
        try {
            switch (state) {
                case ENABLED -> onEnable();
                case DISABLED -> onDisable();
            }
            this.state = state;
        } catch (Exception exception) {
            log.error("An error occurred when " + getName() + " is changing its state into " + state,exception);
            this.state = State.ERROR;
        }
    }

    /**
     * Often called when server is shutting down. Can also be triggered by manual operation
     */
    public void onDisable() {

    }

    /**
     * Often called when server is starting. Can also be triggered by manual operation
     * In this phase you can access MinecraftServer via {@link io.ib67.sfcraft.inject.MinecraftServerSupplier}, which is injectable.
     */
    public void onEnable() {

    }

    public final boolean isEnabled() {
        return state == State.ENABLED;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }
}
