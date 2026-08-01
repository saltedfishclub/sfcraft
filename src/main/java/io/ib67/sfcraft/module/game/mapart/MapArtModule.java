package io.ib67.sfcraft.module.game.mapart;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 地图画:把 http(s) 图片下载后拉伸映射为一张原版 128x128 填充地图(MapColor 调色板近似)。
 * <ul>
 *   <li>铁砧:把(空)地图重命名为图片 URL 即可,结果槽直接产出地图画(防抖后才真正下载)。</li>
 *   <li>{@code /mapfor <url>}:OP 直接获得成品。</li>
 *   <li>放进物品框的地图画一旦掉出,会连框一起消失(不掉落任何物品),见 {@code ItemFrameMixin}。</li>
 * </ul>
 * 无论原图多大,成品恒为一张 128x128 的地图物品;输入单边像素上限见
 * {@code GameConfig.MapArt#maxImageDimension},在解码像素前先读头部尺寸拦截,防止解压炸弹。
 * 生成物是普通 {@code FILLED_MAP}+{@code MAP_ID}/{@code CUSTOM_DATA} 组件,纯原版物品,无需 Polymer。
 */
@Log4j2
public class MapArtModule extends ServerModule {
    /** 地图画标记(custom_data 里的布尔键),用于物品框掉落判定,可跨重启/复制保留。 */
    public static final String MAP_ART_MARKER = "sfcraft_map_art";
    private static final int ANVIL_DEBOUNCE_MS = 400;
    private static final int RENDER_CACHE_SIZE = 16;
    /** JDK ImageIO 默认可栅格化的扩展名;webp 不在其中。 */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");

    @Inject
    private GameConfigService configService;
    @Inject
    private MinecraftServerSupplier serverSupplier;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<AnvilMenu, AnvilSession> anvilSessions = Collections.synchronizedMap(new WeakHashMap<>());
    private final Cache<String, byte[]> renderCache = CacheBuilder.newBuilder()
            .maximumSize(RENDER_CACHE_SIZE)
            .build();
    /** 远端下载/解码失败的地址黑名单:url -> 允许重试的 epoch millis。 */
    private final Map<String, Long> failedUrls = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
        SFCallbacks.ANVIL_CREATE_RESULT.register(this::onAnvilCreateResult);
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext registryAccess,
                                 Commands.CommandSelection env) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("mapfor")
                .requires(source -> this.isEnabled() && source.isPlayer()
                        && Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("url", StringArgumentType.string())
                        .executes(this::onMapFor)));
    }

    private int onMapFor(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        // 异步回调只带 UUID:下载期间玩家可能下线,强引用 ServerPlayer 会滞留整个下载周期
        var playerId = player.getUUID();
        var url = StringArgumentType.getString(context, "url");
        source.sendSuccess(() -> Component.translatable("command.sfcraft.mapart.started", url), false);
        render(url).whenComplete((outcome, error) -> scheduleOnServer(() -> {
            var target = serverSupplier.get().getPlayerList().getPlayer(playerId);
            if (target == null) {
                return; // 已下线;渲染结果仍在缓存里,重新执行命令即可
            }
            if (outcome == null || outcome.pixels() == null) {
                target.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                        describe(outcome, error)));
                return;
            }
            give(target, buildMap((ServerLevel) target.level(), outcome.pixels(), target.getX(), target.getZ()));
            target.sendSystemMessage(Component.translatable("command.sfcraft.mapart.success"));
        }));
        return 1;
    }

    // ============================== 铁砧 ==============================

    /** 监听 {@link SFCallbacks#ANVIL_CREATE_RESULT}:URL 命名的地图接管结果槽。 */
    public void onAnvilCreateResult(AnvilMenu menu, Player player) {
        var access = (MapArtAnvilAccess) menu;
        var input = access.sfcraft$getInput();
        var name = access.sfcraft$getItemName();
        if ((!input.is(Items.MAP) && !input.is(Items.FILLED_MAP)) || name == null || !isUrlLike(name)) {
            var removed = anvilSessions.remove(menu);
            if (removed != null && removed.future != null) {
                removed.future.cancel(false);
            }
            return;
        }
        var session = anvilSessions.get(menu);
        if (session != null && session.url.equals(name)) {
            // 同名重入(createResult 会因槽位/输入变动反复触发):失败过或未完成则保持压制,完成则补上结果
            access.sfcraft$setResult(session.stack == null ? ItemStack.EMPTY : session.stack,
                    session.stack == null ? 0 : anvilXpCost());
            return;
        }
        if (session != null && session.future != null) {
            session.future.cancel(false);
        }
        // 先压掉原版"重命名为 URL"的结果,下载渲染完成前玩家什么也拿不走
        access.sfcraft$setResult(ItemStack.EMPTY, 0);
        var newSession = new AnvilSession(name, player.getUUID());
        anvilSessions.put(menu, newSession);
        var future = CompletableFuture
                .runAsync(() -> {
                }, CompletableFuture.delayedExecutor(ANVIL_DEBOUNCE_MS, TimeUnit.MILLISECONDS))
                .thenCompose(ignored -> render(name));
        newSession.future = future;
        future.whenComplete((outcome, error) ->
                scheduleOnServer(() -> completeAnvilSession(menu, newSession, outcome, error)));
    }

    private void completeAnvilSession(AnvilMenu menu, AnvilSession session, RenderOutcome outcome, Throwable error) {
        if (anvilSessions.get(menu) != session) {
            return; // 玩家已经改动输入/改名,丢弃过期结果
        }
        var player = serverSupplier.get().getPlayerList().getPlayer(session.playerId);
        if (player == null || player.isRemoved()) {
            anvilSessions.remove(menu); // 已下线,界面也没了,无需再发
            return;
        }
        if (outcome == null || outcome.pixels() == null) {
            session.failed = true;
            player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                    describe(outcome, error)));
            return;
        }
        var stack = buildMap((ServerLevel) player.level(), outcome.pixels(), player.getX(), player.getZ());
        if (player.containerMenu != menu) {
            // 完成时砧子界面已关:直接发进背包
            anvilSessions.remove(menu);
            give(player, stack);
            player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.success_inventory"));
            return;
        }
        session.stack = stack;
        ((MapArtAnvilAccess) menu).sfcraft$setResult(stack, anvilXpCost());
    }

    // ============================== 物品框 ==============================

    /** {@code ItemFrameMixin} 在掉落前调用。返回 true 表示已接管:画连框消失,什么都不掉。 */
    public boolean onItemFrameDrop(ItemFrame frame) {
        if (!isMapArt(frame.getItem())) {
            return false;
        }
        frame.setItem(ItemStack.EMPTY);
        frame.discard();
        return true;
    }

    public static boolean isMapArt(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return false;
        }
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(MAP_ART_MARKER, false);
    }

    // ============================== 生成 ==============================

    /** 在主线程调用:申请 map id、写入像素并锁定,产出带标记的填充地图。 */
    public ItemStack buildMap(ServerLevel level, byte[] pixels, double originX, double originZ) {
        var data = MapItemSavedData.createFresh(originX, originZ, (byte) 0, false, false, level.dimension());
        System.arraycopy(pixels, 0, data.colors, 0, MapArtRenderer.MAP_SIZE * MapArtRenderer.MAP_SIZE);
        var locked = data.locked(); // 锁定:不可再被制图台改图/缩放
        locked.setDirty();
        var mapId = level.getFreeMapId();
        level.setMapData(mapId, locked);
        var stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, mapId);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sfcraft.map_art"));
        var marker = new CompoundTag();
        marker.putBoolean(MAP_ART_MARKER, true);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, marker);
        return stack;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ============================== 下载与渲染 ==============================

    private CompletableFuture<RenderOutcome> render(String url) {
        var settings = configService.get().mapArt;
        var cached = renderCache.getIfPresent(url);
        if (cached != null) {
            return CompletableFuture.completedFuture(RenderOutcome.ok(cached));
        }
        var retryAfter = failedUrls.get(url);
        if (retryAfter != null) {
            long remainingMillis = retryAfter - System.currentTimeMillis();
            if (remainingMillis > 0) {
                return CompletableFuture.completedFuture(RenderOutcome.fail(
                        RenderFailure.COOLDOWN, Long.toString(TimeUnit.MILLISECONDS.toSeconds(remainingMillis) + 1)));
            }
            failedUrls.remove(url);
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return CompletableFuture.completedFuture(RenderOutcome.fail(RenderFailure.INVALID_URL, null));
        }
        var scheme = uri.getScheme();
        if (uri.getHost() == null || scheme == null
                || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            return CompletableFuture.completedFuture(RenderOutcome.fail(RenderFailure.INVALID_URL, null));
        }
        // 防 SSRF:不允许 user:pass@host 这类内嵌验证信息
        if (uri.getUserInfo() != null) {
            return CompletableFuture.completedFuture(RenderOutcome.fail(RenderFailure.USERINFO, null));
        }
        var path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (SUPPORTED_EXTENSIONS.stream().noneMatch(path::endsWith)) {
            return CompletableFuture.completedFuture(RenderOutcome.fail(RenderFailure.UNSUPPORTED_EXTENSION, null));
        }
        var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, settings.httpTimeoutSeconds)))
                .GET()
                .build();
        int maxBytes = Math.max(1024, settings.maxDownloadBytes);
        return httpClient.sendAsync(request,
                        HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofByteArray(), maxBytes))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        markFailed(url);
                        return RenderOutcome.fail(RenderFailure.DOWNLOAD_FAILED, "HTTP " + response.statusCode());
                    }
                    var contentType = response.headers().firstValue("content-type").orElse("");
                    if (!contentType.isBlank() && !contentType.regionMatches(true, 0, "image/", 0, 6)) {
                        markFailed(url);
                        return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
                    }
                    return decode(url, response.body());
                })
                .exceptionally(error -> {
                    markFailed(url);
                    return RenderOutcome.fail(RenderFailure.DOWNLOAD_FAILED, rootMessage(error));
                });
    }

    /** 先读图片头部尺寸(不解码像素)拦截超大图,通过后再栅格化到 128x128。 */
    private RenderOutcome decode(String url, byte[] body) {
        int maxDimension = configService.get().mapArt.maxImageDimension;
        ImageReader reader = null;
        try (var imageStream = ImageIO.createImageInputStream(new ByteArrayInputStream(body))) {
            if (imageStream == null) {
                markFailed(url);
                return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
            }
            var readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                markFailed(url);
                return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
            }
            reader = readers.next();
            reader.setInput(imageStream);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width <= 0 || height <= 0) {
                markFailed(url);
                return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
            }
            if (width > maxDimension || height > maxDimension) {
                markFailed(url);
                return RenderOutcome.fail(RenderFailure.TOO_LARGE, width + "x" + height);
            }
            var pixels = MapArtRenderer.render(reader.read(0));
            renderCache.put(url, pixels);
            return RenderOutcome.ok(pixels);
        } catch (IOException e) {
            log.warn("Failed to decode map art image from {}", url, e);
            markFailed(url);
            return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    /** 远端失败(网络/非图片/超大)后进入短时黑名单,避免玩家反复触发下载。 */
    private void markFailed(String url) {
        long cooldownMillis = Math.max(0, configService.get().mapArt.failureCooldownSeconds) * 1000L;
        failedUrls.put(url, System.currentTimeMillis() + cooldownMillis);
    }

    private Component describe(RenderOutcome outcome, Throwable error) {
        if (outcome != null && outcome.failure() != null) {
            return switch (outcome.failure()) {
                case INVALID_URL -> Component.translatable("message.sfcraft.mapart.error.invalid_url");
                case USERINFO -> Component.translatable("message.sfcraft.mapart.error.userinfo");
                case UNSUPPORTED_EXTENSION ->
                        Component.translatable("message.sfcraft.mapart.error.unsupported_extension");
                case COOLDOWN -> Component.translatable("message.sfcraft.mapart.error.cooldown", outcome.detail());
                case TOO_LARGE -> Component.translatable("message.sfcraft.mapart.error.too_large",
                        outcome.detail(), configService.get().mapArt.maxImageDimension);
                case NOT_IMAGE -> Component.translatable("message.sfcraft.mapart.error.not_image");
                case DOWNLOAD_FAILED ->
                        Component.translatable("message.sfcraft.mapart.error.download_failed",
                                outcome.detail() == null ? "?" : outcome.detail());
            };
        }
        return Component.translatable("message.sfcraft.mapart.error.download_failed", rootMessage(error));
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause != null && (cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.util.concurrent.ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause == null ? "?" : String.valueOf(cause.getMessage());
    }

    private void scheduleOnServer(Runnable task) {
        var server = serverSupplier.get();
        if (server == null || server.isStopped()) {
            return;
        }
        server.execute(task);
    }

    private int anvilXpCost() {
        return Math.max(1, configService.get().mapArt.anvilXpCost);
    }

    private static boolean isUrlLike(String text) {
        var lower = text.toLowerCase(Locale.ROOT);
        return (lower.startsWith("https://") || lower.startsWith("http://"))
                && text.chars().noneMatch(Character::isWhitespace);
    }

    private record RenderOutcome(@Nullable byte[] pixels, @Nullable RenderFailure failure, @Nullable String detail) {
        private static RenderOutcome ok(byte[] pixels) {
            return new RenderOutcome(pixels, null, null);
        }

        private static RenderOutcome fail(RenderFailure failure, @Nullable String detail) {
            return new RenderOutcome(null, failure, detail);
        }
    }

    private enum RenderFailure {
        INVALID_URL, USERINFO, UNSUPPORTED_EXTENSION, COOLDOWN, DOWNLOAD_FAILED, NOT_IMAGE, TOO_LARGE
    }

    private static final class AnvilSession {
        private final String url;
        private final UUID playerId;
        private volatile CompletableFuture<RenderOutcome> future;
        private volatile ItemStack stack;
        private volatile boolean failed;

        private AnvilSession(String url, UUID playerId) {
            this.url = url;
            this.playerId = playerId;
        }
    }
}
