package io.ib67.sfcraft.module;

import com.google.common.collect.Lists;
import io.ib67.kiwi.RandomHelper;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import jakarta.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

public class SoundModule extends ServerModule {
    public static final List<SoundEvent> DEATH_SOUNDS = Lists.newArrayList(
            SoundEvents.BEE_DEATH,
            SoundEvents.PLAYER_DEATH
    );

    protected SoundEvent join = SoundEvents.UI_TOAST_IN;
    protected SoundEvent leave = SoundEvents.UI_TOAST_OUT;

    protected final MinecraftServerSupplier serverSupplier;

    @Inject
    public SoundModule(MinecraftServerSupplier serverSupplier) {
        this.serverSupplier = serverSupplier;
    }

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_DEATH.register(this::playDeathSound);
        ServerPlayConnectionEvents.JOIN.register(this::playJoinSound);
        ServerPlayConnectionEvents.DISCONNECT.register(this::playLeaveSound);
    }

    @Override
    public void onEnable() {
        var sounds = SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariants.SoundSet.CLASSIC).adultSounds();
        DEATH_SOUNDS.add(sounds.hurtSound().value());
        join = SoundEvents.NOTE_BLOCK_HAT.value();
        join = SoundEvents.NOTE_BLOCK_BASS.value();
    }

    private void playLeaveSound(ServerGamePacketListenerImpl serverGamePacketListener, MinecraftServer minecraftServer) {
        PlayerLookup.all(serverSupplier.get()).forEach(it -> it.playSound(leave));
    }

    private void playJoinSound(ServerGamePacketListenerImpl serverGamePacketListener, PacketSender packetSender, MinecraftServer minecraftServer) {
        PlayerLookup.all(serverSupplier.get()).forEach(it -> it.playSound(join));
    }

    private void playDeathSound(Player player, DamageSource damageSource) {
        var rand = RandomHelper.number(0, DEATH_SOUNDS.size() - 1);
        PlayerLookup.all(serverSupplier.get()).forEach(it -> it.playSound(DEATH_SOUNDS.get(rand)));
    }
}
