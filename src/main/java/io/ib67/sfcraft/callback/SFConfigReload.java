package io.ib67.sfcraft.callback;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired by {@code /sfcraft reload}. Modules that load configuration resources
 * subscribe to this event and re-read their files when it fires.
 * Listeners should throw an unchecked exception on failure so the command can report it.
 *
 * <p>Listeners are isolated: one failing listener does not stop the others from reloading.
 * Every listener runs, then the failures are aggregated and rethrown so the command still
 * reports the error instead of silently leaving a half-reloaded state.
 */
public interface SFConfigReload {
    Event<SFConfigReload> EVENT = EventFactory.createArrayBacked(SFConfigReload.class,
            listeners -> () -> {
                List<Exception> errors = new ArrayList<>();
                for (SFConfigReload listener : listeners) {
                    try {
                        listener.onConfigReload();
                    } catch (Exception e) {
                        errors.add(e);
                    }
                }
                if (!errors.isEmpty()) {
                    var aggregate = new RuntimeException(errors.size() + " config listener(s) failed to reload");
                    errors.forEach(aggregate::addSuppressed);
                    throw aggregate;
                }
            });

    void onConfigReload();
}
