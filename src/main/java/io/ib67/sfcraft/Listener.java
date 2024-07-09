package io.ib67.sfcraft;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class Listener {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final SFCraft core;

    public Listener(SFCraft core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean onPlayerLogin(String currentPlayer, ClientConnection connection, Consumer<Text> disconnect) {
        var wl = core.getServer().getPlayerManager().getWhitelist();
        var offlineProfile = Uuids.getOfflinePlayerProfile(currentPlayer);
        if (connection.getAddress() instanceof InetSocketAddress address) {
            try {
                var clock = core.getGeoIPService().clockOf(address.getAddress());
                var config = core.getConfig();
                if (config.isClosed(clock) && !"iceBear67".equals(currentPlayer)) {
                    final var msg = "服务器尚未开放\n服务器维护时间： %1$d~%2$d\n你的下午不值得浪费在游戏上，晚点再来吧！";
                    disconnect.accept(Text.of(String.format(msg, config.maintainceStartHour, config.maintainceEndHour)));
                }
            } catch (Exception ignored) {
                System.err.println("Failed to locate " + address.getAddress());
            }
        }
        return wl.isAllowed(offlineProfile);
    }

    public void onPlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
        if (Helper.canBack(player)) {
            player.sendMessage(Text.of("Tip: 死亡地点附近有玩家，可以使用 /back 传送到他们那里。（即使在对方也死亡的状态下）"));
        }
    }

    public void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender
            packetSender, MinecraftServer minecraftServer) {
        var player = serverPlayNetworkHandler.getPlayer();
        if (!player.getCommandTags().contains("has_joined_before")) {
            player.addCommandTag("has_joined_before");
            player.sendMessage(Text.of("欢迎加入 SaltedFish Club Server!"));
            player.sendMessage(Text.of("您受本服务器成员邀请并第一次加入本服务器，以下是一些注意事项："));
            player.sendMessage(Text.of(" 1. 服务器内有 base，但我们也鼓励您自立门户"));
            player.sendMessage(Text.of(" 2. 服务器 QQ 群: (qq)"));
            player.sendMessage(Text.of(" 3. 下午 " + SFCraft.getInstance().getConfig().maintainceStartHour + " ~ " + SFCraft.getInstance().getConfig().maintainceEndHour + " 为服务器维护时间(防沉迷)，在这段时间内无法加入服务器。"));
            player.sendMessage(Text.of("该时间由您 IP 属地的时区决定。我们不希望您的下午仅仅荒废在服务器上。"));
            player.sendMessage(Text.of("如果您想邀请朋友加入服务器，可以向管理员提出申请。本服务器为正版服，但是离线玩家也可以加入（需备注）。"));
            player.sendMessage(Text.of("为了账号安全起见，请尽量使用正版账号登录后加入。此外，正/盗同名将会被视为两个不同的玩家存档，数据不互通。"));
            player.giveItemStack(generateHelperBook(core.getUpdateLog(), player));
            unlockRecipe(player);
        }
    }

    private ItemStack generateHelperBook(String updateLog, ServerPlayerEntity player) {
        Clock clock = null;
        if (player.networkHandler.getConnectionAddress() instanceof InetSocketAddress addr) {
            try {
                clock = SFCraft.getInstance().getGeoIPService().clockOf(addr.getAddress());
            } catch (GeoIp2Exception e) {
                clock = Clock.systemDefaultZone();
            }
        }
        var book = new ItemStack(Items.WRITTEN_BOOK);
        var updLog = new ArrayList<String>();
        updLog.add("本书修订截至至您加入服务器的那一刻 (" + FMT.format(ZonedDateTime.now(clock)) + ")\n" + "更多内容请查阅群公告");
        var pages = updateLog.length() / 100;
        for (int i = 0; i < pages; i++) {
            updLog.add(updateLog.substring(i * 100, (i + 1) * 100));
        }
        updLog.add(updateLog.substring(pages * 100, updateLog.length()));
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

    private final Set<String> motdRequestedIps = new HashSet<>();

    public ServerMetadata onMotdPing(MinecraftServer instance, ClientConnection connection) {
        var ip = connection.getAddressAsString(false);
        var cached = motdRequestedIps.contains(ip);
        if (!cached) {
            motdRequestedIps.add(ip);
        }
        var originalMetadata = instance.getServerMetadata();
        if (originalMetadata == null) return null;
        String version = null;
        if (connection.getAddress() instanceof InetSocketAddress address) {
            version = Helper.getVersionString(address);
        }
        if (version != null) {
            return new ServerMetadata(
                    originalMetadata.description(),
                    originalMetadata.players(),
                    Optional.of(new ServerMetadata.Version(version, 0)),
                    cached ? Optional.empty() : originalMetadata.favicon(),
                    originalMetadata.secureChatEnforced()
            );
        } else {
            return originalMetadata;
        }
    }
}
