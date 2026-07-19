package io.ib67.sfcraft.config;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ib67.sfcraft.inject.ConfigRoot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and holds {@link GameConfig} (sfcraft/gameplay.json). The initial load happens in
 * {@link io.ib67.sfcraft.module.game.GameExtensionModule}, which also re-reads it on
 * {@link io.ib67.sfcraft.callback.SFConfigReload}.
 */
@Singleton
public class GameConfigService {
    private final Path root;
    private final Gson gson;
    private volatile GameConfig config = new GameConfig();

    @Inject
    public GameConfigService(@ConfigRoot Path root, Gson gson) {
        this.root = root;
        this.gson = gson;
    }

    public GameConfig get() {
        return config;
    }

    public synchronized void reload() throws IOException {
        if (Files.notExists(root)) {
            Files.createDirectories(root);
        }
        var file = root.resolve("gameplay.json");
        if (Files.notExists(file)) {
            Files.writeString(file, gson.toJson(new GameConfig()));
            config = new GameConfig();
            return;
        }
        var loaded = gson.fromJson(Files.readString(file), GameConfig.class);
        if (loaded == null) {
            throw new IOException("gameplay.json is empty");
        }
        config = loaded;
    }
}
