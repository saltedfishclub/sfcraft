package io.ib67.sfcraft.subserver;

import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface Room {
    Identifier getServerIdentifier();

    RoomPlayerManager getPlayerManager();

    void shutdown();
}
