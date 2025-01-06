package io.ib67.sfcraft.room;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;

import java.util.UUID;

public record RequestedRoom(
        Identifier identifier,
        String profileName,
        UUID profileUuid
) {
    public static final PacketCodec<ByteBuf, RequestedRoom> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, RequestedRoom::identifier,
            PacketCodecs.STRING, RequestedRoom::profileName,
            Uuids.PACKET_CODEC, RequestedRoom::profileUuid,
            RequestedRoom::new
    );
}
