package io.ib67.sfcraft.room;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record RequestedRoom(
        Identifier identifier,
        String profileName,
        UUID profileUuid
) {
    public static final StreamCodec<ByteBuf, RequestedRoom> PACKET_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, RequestedRoom::identifier,
            ByteBufCodecs.STRING_UTF8, RequestedRoom::profileName,
            UUIDUtil.STREAM_CODEC, RequestedRoom::profileUuid,
            RequestedRoom::new
    );
}
