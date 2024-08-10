package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.*;
import io.ib67.sfcraft.module.*;
import io.ib67.sfcraft.module.chat.ChatPingModule;
import io.ib67.sfcraft.module.chat.ChatSendLocModule;
import io.ib67.sfcraft.module.compat.ModCompatModule;
import io.ib67.sfcraft.module.randomevt.LongNightModule;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.registry.event.SFRandomEventRegistry;
import io.ib67.sfcraft.init.GuiceModInitializer;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.geoip.MaxMindGeoIPService;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
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
        registerFeature(ElytraSpeedMeterModule.class);
        registerFeature(PlayLimitModule.class);
        registerFeature(BackModule.class);
        registerFeature(ManagementModule.class);
        registerFeature(AFKModule.class);
        registerFeature(FartFertilizerModule.class);
        registerFeature(ChatPingModule.class);
        registerFeature(ChatSendLocModule.class);
        registerFeature(LongNightModule.class);
        registerFeature(ModCompatModule.class);
    }

    @Override
    @SuppressWarnings("removal")
    protected void onReady(MinecraftServer minecraftServer) {
        log.info("SFCraft is loading");
        SFCraft.server = minecraftServer;
        getModuleManager().switchGlobalState(Lifecycle.State.ENABLED);
        log.info("Load completed! " + getModuleManager().getModules().size() + " modules were loaded.");
    }

    @Override
    protected Injector onInit() {
        SFEntityType.registerEntities();
        return SFCraft.injector = Guice.createInjector(
                new SFCraft(),
                this
        );
    }
}
