package io.ib67.sfcraft.callback;

import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

import static io.ib67.sfcraft.callback.Utility.*;

public interface SFCallbacks {

    Event<PlayerPreLoginCallback> PRE_LOGIN = EventFactory.createArrayBacked(PlayerPreLoginCallback.class,
            (listeners) -> (s, c, d) -> anyMatch(listeners, i -> i.onPlayerPreLogin(s, c, d)));
    Event<PlayerSleepCallback> PLAYER_SLEEP = EventFactory.createArrayBacked(PlayerSleepCallback.class,
            l -> (pl, pos) -> anyMatch(l, i -> i.onPlayerSleep(pl, pos), a -> a.left().isPresent())
                    .orElse(getEitherUnitR()));

    Event<PlayerDeathCallback> PLAYER_DEATH = EventFactory.createArrayBacked(PlayerDeathCallback.class,
            l -> (pl, damageSource) -> forEach(l, i -> i.onPlayerDeath(pl, damageSource)));
    Event<ServerMotdCallback> MOTD = EventFactory.createArrayBacked(ServerMotdCallback.class,
            l -> (s, c) -> first(l, t -> t.onMotd(s, c)));

    interface ServerMotdCallback {
        ServerMetadata onMotd(MinecraftServer server, ClientConnection connection);
    }

    interface PlayerDeathCallback {
        void onPlayerDeath(PlayerEntity player, DamageSource damageSource);
    }

    interface PlayerPreLoginCallback {
        /**
         * @return if let in
         */
        boolean onPlayerPreLogin(String currentPlayer, ClientConnection connection, Consumer<Text> disconnect);
    }

    interface PlayerSleepCallback {

        Either<PlayerEntity.SleepFailureReason, Unit> onPlayerSleep(PlayerEntity player, BlockPos pos);
    }
}
