package io.ib67.sfcraft.module.room;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.room.RoomTeleporter;
import io.ib67.sfcraft.room.create.CreativeSpaceFactory;
import io.ib67.sfcraft.room.create.CreativeSpaceRoom;
import io.ib67.sfcraft.util.SFConsts;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.chunk.ChunkStatus;

public class CreativeRoomModule extends ServerModule {
    @Inject
    private RoomRegistry roomRegistry;
    @Inject
    private RoomTeleporter teleporter;
    @Inject
    private RoomModule roomModule;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    private CreativeSpaceRoom room;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT
                .register(this::registerCommand);
        room = new CreativeSpaceRoom();
        roomRegistry.registerRoomType(CreativeSpaceRoom.class, CreativeSpaceRoom.WORLD, new CreativeSpaceFactory(room));
        roomRegistry.createRoomOf(CreativeSpaceRoom.class, CreativeSpaceRoom.IDENTIFIER, null, null);
        roomModule.enqueuePregen(CreativeSpaceRoom.WORLD,CreativeSpaceRoom.SPAWN_POS);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registry, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("playground")
                        .requires(it -> it.isExecutedByPlayer() && SFConsts.COMMAND_PLAYGROUND.hasPermission(it.getPlayer()))
                        .executes(this::gotoPlayground)
        );
    }

    private int gotoPlayground(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        teleporter.teleportTo(room, player);
        return 0;
    }
}
