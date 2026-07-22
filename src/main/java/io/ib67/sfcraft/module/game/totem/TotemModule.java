package io.ib67.sfcraft.module.game.totem;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFConfigReload;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.module.game.item.SimplePolymerItem;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.ItemGroupService;
import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import lombok.Getter;
import net.minecraft.advancements.triggers.CriteriaTriggers;
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
import net.minecraft.stats.Stats;
import net.minecraft.world.level.gameevent.GameEvent;

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
    @Inject
    private ItemGroupService itemGroups;

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
                        // 刻意不挂 DEATH_PROTECTION 组件:否则原版死亡保护会识别它、在本图腾失效(冷却/
                        // 经验不足)时把它当普通图腾消耗,或连带取消掉玩家另一只手里的原版图腾。
                        // 效果改由 mixin 完全接管,复用 DeathProtection.TOTEM_OF_UNDYING 常量(见 tryProtect)。
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.standing_firm_totem"))
        );
        itemGroups.add(standingFirmTotem);
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
                "standing_firm_totem",
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
        // 复用原版图腾的死亡效果(清负面 + 再生/伤害吸收/抗火);物品本身不挂组件,直接用常量应用
        DeathProtection.TOTEM_OF_UNDYING.applyEffects(totem, player);
        // 事件 35 = 图腾使用动画+粒子+音效,由客户端按手持物品外观播放
        player.level().broadcastEntityEvent(player, (byte) 35);
        // 复刻原版图腾的统计/成就副作用:物品使用统计、「向死而生」成就、交互结束游戏事件
        player.awardStat(Stats.ITEM_USED.get(standingFirmTotem));
        CriteriaTriggers.USED_TOTEM.trigger(player, totem);
        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
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
