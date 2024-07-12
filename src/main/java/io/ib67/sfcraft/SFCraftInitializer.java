package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.*;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.registry.event.SFRandomEventRegistry;
import io.ib67.sfcraft.init.GuiceModInitializer;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.geoip.MaxMindGeoIPService;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.MotdModule;
import io.ib67.sfcraft.module.OfflineExemptModule;
import io.ib67.sfcraft.module.PlayLimitModule;
import io.ib67.sfcraft.module.WelcomeModule;
import io.ib67.sfcraft.registry.event.RandomEventRegistry;
import io.ib67.sfcraft.module.command.BackModule;
import io.ib67.sfcraft.module.command.ManagementModule;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;

@Log4j2
public class SFCraftInitializer extends GuiceModInitializer {
    @Override
    protected void configure() {
        super.configure();
        binder().bind(Gson.class).toInstance(new GsonBuilder().setPrettyPrinting().create());
        binder().bind(MinecraftServerSupplier.class).asEagerSingleton();
        registerServices();
        registerFeatures();
    }

    private void registerServices() {
        binder().bind(GeoIPService.class).to(MaxMindGeoIPService.class);
        binder().bind(RandomEventRegistry.class).to(SFRandomEventRegistry.class);
        binder().bind(SimpleMessageDecorator.class).in(Singleton.class);
    }

    private void registerFeatures() {
        registerFeature(MotdModule.class);
        registerFeature(WelcomeModule.class);
        registerFeature(OfflineExemptModule.class);
        registerFeature(PlayLimitModule.class);
        registerFeature(BackModule.class);
        registerFeature(ManagementModule.class);
    }

    @Override
    @SuppressWarnings("removal")
    protected void onReady(MinecraftServer minecraftServer) {
        log.info("SFCraft is loading");
        SFCraft.server = minecraftServer;
        SFCraft.injector = getInjector();
        getModuleManager().switchGlobalState(Lifecycle.State.ENABLED);
    }

    @Override
    protected Injector onInit() {
        return Guice.createInjector(
                this,
                new SFCraft()
        );
    }
}
