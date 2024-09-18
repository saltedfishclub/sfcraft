package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.room.RequestedRoom;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.room.data.RoomWorldDataLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.ib67.sfcraft.room.RoomTeleporter.ROOM_COOKIE;
import static io.ib67.sfcraft.util.SFConsts.COMMAND_RECO;

@Log4j2
public class RoomModule extends ServerModule {
    private static final byte[] EMPTY = new byte[0];
    @Inject
    private RoomRegistry roomRegistry;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    @Inject
    private SFConfig config;
    private Key key;
    private final List<Pair<RegistryKey<World>, BlockPos>> pregenQueue = new ArrayList<>();
    private final Map<UUID, GameProfile> uuidMapper = new ConcurrentHashMap<>();
    private final Map<Identifier, GameRules> gameRulesPerRoom = new HashMap<>();

    @Override
    public void onInitialize() {
        key = getKeyFromPassword(config.serverSecret, RandomStringUtils.random(16));
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
            world.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(registryKeyBlockPosPair.getRight()), 64, Unit.INSTANCE);
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

    @SneakyThrows
    byte[] encrypt(byte[] b) {
        var cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(b);
    }

    @SneakyThrows
    byte[] decrypt(byte[] b) {
        var cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(b);
    }

    public byte[] serializeRequestedRoom(RequestedRoom room) {
        var buf = Unpooled.buffer();
        ByteBuf nbuf = null;
        try {
            Identifier.PACKET_CODEC.encode(buf, room.identifier());
            PacketCodecs.GAME_PROFILE.encode(buf, new GameProfile(room.profileUuid(), room.profileName()));
            var encrypted = encrypt(buf.array());
            nbuf = Unpooled.buffer();
            nbuf.writeShortLE(encrypted.length);
            nbuf.writeBytes(encrypted);
            return nbuf.array();
        } finally {
            if (nbuf != null) nbuf.release();
            buf.release();
        }
    }

    public RequestedRoom readRoomRequest(byte[] payload) {
        ByteBuf buf = null;
        ByteBuf decrypted = null;
        try {
            buf = Unpooled.wrappedBuffer(payload);
            var len = buf.readUnsignedShortLE();
            var content = new byte[len];
            buf.readBytes(content);
            decrypted = Unpooled.wrappedBuffer(decrypt(content));

            var identifier = Identifier.PACKET_CODEC.decode(decrypted);
            var profile = PacketCodecs.GAME_PROFILE.decode(decrypted);
            return new RequestedRoom(identifier, profile.getName(), profile.getId());
        } finally {
            if (buf != null) buf.release(); //todo
            if (decrypted != null) decrypted.release();
        }
    }

    @SneakyThrows
    private static SecretKey getKeyFromPassword(String password, String salt) {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
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

    public GameRules readGameRuleForRoom(ServerWorld world){
        return RoomWorldDataLoader.get(world).getGameRules();
    }
}
