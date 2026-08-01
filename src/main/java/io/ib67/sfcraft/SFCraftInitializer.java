package io.ib67.sfcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.*;
import io.ib67.sfcraft.module.*;
import io.ib67.sfcraft.module.chat.AntiSpamModule;
import io.ib67.sfcraft.module.chat.ChatPrefixModule;
import io.ib67.sfcraft.module.chat.decorator.ChatPingModule;
import io.ib67.sfcraft.module.chat.decorator.ChatSendLocModule;
import io.ib67.sfcraft.module.chat.decorator.ChatShowoffModule;
import io.ib67.sfcraft.module.compat.ModCompatModule;
import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.module.supervisor.WebModule;
import io.ib67.sfcraft.module.supervisor.web.SchematicUploader;
import io.ib67.sfcraft.module.game.GameExtensionModule;
import io.ib67.sfcraft.module.game.ItemGroupModule;
import io.ib67.sfcraft.module.game.beheading.BeheadingModule;
import io.ib67.sfcraft.module.game.bomb.BombModule;
import io.ib67.sfcraft.module.game.cauldron.CauldronModule;
import io.ib67.sfcraft.module.game.commander.CommanderModule;
import io.ib67.sfcraft.module.game.crystal.GravityCrystalModule;
import io.ib67.sfcraft.module.game.lunchbox.LunchBoxModule;
import io.ib67.sfcraft.module.game.mapart.MapArtModule;
import io.ib67.sfcraft.module.game.mount.MountModule;
import io.ib67.sfcraft.module.game.token.TokenModule;
import io.ib67.sfcraft.module.game.totem.TotemModule;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.ItemGroupService;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.registry.itemgroup.PolymerItemGroupService;
import io.ib67.sfcraft.registry.cauldron.SimpleCauldronRecipeRegistry;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import io.ib67.sfcraft.registry.event.SFRandomEventRegistry;
import io.ib67.sfcraft.init.GuiceModInitializer;
import io.ib67.sfcraft.geoip.GeoIPService;
import io.ib67.sfcraft.geoip.MaxMindGeoIPService;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.registry.RandomEventRegistry;
import io.ib67.sfcraft.module.command.BackModule;
import io.ib67.sfcraft.module.command.GpsModule;
import io.ib67.sfcraft.module.command.ManagementModule;
import io.ib67.sfcraft.module.command.ReloadCommandModule;
import io.ib67.sfcraft.registry.room.SimpleRoomRegistry;
import io.ib67.sfcraft.room.RoomTeleporter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;

@Log4j2
public class SFCraftInitializer extends GuiceModInitializer {
    @Override
    protected void configure() {
        super.configure();
        binder().bind(Gson.class).toInstance(new GsonBuilder().setPrettyPrinting().create());
        binder().bind(MinecraftServerSupplier.class).asEagerSingleton();
        registerServices();
        registerFeatures();
    }

    private void registerServices() {
        binder().bind(GeoIPService.class).to(MaxMindGeoIPService.class);
        binder().bind(RandomEventRegistry.class).to(SFRandomEventRegistry.class);
        binder().bind(SimpleMessageDecorator.class).in(Singleton.class);
        binder().bind(SignatureService.class).in(Singleton.class);
        binder().bind(RoomTeleporter.class).in(Singleton.class);
        binder().bind(RoomRegistry.class).to(SimpleRoomRegistry.class).in(Singleton.class);
        binder().bind(CauldronRecipeRegistry.class).to(SimpleCauldronRecipeRegistry.class).in(Singleton.class);
        binder().bind(ItemGroupService.class).to(PolymerItemGroupService.class).in(Singleton.class);
    }

    private void registerFeatures() {
        registerFeature(MotdModule.class);
        registerFeature(WelcomeModule.class);
        registerFeature(OfflineExemptModule.class);
        registerFeature(ElytraSpeedMeterModule.class);
        registerFeature(BackModule.class);
        registerFeature(ManagementModule.class);
        registerFeature(AFKModule.class);
        registerFeature(FartFertilizerModule.class);
        registerFeature(ChatPingModule.class);
        registerFeature(ChatSendLocModule.class);
        registerFeature(ModCompatModule.class);
        registerFeature(RoomModule.class);
        registerFeature(CreativeRoomModule.class);
        registerFeature(ChatPrefixModule.class);
        registerFeature(SoundModule.class);
        registerFeature(ChatShowoffModule.class);
        registerFeature(AntiSpamModule.class);
        registerFeature(ReloadCommandModule.class);
        registerFeature(GpsModule.class);
        registerGameFeatures();
        registerWebModules();
    }

    private void registerGameFeatures() {
        // GameExtensionModule 必须最先注册:它加载 gameplay.json,后续游戏模块注册内容时会读取配置
        registerFeature(GameExtensionModule.class);
        registerFeature(CauldronModule.class);
        registerFeature(GravityCrystalModule.class);
        registerFeature(BombModule.class);
        registerFeature(TokenModule.class);
        registerFeature(LunchBoxModule.class);
        registerFeature(BeheadingModule.class);
        registerFeature(CommanderModule.class);
        registerFeature(MountModule.class);
        registerFeature(TotemModule.class);
        registerFeature(MapArtModule.class);
        // ItemGroupModule 必须最后注册:它冻结前面各模块被动贡献到 ItemGroupService 的物品,构建创意物品栏
        registerFeature(ItemGroupModule.class);
    }

    private void registerWebModules() {
        registerFeature(WebModule.class);
        registerFeature(SchematicUploader.class);
    }

    @Override
    @SuppressWarnings("removal")
    protected void onReady(MinecraftServer minecraftServer) {
        log.info("SFCraft is loading");
        SFCraft.server = minecraftServer;
        getModuleManager().switchGlobalState(Lifecycle.State.ENABLED);
        log.info("Load completed! " + getModuleManager().getModules().size() + " modules were loaded.");
    }

    @Override
    protected void onStopping(MinecraftServer minecraftServer) {
        getModuleManager().switchGlobalState(Lifecycle.State.DISABLED);
    }

    @Override
    protected Injector onInit() {
        return SFCraft.injector = Guice.createInjector(
                new SFCraft(),
                this
        );
    }
}
