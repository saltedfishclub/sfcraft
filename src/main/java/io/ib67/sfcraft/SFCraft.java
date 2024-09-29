package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.google.inject.*;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.ConfigRoot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SFCraft extends AbstractModule {
    public static final String MOD_ID = "sfcraft";
    @Getter(AccessLevel.PUBLIC)
    static Injector injector;
    /**
     * DO NOT USE THIS DIRECTLY
     * USE {@link io.ib67.sfcraft.inject.MinecraftServerSupplier} IN DI INSTEAD.
     */
    @Deprecated(forRemoval = true)
    public static MinecraftServer server;

    @Provides
    @ConfigRoot
    @Singleton
    public static Path getRoot() {
        return Path.of(MOD_ID);
    }

    @Provides
    @SneakyThrows
    @Singleton
    @Inject
    private static SFConfig loadConfig(@ConfigRoot Path root, Gson gson) {
        if (Files.notExists(root)) {
            Files.createDirectory(root);
        }
        var cfgFile = root.resolve("config.json");
        if (Files.notExists(cfgFile)) {
            Files.writeString(cfgFile, gson.toJson(new SFConfig()));
            return new SFConfig();
        }
        return gson.fromJson(Files.readString(cfgFile), SFConfig.class);
    }
}
