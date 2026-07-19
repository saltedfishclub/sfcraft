package sfcraft.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import sfcraft.GameConfig;
import sfcraft.SFEnchantments;

import java.util.Map;

/**
 * 斩首附魔:持有该附魔的武器击杀敌怪(含玩家)时有概率掉落对应头颅。
 * 掉头逻辑纯服务端,附魔本身仅作等级标记(数据驱动定义于 beheading.json)。
 */
public class BeheadingHandler {
    // 有原版对应头颅的实体 → 头颅物品;其余实体不掉头
    private static final Map<EntityType<?>, Item> HEADS = Map.of(
            EntityTypes.ZOMBIE, Items.ZOMBIE_HEAD,
            EntityTypes.SKELETON, Items.SKELETON_SKULL,
            EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL,
            EntityTypes.CREEPER, Items.CREEPER_HEAD,
            EntityTypes.PIGLIN, Items.PIGLIN_HEAD,
            EntityTypes.PIGLIN_BRUTE, Items.PIGLIN_HEAD,
            EntityTypes.ZOMBIFIED_PIGLIN, Items.PIGLIN_HEAD,
            EntityTypes.ENDER_DRAGON, Items.DRAGON_HEAD);

    private BeheadingHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(BeheadingHandler::onDeath);
    }

    private static void onDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;

        var killer = source.getEntity() instanceof LivingEntity le ? le : victim.getKillCredit();
        if (killer == null) return;

        var head = headFor(victim);
        if (head == null) return;

        var holder = SFEnchantments.getBeheading(serverLevel.registryAccess());
        if (holder.isEmpty()) return;
        int level = EnchantmentHelper.getItemEnchantmentLevel(holder.get(), killer.getMainHandItem());
        if (level <= 0) return;

        var config = GameConfig.get().beheading;
        float chance = victim instanceof Player
                ? config.playerHeadChance
                : config.baseChance + config.perLevelChance * level;
        if (serverLevel.getRandom().nextFloat() >= chance) return;

        victim.spawnAtLocation(serverLevel, head);
    }

    private static ItemStack headFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            var head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
            return head;
        }
        var item = HEADS.get(victim.getType());
        return item == null ? null : new ItemStack(item);
    }
}
