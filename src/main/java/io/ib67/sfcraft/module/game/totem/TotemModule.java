package io.ib67.sfcraft.module.game.totem;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.module.game.item.SimplePolymerItem;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 「屹立不倒」——可反复使用的不死图腾变种。物品自带原版死亡保护组件
 * ({@link DeathProtection#TOTEM_OF_UNDYING}),因此复用原版的清负面/回血/图腾动画/成就统计;
 * 差异行为(可反复用、30 秒冷却、触发扣一半经验、经验不足则放弃)由 {@link #tryProtect}
 * 在 {@code TotemProtectionMixin} 里接管原版判定后实现。
 */
@Getter
public class TotemModule extends ServerModule {
    @Inject
    private GameConfigService config;
    @Inject
    private CauldronRecipeRegistry cauldronRecipes;

    private Item standingFirmTotem;

    private final List<CauldronRecipe> registeredRecipes = new ArrayList<>();

    /** 死亡保护判定结果,回传给 mixin 决定原版方法的返回值。 */
    public enum Result {
        /** 玩家没拿本图腾,交回原版逻辑。 */
        NOT_APPLICABLE,
        /** 已触发保护,玩家存活。 */
        PROTECTED,
        /** 拿了本图腾但条件不满足(冷却/经验不足),放弃治疗、玩家死亡且不消耗物品。 */
        DECLINED
    }

    @Override
    public void onInitialize() {
        standingFirmTotem = RegistryHelper.registerItem(
                "standing_firm_totem",
                properties -> new SimplePolymerItem(properties, Items.TOTEM_OF_UNDYING),
                new Item.Properties()
                        .stacksTo(1)
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/standing_firm_totem"))
                        // 自带原版图腾同款死亡保护组件:原版死亡保护识别的是该组件而非具体物品类型
                        .component(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.standing_firm_totem"))
        );
        registerRecipes();
        // /sfcraft reload 后重建配方,应用最新的 reactionTicks
        SFConfigReload.EVENT.register(() -> {
            registeredRecipes.forEach(cauldronRecipes::unregister);
            registeredRecipes.clear();
            registerRecipes();
        });
    }

    private void registerRecipes() {
        // 紫水晶炼药锅里丢入: 不死图腾 + 附魔之瓶 + 绿宝石(催化剂),加热 → 屹立不倒
        register(new CauldronRecipe(
                List.of(
                        stack -> stack.is(Items.TOTEM_OF_UNDYING),
                        stack -> stack.is(Items.EXPERIENCE_BOTTLE),
                        stack -> stack.is(Items.EMERALD)),
                false,
                true,
                ParticleTypes.TOTEM_OF_UNDYING,
                config.get().cauldron.reactionTicks,
                contents -> new ItemStack(standingFirmTotem)));
    }

    private void register(CauldronRecipe recipe) {
        registeredRecipes.add(recipe);
        cauldronRecipes.register(recipe);
    }

    /**
     * mixin 在 {@code checkTotemDeathProtection} 头部调用。玩家持有本图腾时完全接管本次判定,
     * 阻止原版把它当普通图腾消耗。
     */
    public Result tryProtect(ServerPlayer player, DamageSource source) {
        ItemStack totem = findTotem(player);
        if (totem == null) {
            return Result.NOT_APPLICABLE;
        }
        // 穿透无敌的伤害(虚空、/kill 等)不救:交回原版,原版对此既不保护也不消耗
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return Result.NOT_APPLICABLE;
        }
        var settings = config.get().standingFirmTotem;
        if (player.getCooldowns().isOnCooldown(totem)) {
            return Result.DECLINED;
        }
        int totalXp = ExperienceHelper.total(player);
        if (totalXp < settings.minExperiencePoints) {
            return Result.DECLINED;
        }

        // 触发:扣经验 → 冷却 → 复用原版图腾效果(回血在下方,清负面/加 buff 由组件负责)
        player.giveExperiencePoints(-(int) (totalXp * settings.experienceDrainRatio));
        player.getCooldowns().addCooldown(totem, settings.cooldownTicks);
        player.setHealth(1.0F);
        var protection = totem.get(DataComponents.DEATH_PROTECTION);
        if (protection != null) {
            protection.applyEffects(totem, player);
        }
        // 事件 35 = 图腾使用动画+粒子+音效,由客户端按手持物品外观播放
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.sendSystemMessage(Component.translatable("message.sfcraft.totem.triggered"));
        return Result.PROTECTED;
    }

    private ItemStack findTotem(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(standingFirmTotem)) {
                return held;
            }
        }
        return null;
    }
}
