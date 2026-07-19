package io.ib67.sfcraft.module.game.commander;

import com.google.inject.Inject;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfig;
import io.ib67.sfcraft.config.GameConfigService;
import lombok.Getter;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 「统帅」效果的注册与行为驱动:
 * <ul>
 *   <li>新生成的敌怪有 config 概率携带该效果(入口在 MobCommanderMixin → {@link #onMobSpawned})</li>
 *   <li>携带者发紫光(GLOWING + 紫色队伍颜色)</li>
 *   <li>为半径内至多 N 个「同攻击目标、且不带统帅效果」的怪物提供 2 种 buff</li>
 *   <li>提供者↔受益者之间有紫色粒子纽带</li>
 *   <li>受益者走太远则被寻路拉回</li>
 *   <li>提供者切换仇恨目标 → 已连接的受益者同步覆写为新目标(整队跟随)</li>
 *   <li>提供者死亡/失去效果 → 立即撤销其 buff、去光、退队</li>
 * </ul>
 */
public class CommanderModule extends ServerModule {
    private static final String TEAM_NAME = "sfcraft_commander";
    private static final int PURPLE = 0xAA00FF;
    private static final List<Holder<MobEffect>> BUFF_POOL =
            List.of(MobEffects.STRENGTH, MobEffects.RESISTANCE, MobEffects.SPEED);

    @Inject
    private GameConfigService config;

    /** 「统帅」效果本体,在 ModInit 注册(此时静态注册表尚未冻结)。 */
    @Getter
    private Holder<MobEffect> commanderEffect;

    // 提供者 UUID -> 当前纽带
    private final Map<UUID, Bond> active = new HashMap<>();
    private int tickCounter;

    // lastTargetId:上一轮扫描时提供者的仇恨目标 UUID(null 表示无目标),用于检测目标切换
    private record Bond(ResourceKey<Level> dimension, UUID lastTargetId,
                        Set<UUID> recipients, List<Holder<MobEffect>> buffs) {
    }

    @Override
    public void onInitialize() {
        commanderEffect = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "commander"),
                new CommanderMobEffect());
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        // 提供者死亡即时清理,保证「死亡后 buff 立即消失」
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (active.containsKey(entity.getUUID()) && entity.level() instanceof ServerLevel level) {
                clearBond(entity.getUUID(), level.getServer());
            }
        });
    }

    /** MobCommanderMixin 在 finalizeSpawn 尾部调用:自然刷新的敌怪按概率获得统帅效果。 */
    public void onMobSpawned(Mob mob, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL || !(mob instanceof Enemy)) return;
        double chance = config.get().commander.spawnChance;
        if (chance <= 0 || mob.getRandom().nextDouble() >= chance) return;
        mob.addEffect(new MobEffectInstance(commanderEffect,
                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
    }

    private void onTick(MinecraftServer server) {
        var commander = config.get().commander;
        tickCounter++;
        if (tickCounter % Math.max(1, commander.scanIntervalTicks) == 0) {
            rescan(server, commander);
        }
        if (tickCounter % Math.max(1, commander.particleIntervalTicks) == 0) {
            drawTethers(server);
        }
    }

    private void rescan(MinecraftServer server, GameConfig.Commander config) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = getOrCreateTeam(scoreboard);
        Set<UUID> seen = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) { //todo rewrite by level#getNearbyEntities
            List<? extends Mob> providers = level.getEntities(EntityTypeTest.<Entity, Mob>forClass(Mob.class),
                    m -> m.isAlive() && m.hasEffect(commanderEffect));
            for (Mob provider : providers) {
                seen.add(provider.getUUID());
                // 紫光:GLOWING(无粒子)+ 紫色队伍
                provider.addEffect(new MobEffectInstance(MobEffects.GLOWING, config.glowRefreshTicks, 0, true, false, false));
                if (!team.equals(scoreboard.getPlayersTeam(provider.getScoreboardName()))) {
                    scoreboard.addPlayerToTeam(provider.getScoreboardName(), team);
                }

                Bond previous = active.get(provider.getUUID());
                LivingEntity target = provider.getTarget();
                UUID targetId = target == null ? null : target.getUUID();
                // 提供者切换仇恨目标 → 立即把新目标覆写给上一轮已连接的成员,让整队跟随
                if (previous != null && target != null && !Objects.equals(previous.lastTargetId(), targetId)) {
                    overrideRecipientTargets(level, previous.recipients(), target);
                }

                List<Holder<MobEffect>> buffs = pickBuffs(provider.getUUID());
                Set<UUID> priorRecipients = previous == null ? Set.of() : previous.recipients();
                Set<UUID> recipients = collectAndBuff(level, provider, buffs, config, priorRecipients);
                active.put(provider.getUUID(), new Bond(level.dimension(), targetId, recipients, buffs));
            }
        }

        // 已不再是提供者(失去效果/离开)→ 清理
        List<UUID> stale = new ArrayList<>();
        for (UUID id : active.keySet()) {
            if (!seen.contains(id)) stale.add(id);
        }
        for (UUID id : stale) {
            clearBond(id, server);
        }
    }

    private Set<UUID> collectAndBuff(ServerLevel level, Mob provider, List<Holder<MobEffect>> buffs,
                                     GameConfig.Commander config, Set<UUID> priorRecipients) {
        Set<UUID> recipients = new HashSet<>();
        LivingEntity target = provider.getTarget();
        if (target == null) return recipients; // 无共同目标则只发光,不供 buff

        double r2 = config.radius * config.radius;
        double cohesion2 = config.cohesionMaxDistance * config.cohesionMaxDistance;
        var box = provider.getBoundingBox().inflate(config.radius);
        var candidates = level.getEntitiesOfClass(Mob.class, box, m ->
                m != provider && m.isAlive() && !m.hasEffect(commanderEffect) && m.getTarget() == target);
        // 超过 maxRecipients 时,让上一轮已连接的成员优先留队,避免被新候选挤出队伍
        candidates.sort(Comparator.comparingInt(m -> priorRecipients.contains(m.getUUID()) ? 0 : 1));
        for (Mob candidate : candidates) {
            if (recipients.size() >= config.maxRecipients) break;
            double d2 = candidate.distanceToSqr(provider);
            if (d2 > r2) continue;
            for (Holder<MobEffect> buff : buffs) {
                candidate.addEffect(new MobEffectInstance(buff, config.buffDurationTicks, 0, false, true, true));
            }
            if (d2 > cohesion2) {
                candidate.getNavigation().moveTo(provider, config.cohesionSpeed);
            }
            recipients.add(candidate.getUUID());
        }
        return recipients;
    }

    // 把提供者的新仇恨目标覆写给这些已连接的成员,使其停止各自为战、转而跟随统帅
    private void overrideRecipientTargets(ServerLevel level, Set<UUID> recipients, LivingEntity target) {
        for (UUID rid : recipients) {
            if (level.getEntityInAnyDimension(rid) instanceof Mob mob
                    && mob != target && mob.isAlive() && !mob.hasEffect(commanderEffect)) {
                mob.setTarget(target);
            }
        }
    }

    private void clearBond(UUID providerId, MinecraftServer server) {
        Bond bond = active.remove(providerId);
        if (bond == null) return;
        ServerLevel level = server.getLevel(bond.dimension());
        if (level != null) {
            for (UUID rid : bond.recipients()) {
                if (level.getEntityInAnyDimension(rid) instanceof LivingEntity recipient) {
                    for (Holder<MobEffect> buff : bond.buffs()) {
                        recipient.removeEffect(buff);
                    }
                }
            }
            if (level.getEntityInAnyDimension(providerId) instanceof LivingEntity provider) {
                provider.removeEffect(MobEffects.GLOWING);
            }
        }
        // 非玩家实体的记分板名即 UUID 串,无需实体在场即可退队
        Scoreboard scoreboard = server.getScoreboard();
        String name = providerId.toString();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team != null && team.equals(scoreboard.getPlayersTeam(name))) {
            scoreboard.removePlayerFromTeam(name, team);
        }
    }

    private void drawTethers(MinecraftServer server) {
        if (active.isEmpty()) return;
        var dust = new DustParticleOptions(PURPLE, 1.0F);
        for (var entry : active.entrySet()) {
            Bond bond = entry.getValue();
            ServerLevel level = server.getLevel(bond.dimension());
            if (level == null) continue;
            if (!(level.getEntityInAnyDimension(entry.getKey()) instanceof LivingEntity provider)) continue;
            Vec3 from = provider.position().add(0, provider.getBbHeight() * 0.5, 0);
            for (UUID rid : bond.recipients()) {
                if (level.getEntityInAnyDimension(rid) instanceof LivingEntity recipient) {
                    Vec3 to = recipient.position().add(0, recipient.getBbHeight() * 0.5, 0);
                    drawLine(level, dust, from, to);
                }
            }
        }
    }

    private static void drawLine(ServerLevel level, DustParticleOptions dust, Vec3 a, Vec3 b) {
        Vec3 delta = b.subtract(a);
        int points = Math.max(2, (int) (delta.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3 p = a.add(delta.scale((double) i / points));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static List<Holder<MobEffect>> pickBuffs(UUID id) {
        // 按提供者 UUID 稳定地从 3 种里去掉 1 种,保留 2 种
        List<Holder<MobEffect>> chosen = new ArrayList<>(BUFF_POOL);
        chosen.remove(Math.floorMod(id.hashCode(), BUFF_POOL.size()));
        return chosen;
    }

    private static PlayerTeam getOrCreateTeam(Scoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setColor(Optional.of(TeamColor.LIGHT_PURPLE));
        }
        return team;
    }
}
