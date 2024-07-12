package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.ConfigResources;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.inject.ConfigResource;
import io.ib67.sfcraft.inject.ConfigRoot;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static io.ib67.sfcraft.util.Helper.getConfigResource;

public class WelcomeModule extends ServerModule {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    @Inject
    private SFConfig config;
    @Inject
    @ConfigRoot
    private Path root;
    @Inject
    private GeoIPService geoIPService;
    private String updateLog;

    @Override
    public void onInitialize() {
        updateLog = getConfigResource(root, ConfigResources.UPDATE_LOG).orElse("他没写呢");
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    }

    @Provides
    @ConfigResource(ConfigResources.UPDATE_LOG)
    private String getUpdateLog() {
        return updateLog;
    }

    public void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender
            packetSender, MinecraftServer minecraftServer) {
        var player = serverPlayNetworkHandler.getPlayer();
        if (!player.getCommandTags().contains("has_joined_before") && isEnabled()) {
            player.addCommandTag("has_joined_before");
            player.sendMessage(Text.of("欢迎加入 SaltedFish Club Server!"));
            player.sendMessage(Text.of("您受本服务器成员邀请并第一次加入本服务器，以下是一些注意事项："));
            player.sendMessage(Text.of(" 1. 服务器内有 base，但我们也鼓励您自立门户"));
            player.sendMessage(Text.of(" 2. 下午 " + config.maintainceStartHour + " ~ " + config.maintainceEndHour + " 为服务器维护时间(防沉迷)，在这段时间内无法加入服务器。"));
            player.sendMessage(Text.of("如果您想邀请朋友加入服务器，可以向管理员提出申请。本服务器为正版服，但是离线玩家也可以加入（需备注）。"));
            player.sendMessage(Text.of("为了账号安全起见，请尽量使用正版账号登录后加入。此外，正/盗同名将会被视为两个不同的玩家存档，数据不互通。"));
            player.giveItemStack(generateHelperBook(updateLog, player));
            unlockRecipe(player);
        }
    }

    private ItemStack generateHelperBook(String updateLog, ServerPlayerEntity player) {
        Clock clock = null;
        if (player.networkHandler.getConnectionAddress() instanceof InetSocketAddress addr) {
            try {
                clock = geoIPService.clockOf(addr.getAddress());
            } catch (GeoIp2Exception ignored) {
            }
        }
        if (clock == null) clock = Clock.systemDefaultZone();
        var book = new ItemStack(Items.WRITTEN_BOOK);
        var updLog = new ArrayList<String>();
        updLog.add("本书修订截至至您加入服务器的那一刻 (" + FMT.format(ZonedDateTime.now(clock)) + ")\n" + "更多内容请查阅群公告");
        var pages = updateLog.length() / 100;
        for (int i = 0; i < pages; i++) {
            updLog.add(updateLog.substring(i * 100, (i + 1) * 100));
        }
        updLog.add(updateLog.substring(pages * 100));
        var component = new WrittenBookContentComponent(
                RawFilteredPair.of("特性列表"),
                "icybear & You",
                1,
                updLog.stream().map(it -> RawFilteredPair.of(Text.of(it))).toList(),
                false
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, component);
        return book;
    }

    private void unlockRecipe(ServerPlayerEntity player) {
        var recipe = player.getRecipeBook();
        recipe.unlockRecipes(player.server.getRecipeManager().values(), player);
    }
}
