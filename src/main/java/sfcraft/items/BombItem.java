package sfcraft.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import sfcraft.entity.BombEntity;

public class BombItem extends Item implements PolymerItem {
    private static final int THROW_COOLDOWN_TICKS = 10;

    private final BombEntity.BombType type;
    private final Item visualItem;

    public BombItem(Properties properties, BombEntity.BombType type, Item visualItem) {
        super(properties);
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
                var bomb = new BombEntity(shooter, spawnLevel, spawnStack);
                bomb.setBombType(type);
                return bomb;
            }, serverLevel, stack, player, 0.0F, 1.5F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack, THROW_COOLDOWN_TICKS);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
