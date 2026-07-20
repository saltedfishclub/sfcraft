package io.ib67.sfcraft.module.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /sfcraft reload:触发 {@link SFConfigReload},由各配置相关模块自行重读文件。
 */
@Log4j2
public class ReloadCommandModule extends ServerModule {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext registryAccess,
                                  Commands.CommandSelection env) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("sfcraft")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                        .requires(source -> this.isEnabled() && Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .executes(this::reload)));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        try {
            SFConfigReload.EVENT.invoker().onConfigReload();
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.sfcraft.reload.success"), true);
            return 1;
        } catch (Exception e) {
            log.error("Failed to reload sfcraft configs", e);
            context.getSource().sendFailure(
                    Component.translatable("command.sfcraft.reload.failure", e.getMessage()));
            return 0;
        }
    }
}
