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
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
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
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        injector = onInit();
        var map = new HashMap<String, ServerModule>();
        for (Class<? extends ServerModule> registeredFeature : registeredFeatures) {
            var module = injector.getInstance(registeredFeature);
            try {
                module.onInitialize();
                map.put(module.getName(), module);
            } catch (Exception e) {
                log.error("Cannot initialize module " + module.getName(), e);
            }
        }
        moduleManager = new SimpleModuleManager(ImmutableMap.copyOf(map));
    }

    private void onServerStopping(MinecraftServer minecraftServer) {
        onStopping(minecraftServer);
    }

    private void onServerStarted(MinecraftServer server) {
        onReady(server);
        initialized.set(true);
    }

    protected abstract Injector onInit();

    protected void onReady(MinecraftServer minecraftServer) {
    }

    protected void onStopping(MinecraftServer minecraftServer) {
    }

    @Provides
    protected ModuleManager getModuleManager() {
        return moduleManager;
    }

    protected void registerFeature(Class<? extends ServerModule> featureClass) {
        binder().bind(featureClass).in(Singleton.class);
        registeredFeatures.add(featureClass);
    }
}
