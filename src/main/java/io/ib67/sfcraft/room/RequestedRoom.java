package io.ib67.sfcraft.room;

import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestedRoom(
        Identifier identifier,
        String profileName,
        UUID profileUuid
) {
}
