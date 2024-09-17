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
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
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
    private Team team;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT
                .register(this::registerCommand);
        ServerPlayConnectionEvents.JOIN.register(this::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
        room = new CreativeSpaceRoom();
        roomRegistry.registerRoomType(CreativeSpaceRoom.class, CreativeSpaceRoom.WORLD, new CreativeSpaceFactory(room));
        roomRegistry.createRoomOf(CreativeSpaceRoom.class, CreativeSpaceRoom.IDENTIFIER, null, (String) null);
        roomModule.enqueuePregen(CreativeSpaceRoom.WORLD, CreativeSpaceRoom.SPAWN_POS);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
        if (!RoomModule.isVirtual(player.getUuid())) {
            return;
        }
        String playerName = player.getName().getLiteralString();
        team.getPlayerList().remove(playerName);
        player.server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
    }

    private void onJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
        if (RoomModule.isVirtual(player.getUuid())) {
            String playerName = player.getName().getLiteralString();
            team.getPlayerList().add(playerName);
        }
        player.server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
    }



    @Override
    public void onEnable() {
        if (team == null) {
            team = new Team(serverSupplier.get().getScoreboard(), "playgrd");
            team.setPrefix(Text.literal("[游乐场] ").withColor(Colors.LIGHT_YELLOW));
        }
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
