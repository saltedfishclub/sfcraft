package sfcraft;

import io.ib67.sfcraft.ServerModule;
import sfcraft.cauldron.CauldronRecipes;

public class GameExtensionModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFBlocks.initialize();
        SFBlockEntities.initialize();
        SFItems.initialize();
        SFEntities.initialize();
        CauldronRecipes.bootstrap();
        SFLoot.registerModifiers();
    }
}
