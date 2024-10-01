package io.ib67.sfcraft.module;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.SFBlocks;
import io.ib67.sfcraft.SFRegistries;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.block.SFBlockState;
import io.ib67.sfcraft.util.HackedBlockStateIdList;
import io.ib67.sfcraft.util.SFConsts;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.collection.IdList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Log4j2
public class CustomBlockModule extends ServerModule {
    @Override
    @SneakyThrows
    public void onInitialize() {
        SFBlocks.init();
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        var cmd = CommandManager.literal("sfsetblock")
                //.requires(it -> it.isExecutedByPlayer() && SFConsts.COMMAND_SFSETBLOCK.hasPermission(it.getPlayer()))
                .then(CommandManager.argument("type", IdentifierArgumentType.identifier())
                        .executes(this::performSetBlock));
        dispatcher.register(cmd);
    }

    private int performSetBlock(CommandContext<ServerCommandSource> ctx) {
        var id = IdentifierArgumentType.getIdentifier(ctx, "type");
        var block = SFRegistries.BLOCKS.get(id);
        var player = ctx.getSource().getPlayer();
        var world = player.getWorld();
        var state = block.getDefaultState();
        world.setBlockState(player.getBlockPos(), state);
        return 1;
    }
}
