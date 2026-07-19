package sfcraft.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
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

    public ReversePearlTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.FISHING_ROD;
    }

    public static ItemStack createBound(UUID ownerId, String ownerName) {
        // 满耐久出品(damage 默认为 0),使用次数完全由耐久承载
        var stack = new ItemStack(sfcraft.SFItems.REVERSE_PEARL_TOKEN);
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
            serverPlayer.sendOverlayMessage(Component.literal("这个信物没有绑定主人"));
            return InteractionResult.FAIL;
        }

        var config = sfcraft.GameConfig.get().reverseToken;
        // 自实现冷却:比对上次使用时间戳,未到冷却时间则拒绝
        var data = stack.get(DataComponents.CUSTOM_DATA);
        var tag = data == null ? null : data.copyTag();
        long now = System.currentTimeMillis();
        long lastUse = tag == null ? 0 : tag.getLongOr(LAST_USE_KEY, 0L);
        long remainingMs = config.cooldownSeconds * 1000L - (now - lastUse);
        if (remainingMs > 0) {
            serverPlayer.sendOverlayMessage(Component.literal("信物冷却中,还需 " + (remainingMs / 1000 + 1) + " 秒"));
            return InteractionResult.FAIL;
        }

        var server = ((ServerLevel) level).getServer();
        var owner = server.getPlayerList().getPlayer(ownerId);
        var ownerName = PearlTokenItem.getOwnerName(stack);
        if (owner == null) {
            serverPlayer.sendOverlayMessage(Component.literal("信物的主人 " + ownerName + " 不在线"));
            return InteractionResult.FAIL;
        }

        var targetLevel = (ServerLevel) owner.level();
        serverPlayer.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(),
                Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot(), false);
        targetLevel.sendParticles(ParticleTypes.PORTAL,
                owner.getX(), owner.getY() + 1.0, owner.getZ(), 32, 0.5, 1.0, 0.5, 0.2);
        targetLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.sendSystemMessage(Component.literal(
                serverPlayer.getGameProfile().name() + " 使用反向珍珠信物来到了你身边"));

        // 记录本次使用时间戳(冷却),再扣除耐久(使用次数)
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(LAST_USE_KEY, now));
        stack.hurtAndBreak(1, serverPlayer, hand);
        if (stack.isEmpty()) {
            serverPlayer.sendOverlayMessage(Component.literal("信物已耗尽,化为了灰烬"));
        } else {
            updateLore(stack, ownerName);
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            serverPlayer.sendOverlayMessage(Component.literal("剩余使用次数: " + remaining + "/" + stack.getMaxDamage()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void updateLore(ItemStack stack, String ownerName) {
        int max = stack.getMaxDamage();
        int remaining = max - stack.getDamageValue();
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("主人: " + ownerName),
                Component.literal("剩余次数: " + remaining + "/" + max))));
    }
}
