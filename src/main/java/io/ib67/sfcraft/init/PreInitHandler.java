package io.ib67.sfcraft.init;

import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class PreInitHandler {
    private final AtomicBoolean initialized;

    void defend(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        if (!initialized.get()) {
            handler.disconnect(Component.nullToEmpty("Server is initializing, please wait."));
        }
    }
}
