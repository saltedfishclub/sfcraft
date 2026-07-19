package sfcraft.effect;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import sfcraft.GameConfig;
import sfcraft.SFMobEffects;

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
 * 「统帅」效果的行为驱动:
 * <ul>
 *   <li>携带者发紫光(GLOWING + 紫色队伍颜色)</li>
 *   <li>为半径内至多 N 个「同攻击目标、且不带统帅效果」的怪物提供 2 种 buff</li>
 *   <li>提供者↔受益者之间有紫色粒子纽带</li>
 *   <li>受益者走太远则被寻路拉回</li>
 *   <li>提供者切换仇恨目标 → 已连接的受益者同步覆写为新目标(整队跟随)</li>
 *   <li>提供者死亡/失去效果 → 立即撤销其 buff、去光、退队</li>
 * </ul>
 */
public class CommanderAuraManager {
    private static final String TEAM_NAME = "sfcraft_commander";
    private static final int PURPLE = 0xAA00FF;
    private static final List<Holder<MobEffect>> BUFF_POOL =
            List.of(MobEffects.STRENGTH, MobEffects.RESISTANCE, MobEffects.SPEED);

    // 提供者 UUID -> 当前纽带
    private static final Map<UUID, Bond> ACTIVE = new HashMap<>();
    private static int tickCounter;

    // lastTargetId:上一轮扫描时提供者的仇恨目标 UUID(null 表示无目标),用于检测目标切换
    private record Bond(ResourceKey<Level> dimension, UUID lastTargetId,
                        Set<UUID> recipients, List<Holder<MobEffect>> buffs) {
    }

    private CommanderAuraManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CommanderAuraManager::onTick);
        // 提供者死亡即时清理,保证「死亡后 buff 立即消失」
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (ACTIVE.containsKey(entity.getUUID()) && entity.level() instanceof ServerLevel level) {
                clearBond(entity.getUUID(), level.getServer());
            }
        });
    }

    private static void onTick(MinecraftServer server) {
        var config = GameConfig.get().commander;
        tickCounter++;
        if (tickCounter % Math.max(1, config.scanIntervalTicks) == 0) {
            rescan(server, config);
        }
        if (tickCounter % Math.max(1, config.particleIntervalTicks) == 0) {
            drawTethers(server);
        }
    }

    private static void rescan(MinecraftServer server, GameConfig.Commander config) {
        Holder<MobEffect> commander = SFMobEffects.COMMANDER;
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = getOrCreateTeam(scoreboard);
        Set<UUID> seen = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) { //todo rewrite by level#getNearbyEntities
            List<? extends Mob> providers = level.getEntities(EntityTypeTest.<Entity, Mob>forClass(Mob.class),
                    m -> m.isAlive() && m.hasEffect(commander));
            for (Mob provider : providers) {
                seen.add(provider.getUUID());
                // 紫光:GLOWING(无粒子)+ 紫色队伍
                provider.addEffect(new MobEffectInstance(MobEffects.GLOWING, config.glowRefreshTicks, 0, true, false, false));
                if (!team.equals(scoreboard.getPlayersTeam(provider.getScoreboardName()))) {
                    scoreboard.addPlayerToTeam(provider.getScoreboardName(), team);
                }

                Bond previous = ACTIVE.get(provider.getUUID());
                LivingEntity target = provider.getTarget();
                UUID targetId = target == null ? null : target.getUUID();
                // 提供者切换仇恨目标 → 立即把新目标覆写给上一轮已连接的成员,让整队跟随
                if (previous != null && target != null && !Objects.equals(previous.lastTargetId(), targetId)) {
                    overrideRecipientTargets(level, previous.recipients(), target, commander);
                }

                List<Holder<MobEffect>> buffs = pickBuffs(provider.getUUID());
                Set<UUID> priorRecipients = previous == null ? Set.of() : previous.recipients();
                Set<UUID> recipients = collectAndBuff(level, provider, buffs, config, commander, priorRecipients);
                ACTIVE.put(provider.getUUID(), new Bond(level.dimension(), targetId, recipients, buffs));
            }
        }

        // 已不再是提供者(失去效果/离开)→ 清理
        List<UUID> stale = new ArrayList<>();
        for (UUID id : ACTIVE.keySet()) {
            if (!seen.contains(id)) stale.add(id);
        }
        for (UUID id : stale) {
            clearBond(id, server);
        }
    }

    private static Set<UUID> collectAndBuff(ServerLevel level, Mob provider, List<Holder<MobEffect>> buffs,
                                            GameConfig.Commander config, Holder<MobEffect> commander,
                                            Set<UUID> priorRecipients) {
        Set<UUID> recipients = new HashSet<>();
        LivingEntity target = provider.getTarget();
        if (target == null) return recipients; // 无共同目标则只发光,不供 buff

        double r2 = config.radius * config.radius;
        double cohesion2 = config.cohesionMaxDistance * config.cohesionMaxDistance;
        var box = provider.getBoundingBox().inflate(config.radius);
        var candidates = level.getEntitiesOfClass(Mob.class, box, m ->
                m != provider && m.isAlive() && !m.hasEffect(commander) && m.getTarget() == target);
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
    private static void overrideRecipientTargets(ServerLevel level, Set<UUID> recipients,
                                                 LivingEntity target, Holder<MobEffect> commander) {
        for (UUID rid : recipients) {
            if (level.getEntityInAnyDimension(rid) instanceof Mob mob
                    && mob != target && mob.isAlive() && !mob.hasEffect(commander)) {
                mob.setTarget(target);
            }
        }
    }

    private static void clearBond(UUID providerId, MinecraftServer server) {
        Bond bond = ACTIVE.remove(providerId);
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

    private static void drawTethers(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        var dust = new DustParticleOptions(PURPLE, 1.0F);
        for (var entry : ACTIVE.entrySet()) {
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
