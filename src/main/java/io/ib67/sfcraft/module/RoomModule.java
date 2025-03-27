package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.registry.RoomRegistry;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

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
    private final List<Pair<RegistryKey<World>, BlockPos>> pregenQueue = new ArrayList<>();
    private final Map<UUID, GameProfile> uuidMapper = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    @Override
    public void onEnable() {
        for (Pair<RegistryKey<World>, BlockPos> registryKeyBlockPosPair : pregenQueue) {
            var world = serverSupplier.get().getWorld(registryKeyBlockPosPair.getLeft());
            if (world == null) {
                log.warn("Cannot find world {}", registryKeyBlockPosPair.getLeft());
                continue;
            }
            world.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(registryKeyBlockPosPair.getRight()), 64);
        }
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> serverCommandSourceCommandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        serverCommandSourceCommandDispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("reco")
                        .requires(it -> it.isExecutedByPlayer() && COMMAND_RECO.hasPermission(it.getPlayer()))
                        .executes(this::onCleanReconnect)
        );
    }

    private int onCleanReconnect(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        var player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        if (!isVirtual(player.getUuid())) {
            player.sendMessage(Text.literal("你不在任何 \"房间\" 内。"));
            return 0;
        }
        var networkHandler = player.networkHandler;
        networkHandler.send(new StoreCookieS2CPacket(ROOM_COOKIE, EMPTY), PacketCallbacks.always(() -> {
            networkHandler.sendPacket(new ServerTransferS2CPacket(config.domain, serverSupplier.get().getServerPort()));
        }));
        return 1;
    }

    public void enqueuePregen(RegistryKey<World> world, BlockPos spawnPos) {
        pregenQueue.add(new Pair<>(world, spawnPos));
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
