package io.ib67.sfcraft.module.game.bomb;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.ib67.sfcraft.config.GameConfigService;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BombItem extends Item implements PolymerItem {
    private final GameConfigService config;
    private final EntityType<BombEntity> bombType;
    private final BombEntity.BombType type;
    private final Item visualItem;

    public BombItem(Properties properties, GameConfigService config, EntityType<BombEntity> bombType,
                    BombEntity.BombType type, Item visualItem) {
        super(properties);
        this.config = config;
        this.bombType = bombType;
        this.type = type;
        this.visualItem = visualItem;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return visualItem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.8F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation((spawnLevel, shooter, spawnStack) -> {
                var bomb = new BombEntity(bombType, shooter, spawnLevel, spawnStack, config, () -> this);
                bomb.setBombType(type);
                return bomb;
            }, serverLevel, stack, player, 0.0F, 1.5F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack, config.get().bomb.throwCooldownTicks);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
