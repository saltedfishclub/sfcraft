package io.ib67.sfcraft.module.manager;

import io.ib67.sfcraft.Lifecycle;
import io.ib67.sfcraft.ServerModule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuleManager {
    Collection<? extends ServerModule> getModules();

    Optional<? extends ServerModule> getModule(String name);

    Lifecycle.State getCurrentState();

    /**
     * @param state nextState, cannot be current state.
     * @return error features
     */
    List<? extends ServerModule> switchGlobalState(Lifecycle.State state);
}
