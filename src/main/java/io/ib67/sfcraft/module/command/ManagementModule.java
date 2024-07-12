package io.ib67.sfcraft.module.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.util.Permission;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.lang.reflect.Field;

public class ManagementModule extends ServerModule {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        serverPlayNetworkHandler.getPlayer().removeCommandTag(SFConsts.SPECIAL_SUDO);
    }

    private void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment env) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sudo")
                .requires(it -> this.isEnabled())
                .requires(it -> it.hasPermissionLevel(2) && it.getPlayer() != null)
                .executes(this::enterSudoMode)
        );
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("listperm")
                .requires(it -> this.isEnabled())
                .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_LISTPERM.hasPermission(it.getPlayer()))
                .executes(this::listPerms)
        );
    }

    public int enterSudoMode(CommandContext<ServerCommandSource> ctx) {
        var player = ctx.getSource().getPlayer();
        if (player.getCommandTags().contains(SFConsts.SPECIAL_SUDO)) {
            player.removeCommandTag(SFConsts.SPECIAL_SUDO);
            player.sendMessage(Text.of("Sudo is off.").copy().withColor(Colors.GREEN));
        } else {
            player.addCommandTag(SFConsts.SPECIAL_SUDO);
            player.sendMessage(Text.of("Sudo is on.").copy().withColor(Colors.GREEN));
        }
        return 0;
    }

    @SneakyThrows
    public int listPerms(CommandContext<ServerCommandSource> ctx) {
        var src = ctx.getSource();
        src.sendMessage(Text.of("Permissions: ").copy().withColor(Colors.GREEN));
        for (Field declaredField : SFConsts.class.getDeclaredFields()) {
            if (Permission.class.isAssignableFrom(declaredField.getType())) {
                var permission = (Permission<?>) declaredField.get(null);
                src.sendMessage(Text.of(" - " + permission.key() + " " + (permission.byDefault() ? "(default)" : "")));
            }
        }
        return 0;
    }
}
