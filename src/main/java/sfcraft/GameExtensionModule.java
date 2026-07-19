package sfcraft;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.ib67.sfcraft.ServerModule;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import sfcraft.cauldron.CauldronRecipes;
import sfcraft.combat.BeheadingHandler;
import sfcraft.effect.CommanderAuraManager;
import sfcraft.mount.MountModule;

@Log4j2
public class GameExtensionModule extends ServerModule {
    @Override
    public void onInitialize() {
        loadGameConfig();
        SFBlocks.initialize();
        SFBlockEntities.initialize();
        SFItems.initialize();
        SFEntities.initialize();
        SFMobEffects.initialize();
        CauldronRecipes.bootstrap();
        SFLoot.registerModifiers();
        BeheadingHandler.register();
        CommanderAuraManager.register();
        MountModule.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("sfcraft")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                                .executes(context -> {
                                    try {
                                        GameConfig.reload();
                                        CauldronRecipes.rebuild();
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("sfcraft gameplay.json 已重载(战利品概率需再执行 /reload 生效)"), true);
                                        return 1;
                                    } catch (Exception e) {
                                        log.error("Failed to reload gameplay.json", e);
                                        context.getSource().sendFailure(Component.literal("重载失败: " + e.getMessage()));
                                        return 0;
                                    }
                                }))));
    }

    private static void loadGameConfig() {
        try {
            GameConfig.reload();
        } catch (Exception e) {
            log.error("Cannot load gameplay.json, falling back to defaults", e);
        }
    }
}
