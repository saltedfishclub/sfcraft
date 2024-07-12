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

    public void onDisable() {

    }

    public void onEnable() {

    }

    public final boolean isEnabled() {
        return state == State.ENABLED;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }
}
