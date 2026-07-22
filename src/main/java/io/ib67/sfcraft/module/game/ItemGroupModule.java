package io.ib67.sfcraft.module.game;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.module.game.cauldron.CauldronModule;
import io.ib67.sfcraft.registry.ItemGroupService;

/**
 * Finalizes the mod's creative inventory tab. Must be the LAST feature registered in
 * {@code registerGameFeatures()} so that, by the time its {@link #onInitialize()} runs, every
 * other feature module has already contributed its items to {@link ItemGroupService}. This module
 * only freezes the passively-collected set — it never registers items itself.
 * <p>
 * The tab icon is the amethyst cauldron, pulled from {@link CauldronModule} (registered earlier,
 * so its item is available here) to keep the icon type-safe rather than looked up by string id.
 */
public class ItemGroupModule extends ServerModule {
    @Inject
    private ItemGroupService itemGroups;
    @Inject
    private CauldronModule cauldron;

    @Override
    public void onInitialize() {
        itemGroups.freeze(cauldron.getCauldronItem());
    }
}
