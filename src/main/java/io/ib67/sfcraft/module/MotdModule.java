package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.ConfigResources;
import io.ib67.sfcraft.inject.ConfigResource;
import io.ib67.sfcraft.inject.ConfigRoot;

import java.nio.file.Path;

public class MotdModule extends ServerModule {
    @Inject
    @ConfigRoot
    private Path configRoot;
    @Inject
    private MinecraftServerSupplier minecraftServer;
    protected String motd;
    protected String originalMotd;

    @Override
    public void onInitialize() {
        motd = getMotd(configRoot);
    }

    @Override
    public void onEnable() {
        originalMotd = minecraftServer.get().getServerMotd();
        minecraftServer.get().setMotd(motd);
    }

    @Override
    public void onDisable() {
        minecraftServer.get().setMotd(originalMotd);
    }

    @Provides
    @ConfigResource(ConfigResources.MOTD)
    @Inject
    @Singleton
    private static String getMotd(@ConfigRoot Path root) {
        return Helper.getConfigResource(root, ConfigResources.MOTD).orElse("SaltedFish Club Server");
    }

}
