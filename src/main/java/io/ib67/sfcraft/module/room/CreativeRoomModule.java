package io.ib67.sfcraft.module.room;

import com.google.inject.Inject;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
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

    public void onPlayerJoin(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.welcome"));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.features_available").withColor(CommonColors.WHITE)
                .append(Component.translatable("message.sfcraft.playground.features_list").withColor(Color.MAGENTA.getRGB())));
        player.sendSystemMessage(Component.nullToEmpty("    "));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.upload_hint").withColor(CommonColors.GRAY)
                .append(Component.literal("/upload schematic").withColor(CommonColors.BLUE).withStyle(it -> it.withUnderlined(true))));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.mobs_no_escape").withColor(CommonColors.GRAY));
        player.sendSystemMessage(Component.translatable("message.sfcraft.playground.how_to_leave").withColor(CommonColors.GRAY));
        player.setGameMode(GameType.CREATIVE);

        chatPrefixModule.applyPrefix(player, prefix);
    }

    public void onPlayerQuit(ServerPlayer player) {
        chatPrefixModule.removePrefix(player, prefix);
    }
}
