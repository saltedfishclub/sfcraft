package io.ib67.sfcraft.module.game.mount;

import java.util.UUID;

/**
 * 由 Ravager / Hoglin 的 mixin 实现的鸭子接口,承载驯服坐骑的持久状态。
 */
public interface MountAccess {
    UUID sfcraft$getOwner();

    void sfcraft$setOwner(UUID owner);

    boolean sfcraft$isSaddled();

    void sfcraft$setSaddled(boolean saddled);

    int sfcraft$getDashTicks();

    void sfcraft$setDashTicks(int ticks);
}
