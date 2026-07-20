package io.ib67.sfcraft.module.game.token;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.ib67.sfcraft.module.game.item.ItemLores;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PearlTokenItem extends Item implements PolymerItem {
    public static final String OWNER_KEY = "sfcraft_owner";
    public static final String OWNER_NAME_KEY = "sfcraft_owner_name";

    public PearlTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.WARPED_FUNGUS_ON_A_STICK;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS_SERVER;

        var ownerId = getOwnerId(stack);
        if (ownerId == null) {
            bindOne(serverPlayer, hand, stack);
            return InteractionResult.SUCCESS_SERVER;
        }

        var server = ((ServerLevel) level).getServer();
        var owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.owner_offline", getOwnerName(stack)));
            return InteractionResult.FAIL;
        }

        var targetLevel = (ServerLevel) serverPlayer.level();
        owner.teleportTo(targetLevel, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                Set.of(), owner.getYRot(), owner.getXRot(), false);
        targetLevel.sendParticles(ParticleTypes.PORTAL,
                serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 32, 0.5, 1.0, 0.5, 0.2);
        targetLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.sendSystemMessage(Component.translatable("message.sfcraft.token.summoned_owner",
                serverPlayer.getGameProfile().name()));
        serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.token.summon_success", getOwnerName(stack)));
        stack.consume(1, serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void bindOne(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        var bound = stack.copyWithCount(1);
        CustomData.update(DataComponents.CUSTOM_DATA, bound, tag -> {
            tag.putString(OWNER_KEY, player.getUUID().toString());
            tag.putString(OWNER_NAME_KEY, player.getGameProfile().name());
        });
        bound.set(DataComponents.LORE, new ItemLore(List.of(
                ItemLores.line("lore.sfcraft.token.owner", player.getGameProfile().name()))));
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, bound);
        } else {
            stack.shrink(1);
            if (!player.getInventory().add(bound)) {
                player.drop(bound, false);
            }
        }
        player.sendOverlayMessage(Component.translatable("message.sfcraft.token.bound"));
    }

    @Nullable
    public static UUID getOwnerId(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var raw = data.copyTag().getStringOr(OWNER_KEY, "");
        if (raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getOwnerName(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "?";
        return data.copyTag().getStringOr(OWNER_NAME_KEY, "?");
    }
}
