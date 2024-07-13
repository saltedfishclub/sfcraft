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
            player.sendMessage(Text.of("§a§l欢迎加入 SaltedFish Club Server!"));
            player.sendMessage(Text.of("§a您受本服务器成员邀请加入本服务器，以下是一些注意事项："));
            player.sendMessage(Text.of(" - 服务器内已有的设施均为其他玩家的财产，(如可以)使用时请注意礼貌"));
            player.sendMessage(Text.of(" - 工作日下午 " + config.maintainceStartHour + " 至 " + config.maintainceEndHour + "点 为服务器维护时间 (防沉迷)"));
            player.sendMessage(Text.of("§c - 邀请新成员即等同为其作担保"));
            player.sendMessage(Text.of("§7如果您想邀请朋友加入服务器, 可以向管理员提出申请§7 (需备注正/盗版/小号)"));
            player.sendMessage(Text.of("§7此外, 本服务器无密码登录，盗版账号安全责任自负"));
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
        final var pageLen = 100;
        var pages = updateLog.length() / pageLen;
        for (int i = 0; i < pages; i++) {
            updLog.add(updateLog.substring(i * pageLen, (i + 1) * pageLen));
        }
        updLog.add(updateLog.substring(pages * pageLen));
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
