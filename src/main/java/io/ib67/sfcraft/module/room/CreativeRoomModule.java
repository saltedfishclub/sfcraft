package io.ib67.sfcraft.module.room;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.module.chat.ChatPrefix;
import io.ib67.sfcraft.module.chat.ChatPrefixModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.room.RoomTeleporter;
import io.ib67.sfcraft.room.create.CreativeSpaceFactory;
import io.ib67.sfcraft.room.create.CreativeSpaceRoom;
import io.ib67.sfcraft.util.SFConsts;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Set;

public class CreativeRoomModule extends ServerModule {
    private static final Set<String> BYPASS_PERMISSIONS = Set.of(
            "minecraft.command.gamerule",
            "carpet.command.player",
            "carpet.command.track",
            "minecraft.command.setblock",
            "minecraft.command.summon",
            "carpet.command.player.gamemode");
    @Inject
    private RoomRegistry roomRegistry;
    @Inject
    private RoomTeleporter teleporter;
    @Inject
    private RoomModule roomModule;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    @Inject
    private ChatPrefixModule chatPrefixModule;
    private CreativeSpaceRoom room;
    private ChatPrefix prefix;

    @Override
    public void onInitialize() {
        prefix = new ChatPrefix(
                Text.literal("[PLAYGRD] ").withColor(Colors.LIGHT_YELLOW),
                "playgrd",
                true,
                20
        );
        CommandRegistrationCallback.EVENT
                .register(this::registerCommand);
        PermissionCheckEvent.EVENT.register(this::onWorldEdit);
        PermissionCheckEvent.EVENT.register(this::onOtherCommands);
        room = new CreativeSpaceRoom(this);
        roomRegistry.registerRoomType(CreativeSpaceRoom.class, CreativeSpaceRoom.WORLD, new CreativeSpaceFactory(room));
        roomRegistry.createRoomOf(CreativeSpaceRoom.class, CreativeSpaceRoom.IDENTIFIER, null, (String) null);
        roomModule.enqueuePregen(CreativeSpaceRoom.WORLD, CreativeSpaceRoom.SPAWN_POS);
    }

    private @NotNull TriState onOtherCommands(@NotNull CommandSource commandSource, @NotNull String s) {
        if (commandSource instanceof ServerCommandSource source && source.getPlayer() != null) {
            var p = source.getPlayer();
            if (p.getServerWorld().getRegistryKey().equals(CreativeSpaceRoom.WORLD)) {
                if (BYPASS_PERMISSIONS.contains(s)) {
                    return TriState.TRUE;
                }
            }
        }
        return TriState.DEFAULT;
    }

    private @NotNull TriState onWorldEdit(@NotNull CommandSource commandSource, @NotNull String s) {
        if (commandSource instanceof ServerCommandSource source && source.getPlayer() != null) {
            var p = source.getPlayer();
            if (p.getServerWorld().getRegistryKey().equals(CreativeSpaceRoom.WORLD)) {
                if (s.startsWith("worldedit") && SFConsts.WORLDEDIT_AT_PLAYGROUND.hasPermission(p)) {
                    return TriState.TRUE;
                }
            }
        }
        return TriState.DEFAULT;
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registry, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("playgrd")
                        .requires(it -> it.isExecutedByPlayer() && SFConsts.COMMAND_PLAYGROUND.hasPermission(it.getPlayer()))
                        .executes(this::gotoPlayground)
        );
        dispatcher.register(
                CommandManager.literal("gm")
                        .then(CommandManager.argument("mode", GameModeArgumentType.gameMode())
                                .requires(it -> it.isExecutedByPlayer() && SFConsts.COMMAND_PLAYGROUND_GAMEMODE.hasPermission(it.getPlayer()))
                                .executes(this::onGameMode)
                        )
        );
    }

    private int onGameMode(CommandContext<ServerCommandSource> ctx) {
        var src = ctx.getSource();
        if (src instanceof ServerCommandSource source && source.getPlayer() != null) {
            var p = source.getPlayer();
            var mode = ctx.getArgument("mode", GameMode.class);
            if (p.getServerWorld().getRegistryKey().equals(CreativeSpaceRoom.WORLD)) {
                p.changeGameMode(mode);
                p.sendMessage(Text.of("Your gamemode has been changed to "+mode.asString()));
            }else{
                p.sendMessage(Text.of("You can only use this in playground!"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int gotoPlayground(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        try {
            teleporter.teleportTo(room, player);
        } catch (Exception e) {
            player.networkHandler.disconnect(Text.of(e.getMessage()));
            e.printStackTrace();
        }
        return 0;
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        player.sendMessage(Text.of("你现在正在创造游乐园中，使用 //schem list 查看可用投影。"));
        player.sendMessage(Text.literal("以下功能现在可用：").withColor(Colors.WHITE)
                .append(Text.literal("/player /track /summon /setblock /gamerule 及 WorldEdit 的所有命令").withColor(Color.MAGENTA.getRGB()).append("。")));
        player.sendMessage(Text.of("    "));
        player.sendMessage(Text.literal("如何上传投影到游乐场: ").withColor(Colors.GRAY)
                .append(Text.literal("https://github.com/saltedfishclub/sfcraft-schematics").withColor(Colors.GRAY).styled(it -> it.withUnderline(true))));
        player.sendMessage(Text.literal("游乐园中的生物不能逃逸到其他维度。").withColor(Colors.GRAY));
        player.sendMessage(Text.literal("使用 /reco 或重新加入游戏即可离开。").withColor(Colors.GRAY));
        player.changeGameMode(GameMode.CREATIVE);

        chatPrefixModule.applyPrefix(player, prefix);
    }

    public void onPlayerQuit(ServerPlayerEntity player) {
        chatPrefixModule.removePrefix(player, prefix);
    }
}
