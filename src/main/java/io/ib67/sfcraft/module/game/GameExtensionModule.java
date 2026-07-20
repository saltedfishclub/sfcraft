package io.ib67.sfcraft.module.game;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import io.ib67.sfcraft.config.GameConfigService;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * Owns the lifecycle of gameplay.json ({@link GameConfigService}): performs the initial load
 * before the other game modules register their content, and re-reads it on
 * {@link SFConfigReload} (fired by /sfcraft reload).
 * <p>
 * Must be registered before all other {@code module.game} features, some of which bake
 * config values into registry content (e.g. reverse pearl token durability).
 */
@Log4j2
public class GameExtensionModule extends ServerModule {
    @Inject
    private GameConfigService gameConfig;

    @Override
    public void onInitialize() {
        try {
            gameConfig.reload();
        } catch (Exception e) {
            log.error("Cannot load gameplay.json, falling back to defaults", e);
        }
        SFConfigReload.EVENT.register(() -> {
            try {
                gameConfig.reload();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot reload gameplay.json: " + e.getMessage(), e);
            }
        });
    }
}
