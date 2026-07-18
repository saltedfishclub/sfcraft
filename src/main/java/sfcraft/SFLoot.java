package sfcraft;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class SFLoot {
    public static void registerModifiers() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.3F))
                        .add(LootItem.lootTableItem(SFItems.BOMB)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.15F))
                        .add(LootItem.lootTableItem(SFItems.OBSIDIAN_BOMB)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))));
            }
            EntityTypes.CREEPER.getDefaultLootTable().ifPresent(creeperTable -> {
                if (creeperTable.equals(key)) {
                    tableBuilder.withPool(LootPool.lootPool()
                            .when(LootItemRandomChanceCondition.randomChance(0.1F))
                            .add(LootItem.lootTableItem(SFItems.BOMB)));
                }
            });
        });
    }
}
