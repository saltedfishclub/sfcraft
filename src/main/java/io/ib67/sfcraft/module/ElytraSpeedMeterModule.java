package io.ib67.sfcraft.module;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.util.ColorHelper;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ElytraItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Unique;

import java.awt.*;
import java.text.DecimalFormat;

public class ElytraSpeedMeterModule extends ServerModule {
    private static final int BAR_LEN = 20;
    private static final int MAX_NORMAL_SPEED = 6;
    private static final int THRESHLD_OF_ELYTRA_FLY = 8;
    private static final Color BAR_LEFT = new Color(85, 85, 255);
    private static final Color BAR_RIGHT = new Color(255, 87, 51);
    private static final Text KEEP_FLYING = Text.literal("... 保持飞行以启动仪表盘 ...").withColor(Colors.LIGHT_GRAY);
    private static final Text[] ELYTRA_DURABILITY = new Text[100];
    private static final Text[] PROGRESS_BARS = new Text[BAR_LEN];
    private static final Text[] Y_METER = new Text[256];
    private final Object2ObjectMap<PlayerEntity, BlockPos> playerFlyMap = new Object2ObjectOpenHashMap<>();
    private final Object2FloatMap<PlayerEntity> lastMeasuredSpeed = new Object2FloatOpenHashMap<>();

    static {
        for (int i = 1; i <= BAR_LEN; i++) {
            PROGRESS_BARS[i - 1] = buildProgressBar(i);
        }
        for (int i = 1; i <= 256; i++) {
            Y_METER[i - 1] = makeYMeter(i);
        }
        for (int i = 1; i <= 100; i++) {
            ELYTRA_DURABILITY[i - 1] = Text.literal("ELYTRA: " + i + "%").withColor(getDamageColor(i));
        }
    }

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_FLYING.register(this::onFlying);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        clean(serverPlayNetworkHandler.getPlayer());
    }

    private void clean(ServerPlayerEntity player) {
        playerFlyMap.remove(player);
        lastMeasuredSpeed.removeFloat(player);
    }

    private void onFlying(PlayerEntity player, long f, boolean b) {
        if (f < THRESHLD_OF_ELYTRA_FLY * 20) {
            if (f > THRESHLD_OF_ELYTRA_FLY * 15) {
                player.sendMessage(KEEP_FLYING, true);
            }
            return;
        }
        if (b) {
            if (!lastMeasuredSpeed.containsKey(player)) {
                ((ServerPlayerEntity) player).networkHandler.sendPacket(new TitleFadeS2CPacket(0, 20, 0));
            }
            var text = Text.literal("");
            text.append(generateYMeter(f, player));
            text.append(generateSpeedMeter(f, player));
            text.append(generateDurabilityMeter(f, player));
            player.sendMessage(text, true);
            ((ServerPlayerEntity) player).networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
            ((ServerPlayerEntity) player).networkHandler.sendPacket(buildPitchMeter(player));
        } else {
            clean((ServerPlayerEntity) player);
            ((ServerPlayerEntity) player).networkHandler.sendPacket(new TitleFadeS2CPacket(10, 20, 10));
            if (f > THRESHLD_OF_ELYTRA_FLY * 20) {
                player.sendMessage(Text.literal("!! LANDED !!").withColor(Colors.GREEN), true);
                ((ServerPlayerEntity) player).networkHandler.sendPacket(new SubtitleS2CPacket(Text.empty()));
            } else {
                player.sendMessage(Text.empty(), true);

            }
        }
    }

    private Packet<?> buildPitchMeter(PlayerEntity player) {
        var pitch = player.getPitch();
        MutableText text;
        var display = (Math.ceil(Math.abs(pitch) * 100) / 100);
        if (pitch < 0) { // 朝上
            text = Text.literal("🡹 " + display + "°");
        } else {
            text = Text.literal("🡻 " + display + "°");
        }
        if (pitch > 75) {
            text.withColor(Colors.LIGHT_RED);
        }
        return new SubtitleS2CPacket(text);
    }

    private Text generateDurabilityMeter(long f, PlayerEntity player) {
        var stack = player.getInventory().getArmorStack(PlayerInventory.ARMOR_SLOTS[2]);
        if (stack.isDamageable() && stack.isDamaged()) {
            var remaining = stack.getMaxDamage() - stack.getDamage();
            var percent = (int) Math.ceil(((double) remaining / stack.getMaxDamage()) * 100);
            return ELYTRA_DURABILITY[Math.max(percent - 1, 0)];
        }
        return Text.literal(" ELYTRA: INFINITY").withColor(Colors.BLUE);
    }

    private static int getDamageColor(int percent) {
        if (percent > 70) {
            return Colors.GREEN;
        } else if (percent > 50) {
            return Colors.LIGHT_YELLOW;
        } else if (percent > 30) {
            return Colors.LIGHT_RED;
        }
        return Colors.RED;
    }

    private Text generateYMeter(long f, PlayerEntity player) {
        var y = player.getBlockY();
        if (y <= 256 && y > 0) {
            return Y_METER[y - 1];
        } else {
            return makeYMeter(y);
        }
    }

    private static Text makeYMeter(int y) {
        return Text.literal("Y: " + y + " | ").withColor(getYColor(y));
    }

    private static int getYColor(int y) {
        if (y < 120) {
            return Colors.YELLOW;
        } else if (y <= 180) {
            return Colors.GREEN;
        } else {
            return Colors.BLUE;
        }
    }

    private Text generateSpeedMeter(long flyTime, PlayerEntity player) {
        final var MEASURE_TIME = 5;
        if (!playerFlyMap.containsKey(player)) {
            playerFlyMap.put(player, player.getBlockPos());
            return Text.empty();
        }
        float speed;
        if (flyTime % MEASURE_TIME == 0) {
            var pos = playerFlyMap.get(player);
            var distance = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            speed = (float) (Math.abs(Math.round(distance / Math.pow(MEASURE_TIME, 2) * 100) * 0.01));
            lastMeasuredSpeed.put(player, speed);
            playerFlyMap.put(player, player.getBlockPos());
        } else {
            speed = lastMeasuredSpeed.getFloat(player);
        }
        var percentToSix = speed / MAX_NORMAL_SPEED;
        var result = Text.literal("SPD: [");


        int steps = (int) Math.ceil(percentToSix * BAR_LEN);
        if (steps > BAR_LEN - 1) {
            steps = BAR_LEN - 1;
        }
        result.append(PROGRESS_BARS[steps]);
        result.append("] " + Math.ceil(speed * 100) / 100 + "/s | ");
        if (speed > MAX_NORMAL_SPEED) {
            result.withColor(Colors.LIGHT_RED);
        }
        return result;
    }

    private static Text buildProgressBar(int steps) {
        var result = Text.literal("");
        for (int i = 0; i < steps; i++) {
            var color = ColorHelper.interpolate(ColorHelper.GradientType.HSV, BAR_LEFT, BAR_RIGHT, (float) ((double) i / BAR_LEN));
            result.append(Text.literal("|").withColor(color.getRGB()));
        }
        result.append(Text.literal("|".repeat(BAR_LEN - steps)).withColor(Colors.LIGHT_GRAY));
        return result;
    }
}
