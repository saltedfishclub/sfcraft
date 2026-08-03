package io.ib67.sfcraft.callback;

import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import java.util.function.Consumer;

import static io.ib67.sfcraft.callback.Utility.*;

public interface SFCallbacks {

    Event<PlayerPreLoginCallback> PRE_LOGIN = EventFactory.createArrayBacked(PlayerPreLoginCallback.class,
            (listeners) -> (s, c, d, o) -> forEach(listeners, i -> i.onPlayerPreLogin(s, c, d, o)));
    Event<PlayerSleepCallback> PLAYER_SLEEP = EventFactory.createArrayBacked(PlayerSleepCallback.class,
            l -> (pl, pos) -> anyMatch(l, i -> i.onPlayerSleep(pl, pos), a -> a.left().isPresent())
                    .orElse(getEitherUnitR()));

    Event<PlayerDeathCallback> PLAYER_DEATH = EventFactory.createArrayBacked(PlayerDeathCallback.class,
            l -> (pl, damageSource) -> forEach(l, i -> i.onPlayerDeath(pl, damageSource)));
    Event<ServerMotdCallback> MOTD = EventFactory.createArrayBacked(ServerMotdCallback.class,
            l -> (s, c) -> first(l, t -> t.onMotd(s, c)));
    Event<PlayerIdlingCallback> PLAYER_IDLE = EventFactory.createArrayBacked(PlayerIdlingCallback.class,
            l -> (p, a) -> forEach(l, i -> i.onSwitchIdle(p, a)));
    Event<PlayerSneakingCallback> PLAYER_SNEAKING = EventFactory.createArrayBacked(PlayerSneakingCallback.class,
            l -> (p, a) -> forEach(l, i -> i.onSneaking(p, a)));
    Event<PlayerFlyingCallback> PLAYER_FLYING = EventFactory.createArrayBacked(PlayerFlyingCallback.class,
            l -> (p, t, f) -> forEach(l, i -> i.onFlyingTick(p, t, f)));
    Event<PlayerAFKCallback> PLAYER_AFK = EventFactory.createArrayBacked(PlayerAFKCallback.class,
            l -> (p, s) -> forEach(l, i -> i.onAFKStatus(p, s)));
    /** 铁砧 createResult 尾部触发:此时原版已算好结果槽,监听器可读取输入/命名并接管结果槽。 */
    Event<AnvilCreateResultCallback> ANVIL_CREATE_RESULT = EventFactory.createArrayBacked(AnvilCreateResultCallback.class,
            l -> (m, p) -> forEach(l, i -> i.onAnvilCreateResult(m, p)));

    interface AnvilCreateResultCallback {
        void onAnvilCreateResult(AnvilMenu menu, Player player);
    }

    interface PlayerAFKCallback {
        void onAFKStatus(ServerPlayer player, boolean inAFK);
    }

    interface PlayerFlyingCallback {
        void onFlyingTick(Player player, long flyingTick, boolean flying);
    }

    interface PlayerSneakingCallback {
        void onSneaking(Player player, boolean sneak);
    }

    interface PlayerIdlingCallback {
        void onSwitchIdle(ServerPlayer player, boolean afk);
    }

    interface ServerMotdCallback {
        ServerStatus onMotd(MinecraftServer server, Connection connection);
    }

    interface PlayerDeathCallback {
        void onPlayerDeath(Player player, DamageSource damageSource);
    }

    interface PlayerPreLoginCallback {
        void onPlayerPreLogin(String currentPlayer, Connection connection, Consumer<Component> disconnect, boolean offline);
    }

    interface PlayerSleepCallback {

        Either<Player.BedSleepingProblem, Unit> onPlayerSleep(Player player, BlockPos pos);
    }
}
