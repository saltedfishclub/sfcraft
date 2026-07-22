package io.ib67.sfcraft.module.game.bomb;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.registry.ItemGroupService;
import lombok.Getter;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 三种投掷炸弹(普通/黑曜石/烈焰)与它们的战利品注入(地牢箱、苦力怕掉落)。
 */
@Getter
public class BombModule extends ServerModule {
    @Inject
    private GameConfigService config;
    @Inject
    private ItemGroupService itemGroups;

    private EntityType<BombEntity> bombEntityType;
    private Item bomb;
    private Item obsidianBomb;
    private Item blazeBomb;

    @Override
    public void onInitialize() {
        bombEntityType = RegistryHelper.registerEntity(
                "bomb",
                EntityType.Builder.<BombEntity>of(
                                (type, level) -> new BombEntity(type, level, config, () -> bomb),
                                MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(4)
                        .updateInterval(10)
        );
        bomb = registerBomb("bomb", BombEntity.BombType.NORMAL);
        obsidianBomb = registerBomb("obsidian_bomb", BombEntity.BombType.OBSIDIAN);
        blazeBomb = registerBomb("blaze_bomb", BombEntity.BombType.BLAZE);
        itemGroups.add(bomb);
        itemGroups.add(obsidianBomb);
        itemGroups.add(blazeBomb);
        registerLootModifiers();
    }

    private Item registerBomb(String name, BombEntity.BombType type) {
        return RegistryHelper.registerItem(
                name,
                properties -> new BombItem(properties, config, bombEntityType, type, Items.WARPED_FUNGUS_ON_A_STICK),
                new Item.Properties()
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/" + name))
                        .stacksTo(16)
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft." + name))
        );
    }

    private void registerLootModifiers() {
        // 概率在战利品表构建时(数据包加载)取样:改完配置需 /sfcraft reload + 原版 /reload 才生效
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            var loot = config.get().loot;
            if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(loot.dungeonBombChance))
                        .add(LootItem.lootTableItem(bomb)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(loot.dungeonObsidianBombChance))
                        .add(LootItem.lootTableItem(obsidianBomb)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))));
            }
            EntityTypes.CREEPER.getDefaultLootTable().ifPresent(creeperTable -> {
                if (creeperTable.equals(key)) {
                    tableBuilder.withPool(LootPool.lootPool()
                            .when(LootItemRandomChanceCondition.randomChance(loot.creeperBombChance))
                            .add(LootItem.lootTableItem(bomb)));
                }
            });
        });
    }
}
