package io.ib67.sfcraft.module.game.token;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.ib67.sfcraft.config.GameConfigService;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReversePearlTokenItem extends Item implements PolymerItem {
    // 冷却由本类自行实现:在 CUSTOM_DATA 内记录上次使用的时间戳
    public static final String LAST_USE_KEY = "sfcraft_last_use";

    private final GameConfigService config;

    public ReversePearlTokenItem(Properties properties, GameConfigService config) {
        super(properties);
        this.config = config;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.FISHING_ROD;
    }

    public ItemStack createBound(UUID ownerId, String ownerName) {
        // 满耐久出品(damage 默认为 0),使用次数完全由耐久承载
        var stack = new ItemStack(this);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(PearlTokenItem.OWNER_KEY, ownerId.toString());
            tag.putString(PearlTokenItem.OWNER_NAME_KEY, ownerName);
        });
        updateLore(stack, ownerName);
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS_SERVER;

        var ownerId = PearlTokenItem.getOwnerId(stack);
        if (ownerId == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.not_bound"));
            return InteractionResult.FAIL;
        }

        var tokenConfig = config.get().reverseToken;
        // 自实现冷却:比对上次使用时间戳,未到冷却时间则拒绝
        var data = stack.get(DataComponents.CUSTOM_DATA);
        var tag = data == null ? null : data.copyTag();
        long now = System.currentTimeMillis();
        long lastUse = tag == null ? 0 : tag.getLongOr(LAST_USE_KEY, 0L);
        long remainingMs = tokenConfig.cooldownSeconds * 1000L - (now - lastUse);
        if (remainingMs > 0) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.cooldown",
                    remainingMs / 1000 + 1));
            return InteractionResult.FAIL;
        }

        var server = ((ServerLevel) level).getServer();
        var owner = server.getPlayerList().getPlayer(ownerId);
        var ownerName = PearlTokenItem.getOwnerName(stack);
        if (owner == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.owner_offline", ownerName));
            return InteractionResult.FAIL;
        }

        var targetLevel = (ServerLevel) owner.level();
        serverPlayer.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(),
                Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot(), false);
        targetLevel.sendParticles(ParticleTypes.PORTAL,
                owner.getX(), owner.getY() + 1.0, owner.getZ(), 32, 0.5, 1.0, 0.5, 0.2);
        targetLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.sendSystemMessage(Component.translatable("message.sfcraft.token.arrived",
                serverPlayer.getGameProfile().name()));

        // 记录本次使用时间戳(冷却),再扣除耐久(使用次数)
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(LAST_USE_KEY, now));
        stack.hurtAndBreak(1, serverPlayer, hand);
        if (stack.isEmpty()) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.depleted"));
        } else {
            updateLore(stack, ownerName);
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.uses_left",
                    remaining, stack.getMaxDamage()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void updateLore(ItemStack stack, String ownerName) {
        int max = stack.getMaxDamage();
        int remaining = max - stack.getDamageValue();
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("lore.sfcraft.token.owner", ownerName),
                Component.translatable("lore.sfcraft.token.uses", remaining, max))));
    }
}
