package io.ib67.sfcraft.module.chat;

import io.ib67.sfcraft.ServerModule;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 简单反刷屏:限制单个玩家的公屏发言频率,并拦截连续的重复消息。
 * <p>
 * 命中以下任意一条即拦截(消息不会广播,只给发言者一个 overlay 提示):
 * <ul>
 *   <li><b>频率限制</b>:{@link #WINDOW_MILLIS} 毫秒内最多放行 {@link #MAX_MESSAGES} 条。</li>
 *   <li><b>重复拦截</b>:与上一条内容相同(忽略大小写与首尾空白)且间隔不足
 *       {@link #DUPLICATE_WINDOW_MILLIS} 毫秒的消息被拦截。</li>
 * </ul>
 * 判定完全在服务端进行,走 Fabric {@code ALLOW_CHAT_MESSAGE} 钩子——返回 false 即取消该条消息。
 * 只影响公屏聊天;命令(如 /msg)走的是 {@code ALLOW_COMMAND_MESSAGE},不受影响。
 */
public class AntiSpamModule extends ServerModule {
    /** 频率窗口长度(毫秒)。 */
    private static final long WINDOW_MILLIS = 3_000L;
    /** 频率窗口内允许放行的最大消息数,超出即拦截。 */
    private static final int MAX_MESSAGES = 4;
    /** 判定「重复消息」的时间窗口(毫秒)。 */
    private static final long DUPLICATE_WINDOW_MILLIS = 10_000L;
    /** 两次 overlay 提示之间的最短间隔,避免提示本身也刷屏。 */
    private static final long WARN_COOLDOWN_MILLIS = 1_500L;

    // 仅在服务端主线程访问(聊天与断线回调都在主线程),无需并发容器。
    private final Map<UUID, Tracker> trackers = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(this::allowChat);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> trackers.remove(handler.getPlayer().getUUID()));
    }

    private boolean allowChat(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        if (!isEnabled()) return true;
        long now = System.currentTimeMillis();
        var tracker = trackers.computeIfAbsent(sender.getUUID(), ignored -> new Tracker());
        var content = message.signedContent().strip();

        var reasonKey = tracker.check(content, now);
        if (reasonKey != null) {
            // 被拦截的消息不计入放行窗口、也不刷新「上一条」,以免刷屏者借此绕过判定。
            warn(sender, tracker, now, reasonKey);
            return false;
        }
        tracker.accept(content, now);
        return true;
    }

    private void warn(ServerPlayer sender, Tracker tracker, long now, String reasonKey) {
        if (!tracker.canWarn(now)) return;
        sender.sendOverlayMessage(Component.translatable(reasonKey).withColor(CommonColors.RED));
    }

    /** 单个玩家的发言节流状态。 */
    private static final class Tracker {
        /** 近期已放行消息的时间戳,构成滑动窗口。 */
        private final Deque<Long> recent = new ArrayDeque<>();
        private String lastContent = "";
        private long lastContentTime;
        private long lastWarnTime;

        /** @return 拦截原因的翻译键,{@code null} 表示放行。 */
        String check(String content, long now) {
            if (!content.isEmpty()
                    && content.equalsIgnoreCase(lastContent)
                    && now - lastContentTime < DUPLICATE_WINDOW_MILLIS) {
                return "message.sfcraft.antispam.duplicate";
            }
            while (!recent.isEmpty() && now - recent.peekFirst() >= WINDOW_MILLIS) {
                recent.pollFirst();
            }
            if (recent.size() >= MAX_MESSAGES) {
                return "message.sfcraft.antispam.too_fast";
            }
            return null;
        }

        void accept(String content, long now) {
            recent.addLast(now);
            lastContent = content;
            lastContentTime = now;
        }

        boolean canWarn(long now) {
            if (now - lastWarnTime < WARN_COOLDOWN_MILLIS) return false;
            lastWarnTime = now;
            return true;
        }
    }
}
