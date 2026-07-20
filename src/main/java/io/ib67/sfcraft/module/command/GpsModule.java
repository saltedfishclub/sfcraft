package io.ib67.sfcraft.module.command;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.SimpleEntityElement;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfig;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.util.Helper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /gps &lt;x&gt; &lt;y&gt; &lt;z&gt;:在玩家视线前方悬浮一只发光小史莱姆作为导航光标。
 * 视线与目标方向的偏差在容差角内时,光标锁定在准星上;超出容差则跳到真实目标方向,
 * 玩家把准星重新对准史莱姆即可回到正确航向。/gps stop 取消导航。
 *
 * <p>光标是 polymer 虚拟实体(纯数据包,只对导航者本人可见):服务端不存在真实实体,
 * 天然无 AI、不可伤害、不随区块保存;位置每 tick 由服务端按最新玩家位置重算,
 * 鞘翅高速飞行时也保持稳定。一名玩家同时只有一个导航目标,重复 /gps 直接替换。
 */
public class GpsModule extends ServerModule {
    // 史莱姆(size=1)半高,用于把光标中心对齐到视线,而不是让史莱姆"脚踩"在视线上
    private static final double CURSOR_HALF_HEIGHT = 0.26;

    @Inject
    private GameConfigService config;

    private final Map<UUID, GpsSession> sessions = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var session = sessions.remove(handler.getPlayer().getUUID());
            if (session != null) session.destroy();
        });
        // 重生会让客户端重建世界,虚拟实体需要向新实例重发生成包
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            var session = sessions.get(newPlayer.getUUID());
            if (session != null) session.rewatch(oldPlayer, newPlayer);
        });
    }

    @Override
    public void onDisable() {
        sessions.values().forEach(GpsSession::destroy);
        sessions.clear();
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext registryAccess,
                                  Commands.CommandSelection env) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("gps")
                .requires(source -> this.isEnabled() && source.getPlayer() != null)
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("stop")
                        .executes(this::stop))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Coordinates>argument("target", Vec3Argument.vec3())
                        .executes(this::start)));
    }

    private int start(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        Vec3 target = Vec3Argument.getVec3(context, "target");
        var old = sessions.remove(player.getUUID());
        if (old != null) old.destroy();
        sessions.put(player.getUUID(), new GpsSession(player, target));
        context.getSource().sendSuccess(() -> Component.translatable("command.sfcraft.gps.started",
                Math.round(target.x), Math.round(target.y), Math.round(target.z)), false);
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> context) {
        var session = sessions.remove(context.getSource().getPlayer().getUUID());
        if (session == null) {
            context.getSource().sendFailure(Component.translatable("command.sfcraft.gps.not_active"));
            return 0;
        }
        session.destroy();
        context.getSource().sendSuccess(() -> Component.translatable("command.sfcraft.gps.stopped"), false);
        return 1;
    }

    private void onTick(MinecraftServer server) {
        if (sessions.isEmpty()) return;
        var gps = config.get().gps;
        var iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                session.destroy();
                iterator.remove();
                continue;
            }
            if (player.level().dimension() != session.dimension) {
                session.destroy();
                iterator.remove();
                player.sendSystemMessage(Component.translatable("command.sfcraft.gps.dimension_changed"));
                continue;
            }
            if (player.position().distanceToSqr(session.target) <= gps.arriveRadius * gps.arriveRadius) {
                session.destroy();
                iterator.remove();
                player.sendSystemMessage(Component.translatable("command.sfcraft.gps.arrived"));
                Helper.playNotifySound(player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
                continue;
            }
            session.tick(player, gps);
        }
    }

    private static final class GpsSession {
        private final Vec3 target;
        private final ResourceKey<Level> dimension;
        private final ElementHolder holder;
        // ManualAttachment 的位置供给器读取该字段;每 tick 按最新玩家位置重算后再 holder.tick()
        private volatile Vec3 cursorPos;

        GpsSession(ServerPlayer player, Vec3 target) {
            this.target = target;
            this.dimension = player.level().dimension();
            this.cursorPos = player.getEyePosition().add(player.getLookAngle()).subtract(0, CURSOR_HALF_HEIGHT, 0);
            this.holder = new ElementHolder();
            var cursor = new SimpleEntityElement(EntityTypes.SLIME); // 默认 size=1,即小史莱姆
            // 本体隐身、只留发光轮廓:发光轮廓穿墙渲染,即使光标位置进了地形玩家也能看到航向
            cursor.setInvisible(true);
            cursor.setGlowing(true);
            cursor.setSilent(true);
            cursor.setNoGravity(true);
            holder.addElement(cursor);
            new ManualAttachment(holder, (ServerLevel) player.level(), () -> this.cursorPos);
            holder.startWatching(player);
        }

        void tick(ServerPlayer player, GameConfig.Gps gps) {
            Vec3 eye = player.getEyePosition();
            double dx = target.x - eye.x;
            double dz = target.z - eye.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            if (horizontalDistance > gps.planarDistance) {
                // 远距离巡航:只在眼睛所在水平面上按左右转向(yaw)偏转,完全忽略俯仰。
                // 用 yaw 直接算水平朝向而非取视线的水平分量——鞘翅看正下方时视线水平分量趋近 0,
                // 归一化会得到 NaN;yaw 始终有定义,天然规避退化也天然"忽略俯仰"。
                Vec3 lookHoriz = horizontalDirection(player.getYRot());
                Vec3 targetHoriz = new Vec3(dx, 0, dz).normalize();
                double angle = Math.toDegrees(Math.acos(Mth.clamp(lookHoriz.dot(targetHoriz), -1.0, 1.0)));
                Vec3 horiz = angle <= gps.toleranceDegrees ? lookHoriz : targetHoriz;
                this.cursorPos = new Vec3(
                        eye.x + horiz.x * gps.cursorDistance,
                        eye.y - CURSOR_HALF_HEIGHT,
                        eye.z + horiz.z * gps.cursorDistance);
            } else {
                // 近距离:恢复全向(含俯仰)指示,便于精确对准竖直方向上的目标。
                // 容差内粘在准星方向(画面上纹丝不动),超出则跳到真实目标方向提示玩家回正。
                Vec3 toTarget = target.subtract(eye);
                Vec3 targetDir = toTarget.normalize();
                Vec3 look = player.getLookAngle();
                double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(targetDir), -1.0, 1.0)));
                Vec3 direction = angle <= gps.toleranceDegrees ? look : targetDir;
                double distance = Math.min(gps.cursorDistance, toTarget.length());
                this.cursorPos = eye.add(direction.scale(distance)).subtract(0, CURSOR_HALF_HEIGHT, 0);
            }
            holder.tick();
        }

        // Minecraft yaw(度)→ 水平单位朝向向量,与俯仰无关
        private static Vec3 horizontalDirection(float yawDegrees) {
            double yaw = Math.toRadians(yawDegrees);
            return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        }

        void rewatch(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
            holder.stopWatching(oldPlayer);
            holder.startWatching(newPlayer);
        }

        void destroy() {
            holder.destroy();
        }
    }
}
