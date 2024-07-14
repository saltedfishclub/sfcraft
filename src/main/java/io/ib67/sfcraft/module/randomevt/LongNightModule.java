package io.ib67.sfcraft.module.randomevt;

import com.google.inject.Inject;
import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.module.randomevt.longnight.DawnAfterLongNightEvent;
import io.ib67.sfcraft.module.randomevt.longnight.LongNightEvent;
import io.ib67.sfcraft.registry.event.RandomEventRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LongNightModule extends ServerModule {
    @Inject
    RandomEventRegistry registry;

    @Override
    public void onInitialize() {
        registry.registerEvent(LongNightEvent::new, World.OVERWORLD
                , world -> !LongNightEvent.isRunning()
                        && isEnabled()
                        && world.getTimeOfDay() % 24000 == 18000
                        && world.getRandom().nextBetween(0, 100) < 5); // At midnight
        registry.registerEvent(DawnAfterLongNightEvent::new, World.OVERWORLD
                , world -> world.getTimeOfDay() % 22200 == 0); // At dawn
        SFCallbacks.PLAYER_SLEEP.register(this::onPlayerSleep);
        UseItemCallback.EVENT.register(this::onUseClock);
    }

    private TypedActionResult<ItemStack> onUseClock(PlayerEntity player, World world, Hand hand) {
        if (player.isSpectator()) return TypedActionResult.pass(ItemStack.EMPTY);
        if (player.getStackInHand(hand).isOf(Items.CLOCK)) {
            if (LongNightEvent.isRunning())
                player.sendMessage(Text.literal("永夜还将持续约 " + LongNightEvent.getRemainingTicks() / 20 + " 秒。").withColor(Colors.LIGHT_GRAY));
        }
        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    public Either<PlayerEntity.SleepFailureReason, Unit> onPlayerSleep(PlayerEntity player, BlockPos pos) {
        if (!isEnabled()) return Either.right(Unit.INSTANCE);
        if (LongNightEvent.isRunning()) {
            player.sendMessage(Text.literal("你感到焦躁不安！").withColor(Colors.LIGHT_RED)
                    .append(Text.literal(" (永夜事件期间无法睡觉）").withColor(Colors.LIGHT_GRAY)));
            return Either.left(PlayerEntity.SleepFailureReason.NOT_POSSIBLE_HERE);
        }
        return Either.right(Unit.INSTANCE);
    }
}
