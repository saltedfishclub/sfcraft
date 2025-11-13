package sfcraft;

import io.ib67.sfcraft.ServerModule;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public class GameExtensionModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFBlockEntities.initialize();
        SFBlocks.initialize();
        SFItems.initialize();
    }
}
