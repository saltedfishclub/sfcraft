package io.ib67.sfcraft.callback;

import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;

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

    interface PlayerAFKCallback {
        void onAFKStatus(ServerPlayerEntity player, boolean inAFK);
    }

    interface PlayerFlyingCallback {
        void onFlyingTick(PlayerEntity player, long flyingTick, boolean flying);
    }

    interface PlayerSneakingCallback {
        void onSneaking(PlayerEntity player, boolean sneak);
    }

    interface PlayerIdlingCallback {
        void onSwitchIdle(ServerPlayerEntity player, boolean afk);
    }

    interface ServerMotdCallback {
        ServerMetadata onMotd(MinecraftServer server, ClientConnection connection);
    }

    interface PlayerDeathCallback {
        void onPlayerDeath(PlayerEntity player, DamageSource damageSource);
    }

    interface PlayerPreLoginCallback {
        void onPlayerPreLogin(String currentPlayer, ClientConnection connection, Consumer<Text> disconnect, boolean offline);
    }

    interface PlayerSleepCallback {

        Either<PlayerEntity.SleepFailureReason, Unit> onPlayerSleep(PlayerEntity player, BlockPos pos);
    }
}
