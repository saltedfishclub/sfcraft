package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.util.Helper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.common.ServerLinksS2CPacket;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HintModule extends ServerModule {
    public static final Identifier IDENTIFIER_ROOM = Identifier.of("sfcraft", "goto_room");
    public static Map<GameProfile, String> rooms = new HashMap<>();
    @Inject
    private MinecraftServerSupplier serverSupplier;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(this::onJoin);

        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        rooms.remove(serverPlayNetworkHandler.getPlayer().getGameProfile());
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("goto")
                .then(
                        RequiredArgumentBuilder.<ServerCommandSource, String>argument("room", StringArgumentType.greedyString())
                                .executes(this::onGoto)
                )
        );
    }

    private int onGoto(CommandContext<ServerCommandSource> ctx) {
        var room = ctx.getArgument("room", String.class);
        var player = ctx.getSource().getPlayer();
        if (player == null) {
            return -1;
        }
        var network = player.networkHandler;
        network.reconfigure();
        network.send(new StoreCookieS2CPacket(IDENTIFIER_ROOM, room.getBytes(StandardCharsets.UTF_8)), PacketCallbacks.always(()->{
            Thread.ofVirtual().start(()->{
                try {
                    Thread.sleep(1000);
                    network.sendPacket(new ServerTransferS2CPacket("localhost", serverSupplier.get().getServerPort()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }));
        return 0;
    }

    @Override
    public void onEnable() {

    }

    private void onJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        var profile = handler.player.getGameProfile();
        handler.player.sendMessage(Text.of("Your uuid: " + profile.getId()));
        handler.player.sendMessage(Text.of("Is room player: "+ Helper.isRoomPlayer(profile)));
        handler.player.sendMessage(Text.of("Room id:" + rooms.get(profile)));
    }
}
