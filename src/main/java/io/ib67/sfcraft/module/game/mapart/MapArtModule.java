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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 地图画:把 http(s) 图片下载后映射为原版填充地图画(MapColor 调色板近似)。
 * <ul>
 *   <li>铁砧:把(空)地图重命名为图片 URL 即可,结果槽直接产出地图画(防抖后才真正下载)。</li>
 *   <li>{@code /mapfor <url>}:OP 直接获得成品。</li>
 *   <li>把地图画放入墙上的物品框时,以该框为左下角自动铺满整幅图片(按原图宽高比在 4x4 格内
 *       取最贴合的 cols × rows 切片,详见 {@link #gridFor})。墙面铺不下、或框朝上/朝下时,
 *       这次放入会被直接拒绝——宁可放不进,也不留一张孤零零的低清种子图。</li>
 *   <li>放进物品框的地图画一旦掉出,会连框一起消失(不掉落任何物品),见 {@code ItemFrameMixin}。</li>
 * </ul>
 * 玩家拿到的是单张"种子"地图(128x128 整图预览);上墙后每格替换为 128x128 的局部清晰切片。
 * 输入单边像素上限见
 * {@code GameConfig.MapArt#maxImageDimension},在解码像素前先读头部尺寸拦截,防止解压炸弹。
 * 生成物是普通 {@code FILLED_MAP}+{@code MAP_ID}/{@code CUSTOM_DATA} 组件,纯原版物品,无需 Polymer。
 * 名称走 {@code ITEM_NAME}(玩家不再看到每格"地图画"名牌,也不能再被铁砧重命名);
 * 上墙的切片用 {@code TOOLTIP_DISPLAY} 隐藏 {@code MAP_ID},工具提示只剩名称一行。
 */
@Log4j2
public class MapArtModule extends ServerModule {
    /**
     * 地图画标记(custom_data 里的布尔键),用于物品框掉落判定,可跨重启/复制保留。
     */
    public static final String MAP_ART_MARKER = "sfcraft_map_art";
    /**
     * 图片来源 URL(custom_data 里的字符串键),上墙展开时凭它从渲染缓存取整幅像素。
     */
    public static final String MAP_ART_URL = "sfcraft_map_art_url";
    /**
     * 铺墙格数(custom_data 里的整数键),在种子图上随 URL 一起写入。
     * 有了它,放置前的空间校验无需等下载/渲染就能同步做完。
     */
    public static final String MAP_ART_COLS = "sfcraft_map_art_cols";
    public static final String MAP_ART_ROWS = "sfcraft_map_art_rows";
    /**
     * 铺墙单边格数上限(gridFor 与种子图格数校验共用,防恶意伪造的种子图铺出超大实体墙)。
     */
    private static final int MAP_GRID_LIMIT = 4;
    private static final int ANVIL_DEBOUNCE_MS = 400;
    private static final int RENDER_CACHE_SIZE = 16;
    /**
     * JDK ImageIO 默认可栅格化的扩展名;webp 不在其中。
     */
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
    private final Cache<String, Rendered> renderCache = CacheBuilder.newBuilder()
            .maximumSize(RENDER_CACHE_SIZE)
            .build();
    private final Executor debouncedExecutor = CompletableFuture.delayedExecutor(ANVIL_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    /**
     * 远端下载/解码失败的地址黑名单:url -> 允许重试的 epoch millis。
     */
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
            if (outcome == null || outcome.rendered() == null) {
                target.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                        describe(outcome, error)));
                return;
            }
            give(target, buildMap(target.level(), outcome.rendered(), url,
                    target.getX(), target.getZ()));
            target.sendSystemMessage(Component.translatable("command.sfcraft.mapart.success"));
        }));
        return 1;
    }

    // ============================== 铁砧 ==============================

    /**
     * 监听 {@link SFCallbacks#ANVIL_CREATE_RESULT}:URL 命名的地图接管结果槽。
     */
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
                }, debouncedExecutor)
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
        if (outcome == null || outcome.rendered() == null) {
            session.failed = true;
            player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                    describe(outcome, error)));
            return;
        }
        var stack = buildMap(player.level(), outcome.rendered(), session.url,
                player.getX(), player.getZ());
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

    /**
     * {@code ItemFrameMixin} 在掉落前调用。返回 true 表示已接管:画连框消失,什么都不掉。
     */
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

    /**
     * 地图画的图片来源 URL(无此键时无法重建整幅,例如旧版本制作的地图画)。
     */
    public static @Nullable String mapArtUrl(ItemStack stack) {
        if (!isMapArt(stack)) {
            return null;
        }
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        return data.copyTag().getString(MAP_ART_URL).filter(MapArtModule::isUrlLike).orElse(null);
    }

    /**
     * 种子图记下的铺墙格数 {cols, rows};旧版本制作的地图画没有这两个键,返回 null。
     */
    private static int @Nullable [] mapArtGrid(ItemStack stack) {
        if (!isMapArt(stack)) {
            return null;
        }
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        int cols = tag.getIntOr(MAP_ART_COLS, 0);
        int rows = tag.getIntOr(MAP_ART_ROWS, 0);
        if (cols < 1 || rows < 1 || cols > MAP_GRID_LIMIT || rows > MAP_GRID_LIMIT) {
            return null;
        }
        return new int[]{cols, rows};
    }

    /**
     * {@code ItemFrameMixin} 在放入前调用(服务端主线程):返回 false 表示这面墙容不下整幅画,
     * 直接否掉这次交互——宁可不让放,也不留一张孤零零的低清种子图挂在墙上。
     */
    public boolean canPlaceMapArt(ItemFrame frame, ItemStack stack, @Nullable ServerPlayer player) {
        var grid = mapArtGrid(stack);
        if (grid == null || (grid[0] == 1 && grid[1] == 1)) {
            return true; // 非地图画/切片/旧版无格数信息/本就单格:照常放
        }
        if (!(frame.level() instanceof ServerLevel level)) {
            return true;
        }
        var facing = frame.getDirection();
        if (facing.getAxis().isVertical()) {
            sendKeyed(player, "message.sfcraft.mapart.expand.wall_only");
            return false;
        }
        if (!hasRoom(frame, level, facing, grid[0], grid[1])) {
            sendKeyed(player, "message.sfcraft.mapart.expand.no_space", grid[0], grid[1]);
            return false;
        }
        return true;
    }

    /**
     * {@code ItemFrameMixin} 在玩家把地图画放入空框后调用(服务端主线程):
     * 以该框为左下角,把整幅图片的其余格自动铺满墙面。
     */
    public void onMapArtPlaced(ItemFrame frame, ServerPlayer player) {
        var url = mapArtUrl(frame.getItem());
        if (url == null) {
            return;
        }
        var rendered = renderCache.getIfPresent(url);
        if (rendered != null) {
            expand(frame, rendered, player); // 渲染完就挂墙的常见路径:缓存必中,同步铺开
            return;
        }
        // 缓存被挤出或服务器重启过:按 URL 重新拉取,完成后回主线程再铺
        var expectedMapId = frame.getItem().get(DataComponents.MAP_ID);
        var playerId = player.getUUID();
        render(url).whenComplete((outcome, error) -> scheduleOnServer(() -> {
            if (frame.isRemoved() || !url.equals(mapArtUrl(frame.getItem()))) {
                return; // 框已消失或内容物已被换掉
            }
            if (!Objects.equals(expectedMapId, frame.getItem().get(DataComponents.MAP_ID))) {
                return; // 内容物虽仍像地图画,但已不是当初那张,放弃
            }
            var target = serverSupplier.get().getPlayerList().getPlayer(playerId);
            if (outcome == null || outcome.rendered() == null) {
                sendKeyed(target, "message.sfcraft.mapart.failed", describe(outcome, error));
                return;
            }
            expand(frame, outcome.rendered(), target);
        }));
    }

    /**
     * 主线程:把 frame(作为左下角)所在墙面铺满成 cols x rows 的整幅墙画。
     * 放置前已校验过空间,这里再兜一次底:下载期间墙面被占的话把种子图退还玩家。
     */
    private void expand(ItemFrame frame, Rendered art, @Nullable ServerPlayer player) {
        if (art.cols() == 1 && art.rows() == 1) {
            return; // 单格:种子图本身即成品
        }
        if (!(frame.level() instanceof ServerLevel level)) {
            return;
        }
        var facing = frame.getDirection();
        if (facing.getAxis().isVertical()) {
            sendKeyed(player, "message.sfcraft.mapart.expand.wall_only");
            revert(frame, player);
            return;
        }
        if (!hasRoom(frame, level, facing, art.cols(), art.rows())) {
            sendKeyed(player, "message.sfcraft.mapart.expand.no_space", art.cols(), art.rows());
            revert(frame, player);
            return;
        }
        // 观察者面对框时的右手方向:面朝向绕 Y 轴逆时针转 90 度(如框朝南,右侧为东)
        var right = facing.getCounterClockWise();
        var origin = frame.blockPosition();
        frame.setItem(buildTile(level, art, 0, 0, frame.getX(), frame.getZ()));
        for (int tx = 0; tx < art.cols(); tx++) {
            for (int ty = 0; ty < art.rows(); ty++) {
                if (tx == 0 && ty == 0) {
                    continue;
                }
                var tile = newFrameLike(frame, level, origin.relative(right, tx).above(ty), facing);
                tile.setItem(buildTile(level, art, tx, ty, frame.getX(), frame.getZ()));
                level.addFreshEntity(tile);
            }
        }
    }

    /**
     * 以 frame 为左下角,cols x rows 的墙面是否都能挂得下框(左下角那格是 frame 自己,跳过)。
     */
    private static boolean hasRoom(ItemFrame frame, ServerLevel level, Direction facing, int cols, int rows) {
        var right = facing.getCounterClockWise();
        var origin = frame.blockPosition();
        for (int tx = 0; tx < cols; tx++) {
            for (int ty = 0; ty < rows; ty++) {
                if (tx == 0 && ty == 0) {
                    continue;
                }
                var probe = newFrameLike(frame, level, origin.relative(right, tx).above(ty), facing);
                var occupied = !level.getEntitiesOfClass(HangingEntity.class, probe.getBoundingBox()).isEmpty();
                if (!probe.survives() || occupied) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 展开不成:把框腾空、种子图退回玩家。玩家已下线就只能维持现状(退无可退)。
     * 创造模式下原版 {@code consume} 不扣手上那份,退回等于复制,故只腾空不补发。
     */
    private static void revert(ItemFrame frame, @Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        var seed = frame.getItem().copy();
        frame.setItem(ItemStack.EMPTY);
        if (!player.hasInfiniteMaterials()) {
            give(player, seed);
        }
    }

    private static ItemFrame newFrameLike(ItemFrame template, ServerLevel level, BlockPos pos, Direction facing) {
        return template instanceof GlowItemFrame
                ? new GlowItemFrame(level, pos, facing)
                : new ItemFrame(level, pos, facing);
    }

    private static void sendKeyed(@Nullable ServerPlayer player, String key, Object... args) {
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }

    // ============================== 生成 ==============================

    /**
     * 在主线程调用:产出"种子"地图画——整幅 128x128 预览,记下来源 URL 供上墙展开。
     */
    private ItemStack buildMap(ServerLevel level, Rendered art, String url, double originX, double originZ) {
        var data = MapItemSavedData.createFresh(originX, originZ, (byte) 0, false, false, level.dimension());
        MapArtRenderer.copyToMapColors(art.canvas(), art.cols(), art.rows(), data.colors);
        return finishMap(level, data, url, art.cols(), art.rows());
    }

    /**
     * 在主线程调用:产出整幅墙画的一格切片(tx/ty 以左下角为原点),不带来源 URL(不再二次展开)。
     */
    private ItemStack buildTile(ServerLevel level, Rendered art, int tx, int ty, double originX, double originZ) {
        var data = MapItemSavedData.createFresh(originX, originZ, (byte) 0, false, false, level.dimension());
        int canvasWidth = art.cols() * MapArtRenderer.MAP_SIZE;
        int rowFromTop = art.rows() - 1 - ty; // 画布像素第 0 行在图像顶部,墙上第 0 格在最底部
        for (int y = 0; y < MapArtRenderer.MAP_SIZE; y++) {
            System.arraycopy(art.canvas(), ((rowFromTop * MapArtRenderer.MAP_SIZE + y) * canvasWidth)
                            + tx * MapArtRenderer.MAP_SIZE, data.colors,
                    y * MapArtRenderer.MAP_SIZE, MapArtRenderer.MAP_SIZE);
        }
        var stack = finishMap(level, data, null, 0, 0);
        stack.set(DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.MAP_ID, true));
        return stack;
    }

    /**
     * 锁定画布、申请 map id 并打包成带地图画标记的填充地图。url 为 null 时不写来源/格数(切片不再展开)。
     */
    private static ItemStack finishMap(ServerLevel level, MapItemSavedData data, @Nullable String url,
                                       int cols, int rows) {
        var locked = data.locked(); // 锁定:不可再被制图台改图/缩放
        locked.setDirty();
        var mapId = level.getFreeMapId();
        level.setMapData(mapId, locked);
        var stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, mapId);
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable("block.sfcraft.map_art").withStyle(style -> style.withItalic(false)));
        var marker = new CompoundTag();
        marker.putBoolean(MAP_ART_MARKER, true);
        if (url != null) {
            marker.putString(MAP_ART_URL, url);
            marker.putInt(MAP_ART_COLS, cols);
            marker.putInt(MAP_ART_ROWS, rows);
        }
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

    /**
     * 先读图片头部尺寸(不解码像素)拦截超大图,通过后再栅格化到整幅墙画画布。
     */
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
            var image = reader.read(0);
            var grid = gridFor(width, height);
            int canvasWidth = grid[0] * MapArtRenderer.MAP_SIZE;
            int canvasHeight = grid[1] * MapArtRenderer.MAP_SIZE;
            var canvas = grid[0] == 1 && grid[1] == 1
                    ? MapArtRenderer.render(image)
                    : MapArtRenderer.render(image, canvasWidth, canvasHeight);
            var rendered = new Rendered(grid[0], grid[1], canvas);
            renderCache.put(url, rendered);
            return RenderOutcome.ok(rendered);
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

    /**
     * 远端失败(网络/非图片/超大)后进入短时黑名单,避免玩家反复触发下载。
     */
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
                case DOWNLOAD_FAILED -> Component.translatable("message.sfcraft.mapart.error.download_failed",
                        outcome.detail() == null ? "?" : outcome.detail());
            };
        }
        return Component.translatable("message.sfcraft.mapart.error.download_failed", rootMessage(error));
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof java.util.concurrent.CompletionException
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

    /**
     * 按原图宽高比决定铺墙格数 {cols, rows}。
     * 在 {@code MAP_GRID_LIMIT} 内枚举所有 cols × rows 组合,取 {@code |ln((cols/rows) / 图片宽高比)|}
     * 最小的——让目标画布比例在原图比例的最近对数邻域里落位。整数离散化后常见输入的落位:
     * 1:1 → 1x1;3:2 / 16:9 → 3x2;用户那张 1.34 ≈ 4:3 → 4x3;明显长条(≥ ~2.4)封顶到 4x1~4x4 里的最贴比。
     * 装得下过去的旧版 3xN 种子图(格数源自 3 上限)依然能铺。
     */
    private static int[] gridFor(int imageWidth, int imageHeight) {
        double logAspect = Math.log((double) imageWidth / imageHeight);
        int bestCols = 1, bestRows = 1;
        double bestDistortion = Double.MAX_VALUE;
        for (int cols = 1; cols <= MAP_GRID_LIMIT; cols++) {
            for (int rows = 1; rows <= MAP_GRID_LIMIT; rows++) {
                double distortion = Math.abs(Math.log((double) cols / rows) - logAspect);
                if (distortion < bestDistortion - 1e-9) { // eps:失真度并列时保持靠前的组合,结果稳定
                    bestDistortion = distortion;
                    bestCols = cols;
                    bestRows = rows;
                }
            }
        }
        return new int[]{bestCols, bestRows};
    }

    private static boolean isUrlLike(String text) {
        var lower = text.toLowerCase(Locale.ROOT);
        return (lower.startsWith("https://") || lower.startsWith("http://"))
                && text.chars().noneMatch(Character::isWhitespace);
    }

    /**
     * 一次渲染的产物:cols*128 x rows*128 的整幅调色板像素(行优先铺开,第 0 行为图像顶部)。1x1 时 canvas=preview。
     */
    private record Rendered(int cols, int rows, byte[] canvas) {
    }

    private record RenderOutcome(@Nullable Rendered rendered, @Nullable RenderFailure failure,
                                 @Nullable String detail) {
        private static RenderOutcome ok(Rendered rendered) {
            return new RenderOutcome(rendered, null, null);
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
