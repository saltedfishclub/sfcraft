package io.ib67.sfcraft.module.supervisor;

import com.google.inject.Inject;
import com.google.inject.Provides;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.module.manager.ModuleManager;
import io.javalin.Javalin;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebModule extends ServerModule {
    private Javalin javalin;
    @Inject
    SFConfig config;

    @Override
    public void onEnable() {
        javalin = Javalin.create(cfg -> cfg.useVirtualThreads = false);
        var moduleManager = SFCraft.getInjector().getInstance(ModuleManager.class);
        for (ServerModule module : moduleManager.getModules()) {
            if(module instanceof WebHandler webHandler) {
                webHandler.register(javalin);
            }
        }
        Thread.ofVirtual().name("SFCraft API Web").start(() -> javalin.start(config.httpPort));
    }

    @Override
    public void onDisable() {
        javalin.stop();
    }

    @Provides
    public Javalin getJavalin(){
        return javalin;
    }

    @Override
    public void onInitialize() {

    }
}
