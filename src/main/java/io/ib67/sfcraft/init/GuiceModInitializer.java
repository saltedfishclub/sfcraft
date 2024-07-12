package io.ib67.sfcraft.init;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.module.manager.ModuleManager;
import io.ib67.sfcraft.module.manager.SimpleModuleManager;
import lombok.AccessLevel;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GuiceModInitializer extends AbstractModule implements ModInitializer {
    public static AtomicBoolean initialized = new AtomicBoolean(false);
    @Getter(AccessLevel.PROTECTED)
    private Injector injector;
    private ModuleManager moduleManager;
    private final List<Class<? extends ServerModule>> registeredFeatures = new ArrayList<>();

    @Override
    public void onInitialize() {
        var handler = new PreInitHandler(initialized);
        ServerPlayConnectionEvents.JOIN.register(handler::defend);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        injector = onInit();
    }

    private void onServerStarted(MinecraftServer server) {
        var map = new HashMap<String, ServerModule>();
        for (Class<? extends ServerModule> registeredFeature : registeredFeatures) {
            var module = injector.getInstance(registeredFeature);
            map.put(module.getName(), module);
        }
        moduleManager = new SimpleModuleManager(ImmutableMap.copyOf(map));
        onReady(server);
        initialized.set(true);
    }

    protected abstract Injector onInit();

    protected abstract void onReady(MinecraftServer minecraftServer);

    @Provides
    protected ModuleManager getModuleManager() {
        return moduleManager;
    }

    protected void registerFeature(Class<? extends ServerModule> featureClass) {
        binder().bind(featureClass).in(Singleton.class);
    }
}
