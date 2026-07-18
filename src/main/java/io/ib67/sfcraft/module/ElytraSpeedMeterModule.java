package io.ib67.sfcraft.module;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class ElytraSpeedMeterModule extends ServerModule {
    private static final int THRESHLD_OF_ELYTRA_FLY = 8;
    private static final Component[] ELYTRA_DURABILITY = new Component[100];
    private final Object2ObjectMap<Player, BlockPos> playerFlyMap = new Object2ObjectOpenHashMap<>();
    private final Object2FloatMap<Player> lastMeasuredSpeed = new Object2FloatOpenHashMap<>();

    static {
        for (int i = 1; i <= 100; i++) {
            ELYTRA_DURABILITY[i - 1] = Component.literal("ELYTRA: " + i + "%").withColor(getDamageColor(i));
        }
    }

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_FLYING.register(this::onFlying);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        clean(serverPlayNetworkHandler.getPlayer());
    }

    private void clean(ServerPlayer player) {
        playerFlyMap.remove(player);
        lastMeasuredSpeed.removeFloat(player);
    }

    private void onFlying(Player player, long f, boolean b) {
        if (f < THRESHLD_OF_ELYTRA_FLY * 20) {
            return;
        }
        if (b) {
            var text = Component.literal("");
            text.append(genPitchMeter(player.getXRot()));
            text.append(" ");
            text.append(generateDurabilityMeter(f, player));
            player.sendOverlayMessage(text);
        } else {
            clean((ServerPlayer) player);
            if (f > THRESHLD_OF_ELYTRA_FLY * 20) {
                player.sendOverlayMessage(Component.literal("!! LANDED !!").withColor(CommonColors.GREEN));
            } else {
                player.sendOverlayMessage(Component.empty());

            }
        }
    }

    private Component genPitchMeter(float pitch) {
        MutableComponent text;
        var display = (Math.ceil(Math.abs(pitch) * 100) / 100);
        if (pitch < 0) { // 朝上
            text = Component.literal(" 🡹 " + display + "°");
        } else {
            text = Component.literal(" 🡻 " + display + "°");
        }
        if (pitch > 75) {
            text.withColor(CommonColors.SOFT_RED);
        }
        return text;
    }

    private Component generateDurabilityMeter(long f, Player player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.isDamageableItem() && stack.isDamaged()) {
            var remaining = stack.getMaxDamage() - stack.getDamageValue();
            var percent = (int) Math.ceil(((double) remaining / stack.getMaxDamage()) * 100);
            return ELYTRA_DURABILITY[Math.max(percent - 1, 0)];
        }
        return Component.literal("ELYTRA: INFINITY").withColor(CommonColors.BLUE);
    }

    private static int getDamageColor(int percent) {
        if (percent > 70) {
            return CommonColors.GREEN;
        } else if (percent > 50) {
            return CommonColors.SOFT_YELLOW;
        } else if (percent > 30) {
            return CommonColors.SOFT_RED;
        }
        return CommonColors.RED;
    }
}
