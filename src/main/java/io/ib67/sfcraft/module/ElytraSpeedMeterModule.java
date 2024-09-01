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
    private static final Text[] ELYTRA_DURABILITY = new Text[100];
    private final Object2ObjectMap<PlayerEntity, BlockPos> playerFlyMap = new Object2ObjectOpenHashMap<>();
    private final Object2FloatMap<PlayerEntity> lastMeasuredSpeed = new Object2FloatOpenHashMap<>();

    static {
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
            return;
        }
        if (b) {
            var text = Text.literal("");
            text.append(genPitchMeter(player.getPitch()));
            text.append(" ");
            text.append(generateDurabilityMeter(f, player));
            player.sendMessage(text, true);
        } else {
            clean((ServerPlayerEntity) player);
            if (f > THRESHLD_OF_ELYTRA_FLY * 20) {
                player.sendMessage(Text.literal("!! LANDED !!").withColor(Colors.GREEN), true);
            } else {
                player.sendMessage(Text.empty(), true);

            }
        }
    }

    private Text genPitchMeter(float pitch) {
        MutableText text;
        var display = (Math.ceil(Math.abs(pitch) * 100) / 100);
        if (pitch < 0) { // 朝上
            text = Text.literal(" 🡹 " + display + "°");
        } else {
            text = Text.literal(" 🡻 " + display + "°");
        }
        if (pitch > 75) {
            text.withColor(Colors.LIGHT_RED);
        }
        return text;
    }

    private Text generateDurabilityMeter(long f, PlayerEntity player) {
        var stack = player.getInventory().getArmorStack(PlayerInventory.ARMOR_SLOTS[2]);
        if (stack.isDamageable() && stack.isDamaged()) {
            var remaining = stack.getMaxDamage() - stack.getDamage();
            var percent = (int) Math.ceil(((double) remaining / stack.getMaxDamage()) * 100);
            return ELYTRA_DURABILITY[Math.max(percent - 1, 0)];
        }
        return Text.literal("ELYTRA: INFINITY").withColor(Colors.BLUE);
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
}
