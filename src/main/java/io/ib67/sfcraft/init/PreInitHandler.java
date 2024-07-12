package io.ib67.sfcraft.init;

import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class PreInitHandler {
    private final AtomicBoolean initialized;

    void defend(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        if (!initialized.get()) {
            handler.disconnect(Text.of("Server is initializing, please wait."));
        }
    }
}
