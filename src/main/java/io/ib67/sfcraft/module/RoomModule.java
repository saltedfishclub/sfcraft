package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.kiwi.tuple.Tuple2;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.registry.RoomRegistry;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.ib67.sfcraft.room.RoomTeleporter.ROOM_COOKIE;
import static io.ib67.sfcraft.util.SFConsts.COMMAND_RECO;

@Log4j2
public class RoomModule extends ServerModule {
    public static final String SIGN_TOPIC = "room";
    private static final byte[] EMPTY = new byte[0];
    @Inject
    private RoomRegistry roomRegistry;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    @Inject
    private SFConfig config;
    private final List<Tuple2<ResourceKey<Level>, BlockPos>> pregenQueue = new ArrayList<>();
    private final Map<UUID, GameProfile> uuidMapper = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    @Override
    public void onEnable() {
        for (Tuple2<ResourceKey<Level>, BlockPos> registryKeyBlockPosPair : pregenQueue) {
            var world = serverSupplier.get().getLevel(registryKeyBlockPosPair.a());
            if (world == null) {
                log.warn("Cannot find world {}", registryKeyBlockPosPair.b());
                continue;
            }
            var blockPos = registryKeyBlockPosPair.b();
            var chunkPos = new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
            world.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, chunkPos, 64);
        }
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> serverCommandSourceCommandDispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
        serverCommandSourceCommandDispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("reco")
                        .requires(it -> it.isPlayer() && COMMAND_RECO.hasPermission(it.getPlayer()))
                        .executes(this::onCleanReconnect)
        );
    }

    private int onCleanReconnect(CommandContext<CommandSourceStack> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        if (!isVirtual(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.sfcraft.room.not_in_room"));
            return 0;
        }
        var networkHandler = player.connection;
        networkHandler.send(new ClientboundStoreCookiePacket(ROOM_COOKIE, EMPTY), PacketSendListener.thenRun(() -> {
            networkHandler.send(new ClientboundTransferPacket(config.domain, serverSupplier.get().getPort()));
        }));
        return 1;
    }

    public void enqueuePregen(ResourceKey<Level> world, BlockPos spawnPos) {
        pregenQueue.add(new Tuple2<>(world, spawnPos));
    }

    public UUID generateIdForRoom(GameProfile issuer, String name, Identifier room) {
        var uuid = UUID.nameUUIDFromBytes((name + "@" + room.toString()).getBytes(StandardCharsets.UTF_8));
        var result = new UUID(uuid.getMostSignificantBits(), 0);
        if (issuer != null) uuidMapper.put(result, issuer);
        return result;
    }

    public static boolean isVirtual(UUID uuid) {
        return uuid.getLeastSignificantBits() == 0;
    }

    public GameProfile devirtualize(UUID virtual) {
        return uuidMapper.get(virtual);
    }
}
