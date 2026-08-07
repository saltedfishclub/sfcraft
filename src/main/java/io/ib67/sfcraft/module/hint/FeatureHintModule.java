package io.ib67.sfcraft.module.hint;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.lunchbox.LunchBoxItem;
import io.ib67.sfcraft.module.game.mount.MountAccess;
import io.ib67.sfcraft.module.game.mount.MountLogic;
import io.ib67.sfcraft.util.Helper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 特性提示模块:玩家「正在用低效方式做事」或「尚未发现相关特性」时,以一次性、低频、
 * 可点击隐藏的方式发提示。触发点全部挂在事件/mixin 回调上,不做逐 tick 轮询。
 */
public class FeatureHintModule extends ServerModule {
    private static final String TAG_PREFIX_SEEN = "sfhint_seen.";
    private static final String TAG_PREFIX_USED = "sfhint_used.";
    private static final String TAG_OPTED_OUT = "sfhint_opted_out";

    private static final int JOIN_SILENCE_TICKS = 200;
    private static final int ELYTRA_STALL_TICKS = 2;
    private static final int MOUNT_HINT_RANGE = 32;
    private static final int LUNCHBOX_MIN_LEVEL = 20;
    private static final float LUNCHBOX_HINT_CHANCE = 0.3f;

    @Inject
    private GameConfigService config;

    /** 全局节流:同一玩家两条提示之间的最小时间戳间隔(毫秒)。 */
    private final Map<UUID, Long> lastAnyAt = new HashMap<>();
    /** 已提示过的可驯服坐骑,全局去重(同一只怪只推荐一次)。 */
    private final Set<UUID> hintedMounts = new HashSet<>();
    private int slowTickCounter;

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register(this::onUseItem);
        SFCallbacks.PLAYER_SLOW_TICK.register(this::onSlowTick);
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    // ---------- 公共 API(mixin / 物品回调调用) ----------

    /** 玩家已掌握某特性,该提示永久静默。 */
    public void markUsed(ServerPlayer player, Hint hint) {
        player.addTag(TAG_PREFIX_USED + hint.id());
    }

    /** 玩家处于传送门内的每 tick 回调(NetherPortalBlockMixin):滑翔免教,站着等门则提示。 */
    public void mixin$onPortalTick(ServerPlayer player) {
        if (player.isFallFlying()) {
            markUsed(player, Hint.ELYTRA_PORTAL_RUSH);
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && player.portalProcess != null
                && player.portalProcess.getPortalTime() >= ELYTRA_STALL_TICKS) {
            tryEmitHint(player, Hint.ELYTRA_PORTAL_RUSH);
        }
    }

    // ---------- 事件回调 ----------

