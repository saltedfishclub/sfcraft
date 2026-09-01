package io.ib67.sfcraft.mixin.common;

import io.ib67.sfcraft.network.HandshakeAddress;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Connection 从握手一路存活到 play 阶段,拿来记玩家自报的进服地址。
 */
@Mixin(Connection.class)
public class ConnectionMixin implements HandshakeAddress.Holder {
    @Unique
    private HandshakeAddress sfcraft$handshakeAddress;

    @Override
    public @Nullable HandshakeAddress sfcraft$handshakeAddress() {
        return sfcraft$handshakeAddress;
    }

    @Override
    public void sfcraft$setHandshakeAddress(@Nullable HandshakeAddress address) {
        this.sfcraft$handshakeAddress = address;
    }
}
