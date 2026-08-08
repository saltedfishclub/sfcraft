package io.ib67.sfcraft.module.room;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Set;

public class CreativeRoomModule extends ServerModule {
    private static final Set<String> BYPASS_PERMISSIONS = Set.of(
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
                Component.literal("[PLAYGRD] ").withColor(CommonColors.SOFT_YELLOW),
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

    private @NotNull TriState onOtherCommands(@NotNull SharedSuggestionProvider commandSource, @NotNull String s) {
        if (commandSource instanceof CommandSourceStack source && source.getPlayer() != null) {
            var p = source.getPlayer();
            if (p.level().dimension().equals(CreativeSpaceRoom.WORLD)) {
                if (BYPASS_PERMISSIONS.contains(s)) {
                    return TriState.TRUE;
                }
            }
        }
        return TriState.DEFAULT;
    }

    private @NotNull TriState onWorldEdit(@NotNull SharedSuggestionProvider commandSource, @NotNull String s) {
        if (commandSource instanceof CommandSourceStack source && source.getPlayer() != null) {
            var p = source.getPlayer();
            if (p.level().dimension().equals(CreativeSpaceRoom.WORLD)) {
                if (s.startsWith("worldedit") && SFConsts.WORLDEDIT_AT_PLAYGROUND.hasPermission(p)) {
                    return TriState.TRUE;
                }
            }
        }
        return TriState.DEFAULT;
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext registry, Commands.CommandSelection env) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("playgrd")
                        .requires(it -> it.isPlayer() && SFConsts.COMMAND_PLAYGROUND.hasPermission(it.getPlayer()))
                        .executes(this::gotoPlayground)
                        .then(Commands.literal("time")
                                .requires(it -> it.isPlayer() && SFConsts.COMMAND_PLAYGROUND_TIME.hasPermission(it.getPlayer()))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("time", TimeArgument.time())
                                                .executes(ctx -> onSetTime(ctx, IntegerArgumentType.getInteger(ctx, "time")))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("time", TimeArgument.time(Integer.MIN_VALUE))
                                                .executes(ctx -> onAddTime(ctx, IntegerArgumentType.getInteger(ctx, "time")))))
                                .then(Commands.literal("query").executes(this::onQueryTime))
                                .then(Commands.literal("day").executes(ctx -> onSetNamedTime(ctx, 1000)))
                                .then(Commands.literal("noon").executes(ctx -> onSetNamedTime(ctx, 6000)))
                                .then(Commands.literal("night").executes(ctx -> onSetNamedTime(ctx, 13000)))
                                .then(Commands.literal("midnight").executes(ctx -> onSetNamedTime(ctx, 18000)))
                        )
        );
        dispatcher.register(
                Commands.literal("gm")
                        .then(Commands.argument("mode", GameModeArgument.gameMode())
                                .requires(it -> it.isPlayer() && SFConsts.COMMAND_PLAYGROUND_GAMEMODE.hasPermission(it.getPlayer()))
                                .executes(this::onGameMode)
                        )
        );
        dispatcher.register(
                Commands.literal("pt")
                        .then(Commands.argument("dest", EntityArgument.entity())
                                .requires(it -> it.isPlayer() && SFConsts.COMMAND_PLAYGROUND_TELEPORT.hasPermission(it.getPlayer()))
                                .executes(this::onTeleport))
        );
    }

    private int onTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        if (src instanceof CommandSourceStack source && source.getPlayer() != null) {
            var p = source.getPlayer();
            var dest = EntityArgument.getEntity(ctx, "dest");
            if (p.level().dimension().equals(CreativeSpaceRoom.WORLD)) {
                if (dest.level() == p.level()) {
                    Helper.teleportSafely(
                            p, p.level(),
                            dest.getBlockX(), dest.getBlockY(), dest.getBlockZ(),
                            dest.getYRot(), dest.getXRot()
                    );
                    return Command.SINGLE_SUCCESS;
                }
            }
            p.sendSystemMessage(Component.translatable("message.sfcraft.playground.must_be_in"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int onGameMode(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        if (src instanceof CommandSourceStack source && source.getPlayer() != null) {
            var p = source.getPlayer();
            var mode = ctx.getArgument("mode", GameType.class);
            if (p.level().dimension().equals(CreativeSpaceRoom.WORLD)) {
                p.setGameMode(mode);
                p.sendSystemMessage(Component.translatable("message.sfcraft.playground.gamemode_changed", mode.getSerializedName()));
            } else {
                p.sendSystemMessage(Component.translatable("message.sfcraft.playground.only_here"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int gotoPlayground(CommandContext<CommandSourceStack> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        try {
            teleporter.teleportTo(room, player);
        } catch (Exception e) {
            player.connection.disconnect(Component.nullToEmpty(e.getMessage()));
            e.printStackTrace();
        }
        return 0;
    }

    // ── /playgrd time ────────────────────────────────────────────────
    // 只操作游乐场自己的 WorldClock(sfcraft:playground),绝不触碰其它钟;
    // 必须身处游乐场才能使用。时间值存原始 tick(可超 24000),展示时按 24000 取余。

    private int onSetTime(CommandContext<CommandSourceStack> ctx, int ticks) {
        var player = requirePlaygroundPlayer(ctx);
        if (player == null) return Command.SINGLE_SUCCESS;
        ctx.getSource().getServer().clockManager().setTotalTicks(playgroundClock(ctx.getSource()), ticks);
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.time_set", wrapTime(ticks)));
        return Command.SINGLE_SUCCESS;
    }

    private int onAddTime(CommandContext<CommandSourceStack> ctx, int ticks) {
        var player = requirePlaygroundPlayer(ctx);
        if (player == null) return Command.SINGLE_SUCCESS;
        ctx.getSource().getServer().clockManager().addTicks(playgroundClock(ctx.getSource()), ticks);
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.time_added", wrapTime(ticks)));
        return Command.SINGLE_SUCCESS;
    }

    private int onQueryTime(CommandContext<CommandSourceStack> ctx) {
        var player = requirePlaygroundPlayer(ctx);
        if (player == null) return Command.SINGLE_SUCCESS;
        long ticks = ctx.getSource().getServer().clockManager().getTotalTicks(playgroundClock(ctx.getSource()));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.time_query", wrapTime(ticks)));
        return Command.SINGLE_SUCCESS;
    }

    private int onSetNamedTime(CommandContext<CommandSourceStack> ctx, int ticks) {
        var player = requirePlaygroundPlayer(ctx);
        if (player == null) return Command.SINGLE_SUCCESS;
        ctx.getSource().getServer().clockManager().setTotalTicks(playgroundClock(ctx.getSource()), ticks);
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.time_set", wrapTime(ticks)));
        return Command.SINGLE_SUCCESS;
    }

    /** 游乐场世界钟;必须身处游乐场(此时世界必然已加载),从维度类型解析。 */
    private Holder<WorldClock> playgroundClock(CommandSourceStack source) {
        var level = source.getServer().getLevel(CreativeSpaceRoom.WORLD);
        if (level == null) {
            throw new IllegalStateException("playground level not loaded");
        }
        return level.dimensionType().defaultClock()
                .orElseThrow(() -> new IllegalStateException("playground dimension type has no default clock"));
    }

    private ServerPlayer requirePlaygroundPlayer(CommandContext<CommandSourceStack> ctx) {
        var p = ctx.getSource().getPlayer();
        if (p == null || !p.level().dimension().equals(CreativeSpaceRoom.WORLD)) {
            if (p != null) {
                p.sendSystemMessage(Component.translatable("message.sfcraft.playground.only_here"));
            }
            return null;
        }
        return p;
    }

    private static int wrapTime(long ticks) {
        return (int) Math.floorMod(ticks, 24000L);
    }

    public void onPlayerJoin(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.welcome"));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.features_available").withColor(CommonColors.WHITE)
                .append(Component.translatable("message.sfcraft.playground.features_list").withColor(Color.MAGENTA.getRGB())));
        player.sendSystemMessage(Component.nullToEmpty("    "));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.upload_hint").withColor(CommonColors.GRAY)
                .append(Component.literal("/upload schematic").withColor(CommonColors.SOFT_YELLOW).withStyle(it -> it.withUnderlined(true))));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.mobs_no_escape").withColor(CommonColors.GRAY));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.how_to_leave").withColor(CommonColors.GRAY));
        player.setGameMode(GameType.CREATIVE);

        chatPrefixModule.applyPrefix(player, prefix);
    }

    public void onPlayerQuit(ServerPlayer player) {
        chatPrefixModule.removePrefix(player, prefix);
    }
}