    private InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!config.get().featureHint.enabled) return InteractionResult.PASS;
        var stack = player.getItemInHand(hand);
        // 骑自家劫掠兽用胡萝卜钓竿(冲或不冲)= 已掌握冲刺手势,免教
        if (stack.is(Items.CARROT_ON_A_STICK)
                && player.getVehicle() instanceof Ravager rav
                && rav instanceof MountAccess mount
                && serverPlayer.getUUID().equals(mount.sfcraft$getOwner())) {
            markUsed(serverPlayer, Hint.RAVAGER_DASH);
            return InteractionResult.PASS;
        }
        maybeHintLunchBox(serverPlayer, stack);
        return InteractionResult.PASS;
    }

    /** 高等级玩家拿普通食物开吃且背包有 Bundle 时,小概率提示午餐盒。 */
    private void maybeHintLunchBox(ServerPlayer player, ItemStack stack) {
        if (player.experienceLevel <= LUNCHBOX_MIN_LEVEL) return;
        if (stack.getItem() instanceof LunchBoxItem) return;
        var food = stack.get(DataComponents.FOOD);
        if (food == null) {
            if (!stack.has(DataComponents.CONSUMABLE)) return;
        } else if (!player.canEat(food.canAlwaysEat())) {
            return; // 吃饱了,进食不会真正开始
        }
        if (!player.getInventory().contains(s -> s.is(Items.BUNDLE))) return;
        if (player.getRandom().nextFloat() >= LUNCHBOX_HINT_CHANCE) return;
        tryEmitHint(player, Hint.LUNCHBOX_EAT);
    }

    private void onSlowTick(ServerPlayer player, int intervalTicks) {
        if (!config.get().featureHint.enabled) return;
        var box = player.getBoundingBox().inflate(MOUNT_HINT_RANGE);
        if (hintUntamedNearby(player, Ravager.class, box)) return;
        hintUntamedNearby(player, Hoglin.class, box);
    }

    private <T extends Mob> boolean hintUntamedNearby(ServerPlayer player, Class<T> type, AABB box) {
        for (T mob : player.level().getEntitiesOfClass(type, box, FeatureHintModule::untamedAndReady)) {
            if (hintedMounts.add(mob.getUUID())) {
                tryEmitHint(player, Hint.MOUNT_TAME_NEARBY, mob.getDisplayName());
                return true;
            }
        }
        return false;
    }

    private static boolean untamedAndReady(Mob mob) {
        if (!(mob instanceof MountAccess mount) || mount.sfcraft$getOwner() != null) return false;
        return MountLogic.tameBlockReason(mob) == null;
    }

    // ---------- 服务端 tick:仅发放 PLAYER_SLOW_TICK(10s)节拍,供各模块低频检测复用 ----------

    private void onTick(MinecraftServer server) {
        if (++slowTickCounter < SFCallbacks.PlayerSlowTickCallback.INTERVAL_TICKS) return;
        slowTickCounter = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            SFCallbacks.PLAYER_SLOW_TICK.invoker().onPlayerSlowTick(
                    p, SFCallbacks.PlayerSlowTickCallback.INTERVAL_TICKS);
        }
    }

    // ---------- 提示发送闸 ----------

    /** 唯一发射点:opt-out / seen / used / 全局节流 / 入服静默,任一不过即不发。 */
    public void tryEmitHint(ServerPlayer player, Hint hint, Object... localizedArgs) {
        var cfg = config.get().featureHint;
        if (!cfg.enabled) return;
        if (player.entityTags().contains(TAG_OPTED_OUT)) return;
        if (player.entityTags().contains(TAG_PREFIX_SEEN + hint.id())) return;
        if (player.entityTags().contains(TAG_PREFIX_USED + hint.id())) return;
        long now = System.currentTimeMillis();
        if (now - lastAnyAt.getOrDefault(player.getUUID(), 0L) < cfg.globalCooldownSeconds * 1000L) return;
        if (player.tickCount < JOIN_SILENCE_TICKS) return;

        player.sendSystemMessage(buildHintComponent(hint, localizedArgs));
        player.addTag(TAG_PREFIX_SEEN + hint.id());
        if (hint.autoUsedOnEmit()) markUsed(player, hint);
        lastAnyAt.put(player.getUUID(), now);
        Helper.playNotifySound(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    private Component buildHintComponent(Hint hint, Object... args) {
        Component body = Component.translatable(hint.langKey(), args);
        Component dismiss = Component.translatable("message.sfcraft.hint.dismiss")
                .withStyle(style -> style
                        .applyFormat(ChatFormatting.GRAY)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand("/sfcraft hint hide " + hint.id()))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("message.sfcraft.hint.dismiss.hover"))));
        return Component.empty().append(body).append(dismiss);
    }

    // ---------- /sfcraft hint 命令 ----------

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext registryAccess,
                                  Commands.CommandSelection env) {
        var hint = Commands.literal("hint")
                .requires(source -> source.isPlayer())
                .then(Commands.literal("hide")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Hint.values()).map(Hint::id).toList(), builder))
                                .executes(ctx -> hideHint(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("off").executes(ctx -> setOptOut(ctx.getSource(), true)))
                .then(Commands.literal("on").executes(ctx -> setOptOut(ctx.getSource(), false)))
                .then(Commands.literal("list").executes(ctx -> listHints(ctx.getSource())));
        dispatcher.register(Commands.literal("sfcraft").then(hint));
    }

    private int hideHint(CommandSourceStack source, String id) {
        Hint hint = Hint.byId(id);
        if (hint == null) {
            source.sendFailure(Component.translatable("message.sfcraft.hint.unknown", id));
            return 0;
        }
        var player = (ServerPlayer) source.getEntity();
        player.addTag(TAG_PREFIX_SEEN + hint.id());
        source.sendSystemMessage(Component.translatable("message.sfcraft.hint.dismiss.hover"));
        return 1;
    }

    private int setOptOut(CommandSourceStack source, boolean optOut) {
        var player = (ServerPlayer) source.getEntity();
        if (optOut) player.addTag(TAG_OPTED_OUT);
        else player.removeTag(TAG_OPTED_OUT);
        source.sendSystemMessage(Component.translatable("message.sfcraft.hint.dismiss.hover"));
        return 1;
    }

    private int listHints(CommandSourceStack source) {
        var player = (ServerPlayer) source.getEntity();
        var sb = new StringBuilder("Hints: ");
        for (Hint hint : Hint.values()) {
            if (player.entityTags().contains(TAG_PREFIX_SEEN + hint.id())) sb.append(hint.id()).append("(seen) ");
            else if (player.entityTags().contains(TAG_PREFIX_USED + hint.id())) sb.append(hint.id()).append("(used) ");
        }
        if (player.entityTags().contains(TAG_OPTED_OUT)) sb.append("[opted_out]");
        source.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }
}
