package io.ib67.sfcraft.module.manager;

import io.ib67.sfcraft.Lifecycle;
import io.ib67.sfcraft.ServerModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class SimpleModuleManager implements ModuleManager {
    private final Map<String, ServerModule> modules;
    @Getter
    private Lifecycle.State currentState;

    @Override
    public List<? extends ServerModule> switchGlobalState(Lifecycle.State state) {
        requireNonNull(state, "state cannot be null");
        if (currentState == state) {
            throw new IllegalArgumentException("currentState == targetState == " + state);
        }
        if (state == Lifecycle.State.ERROR) {
            throw new IllegalArgumentException("targetState == " + state);
        }
        var result = new ArrayList<ServerModule>();
        for (ServerModule value : modules.values()) {
            value.onStateChange(state);
            if (value.getState() == Lifecycle.State.ERROR) {
                result.add(value);
            }
        }
        currentState = state;
        return result;
    }

    @Override
    public Collection<? extends ServerModule> getModules() {
        return new ArrayList<>(modules.values());
    }

    @Override
    public Optional<? extends ServerModule> getModule(String name) {
        requireNonNull(name, "name cannot be null");
        return Optional.ofNullable(modules.get(name));
    }
}
