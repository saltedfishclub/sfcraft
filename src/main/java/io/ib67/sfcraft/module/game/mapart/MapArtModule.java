package io.ib67.sfcraft.module.game.mapart;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.ServerModule;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 地图画:把 http(s) 图片下载后拉伸映射为一张原版 128x128 填充地图(MapColor 调色板近似)。
 * <ul>
 *   <li>铁砧:把(空)地图重命名为图片 URL 即可,结果槽直接产出地图画(防抖后才真正下载)。</li>
 *   <li>{@code /mapfor <url>}:OP 直接获得成品。</li>
 *   <li>放进物品框的地图画一旦掉出,会连框一起消失(不掉落任何物品),见 {@code ItemFrameMixin}。</li>
 * </ul>
 * 生成物是普通 {@code FILLED_MAP}+{@code MAP_ID}/{@code CUSTOM_DATA} 组件,纯原版物品,无需 Polymer。
 */
@Log4j2
public class MapArtModule extends ServerModule {
    /** 地图画标记(custom_data 里的布尔键),用于物品框掉落判定,可跨重启/复制保留。 */
    public static final String MAP_ART_MARKER = "sfcraft_map_art";
    private static final int ANVIL_DEBOUNCE_MS = 400;
    private static final int RENDER_CACHE_SIZE = 16;

    @Inject
    private GameConfigService configService;
    @Inject
    private MinecraftServerSupplier serverSupplier;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<AnvilMenu, AnvilSession> anvilSessions = Collections.synchronizedMap(new WeakHashMap<>());
    @SuppressWarnings("serial")
    private final Map<String, byte[]> renderCache = Collections.synchronizedMap(new java.util.LinkedHashMap<>(RENDER_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, byte[]> eldest) {
            return size() > RENDER_CACHE_SIZE;
        }
    });

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
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
        var url = StringArgumentType.getString(context, "url");
        source.sendSuccess(() -> Component.translatable("command.sfcraft.mapart.started", url), false);
        render(url).whenComplete((outcome, error) -> scheduleOnServer(() -> {
            if (outcome == null || outcome.pixels() == null) {
                player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                        describe(outcome, error)));
                return;
            }
            give(player, buildMap((ServerLevel) player.level(), outcome.pixels(), player.getX(), player.getZ()));
            player.sendSystemMessage(Component.translatable("command.sfcraft.mapart.success"));
        }));
        return 1;
    }

    // ============================== 铁砧 ==============================

    /** {@code AnvilMenuMixin} 在 createResult 尾部调用:URL 命名的地图接管结果槽。 */
    public void onAnvilResult(AnvilMenu menu, Player player) {
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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // 先压掉原版"重命名为 URL"的结果,下载渲染完成前玩家什么也拿不走
        access.sfcraft$setResult(ItemStack.EMPTY, 0);
        var newSession = new AnvilSession(name, serverPlayer);
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
        if (outcome == null || outcome.pixels() == null) {
            session.failed = true;
            session.player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.failed",
                    describe(outcome, error)));
            return;
        }
        var stack = buildMap((ServerLevel) session.player.level(), outcome.pixels(),
                session.player.getX(), session.player.getZ());
        if (session.player.isRemoved() || session.player.containerMenu != menu) {
            // 完成时砧子界面已关:直接发进背包
            anvilSessions.remove(menu);
            give(session.player, stack);
            session.player.sendSystemMessage(Component.translatable("message.sfcraft.mapart.success_inventory"));
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
        var cached = renderCache.get(url);
        if (cached != null) {
            return CompletableFuture.completedFuture(RenderOutcome.ok(cached));
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
        var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, settings.httpTimeoutSeconds)))
                .GET()
                .build();
        int maxBytes = Math.max(1024, settings.maxDownloadBytes);
        return httpClient.sendAsync(request,
                        HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofByteArray(), maxBytes))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return RenderOutcome.fail(RenderFailure.DOWNLOAD_FAILED, "HTTP " + response.statusCode());
                    }
                    try {
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
                        if (image == null) {
                            return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
                        }
                        byte[] pixels = MapArtRenderer.render(image);
                        renderCache.put(url, pixels);
                        return RenderOutcome.ok(pixels);
                    } catch (IOException e) {
                        log.warn("Failed to decode map art image from {}", url, e);
                        return RenderOutcome.fail(RenderFailure.NOT_IMAGE, null);
                    }
                })
                .exceptionally(error -> RenderOutcome.fail(RenderFailure.DOWNLOAD_FAILED, rootMessage(error)));
    }

    private Component describe(RenderOutcome outcome, Throwable error) {
        if (outcome != null && outcome.failure() != null) {
            return switch (outcome.failure()) {
                case INVALID_URL -> Component.translatable("message.sfcraft.mapart.error.invalid_url");
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
        var lower = text.toLowerCase(java.util.Locale.ROOT);
        return (lower.startsWith("https://") || lower.startsWith("http://"))
                && text.chars().noneMatch(Character::isWhitespace);
    }

    private record RenderOutcome(byte[] pixels, RenderFailure failure, String detail) {
        private static RenderOutcome ok(byte[] pixels) {
            return new RenderOutcome(pixels, null, null);
        }

        private static RenderOutcome fail(RenderFailure failure, String detail) {
            return new RenderOutcome(null, failure, detail);
        }
    }

    private enum RenderFailure {
        INVALID_URL, DOWNLOAD_FAILED, NOT_IMAGE
    }

    private static final class AnvilSession {
        private final String url;
        private final ServerPlayer player;
        private volatile CompletableFuture<RenderOutcome> future;
        private volatile ItemStack stack;
        private volatile boolean failed;

        private AnvilSession(String url, ServerPlayer player) {
            this.url = url;
            this.player = player;
        }
    }
}
