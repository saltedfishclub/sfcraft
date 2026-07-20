package io.ib67.sfcraft.module.game.beheading;

import com.google.inject.Inject;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;
import java.util.Optional;

/**
 * 斩首附魔:持有该附魔的武器击杀敌怪(含玩家)时有概率掉落对应头颅。
 * 掉头逻辑纯服务端,附魔本身为数据驱动内容(data/sfcraft/enchantment/beheading.json),
 * 无需代码注册,仅作等级标记。
 */
public class BeheadingModule extends ServerModule {
    public static final ResourceKey<Enchantment> BEHEADING = ResourceKey.create(
            Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "beheading"));

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

    @Inject
    private GameConfigService config;

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);
    }

    private void onDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;

        var killer = source.getEntity() instanceof LivingEntity le ? le : victim.getKillCredit();
        if (killer == null) return;

        var head = headFor(victim);
        if (head == null) return;

        var holder = getBeheading(serverLevel.registryAccess());
        if (holder.isEmpty()) return;
        int level = EnchantmentHelper.getItemEnchantmentLevel(holder.get(), killer.getMainHandItem());
        if (level <= 0) return;

        var beheading = config.get().beheading;
        float chance = victim instanceof Player
                ? beheading.playerHeadChance
                : beheading.baseChance + beheading.perLevelChance * level;
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

    public static Optional<Holder.Reference<Enchantment>> getBeheading(RegistryAccess registryAccess) {
        return registryAccess.lookup(Registries.ENCHANTMENT).flatMap(registry -> registry.get(BEHEADING));
    }
}
