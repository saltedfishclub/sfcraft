package io.ib67.sfcraft.callback;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired by {@code /sfcraft reload}. Modules that load configuration resources
 * subscribe to this event and re-read their files when it fires.
 * Listeners should throw an unchecked exception on failure so the command can report it.
 */
public interface SFConfigReload {
    Event<SFConfigReload> EVENT = EventFactory.createArrayBacked(SFConfigReload.class,
            listeners -> () -> {
                for (SFConfigReload listener : listeners) {
                    listener.onConfigReload();
                }
            });

    void onConfigReload();
}
