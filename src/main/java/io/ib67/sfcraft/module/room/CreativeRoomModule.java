package io.ib67.sfcraft.module.room;

import com.google.inject.Inject;
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
            "minecraft.command.weather",
            "minecraft.command.time",
            "minecraft.command.gamerule",
            "carpet.command.player",
            "carpet.command.track",
            "minecraft.command.setblock",
            "minecraft.command.summon");
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

    public void onPlayerJoin(ServerPlayerEntity player) {
        player.sendMessage(Text.of("你现在正在创造游乐园中，使用 //schem list 查看可用投影。"));
        player.sendMessage(Text.literal("以下功能现在可用：").withColor(Colors.WHITE)
                .append(Text.literal("/player /track /summon /setblock /time /gamerule /weather 及 WorldEdit 的所有命令").withColor(Color.MAGENTA.getRGB()).append("。")));
        player.sendMessage(Text.of("    "));
        player.sendMessage(Text.literal("如何上传投影到游乐场: ").withColor(Colors.GRAY)
                .append(Text.literal("https://github.com/saltedfishclub/sfcraft-schematics").withColor(Colors.GRAY).styled(it -> it.withUnderline(true)))       );
        player.sendMessage(Text.literal("游乐园中的生物不能逃逸到其他维度。").withColor(Colors.GRAY));
        player.sendMessage(Text.literal("使用 /reco 或重新加入游戏即可离开。").withColor(Colors.GRAY));
        player.changeGameMode(GameMode.CREATIVE);

        chatPrefixModule.applyPrefix(player, prefix);
    }

    public void onPlayerQuit(ServerPlayerEntity player) {
        System.out.println(player.getUuid());
        chatPrefixModule.removePrefix(player, prefix);
    }
}
