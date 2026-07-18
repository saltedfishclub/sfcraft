package sfcraft;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import io.ib67.sfcraft.SFCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import sfcraft.entity.BombEntity;
import sfcraft.items.BombItem;
import sfcraft.items.LunchBoxItem;
import sfcraft.items.PearlTokenItem;
import sfcraft.items.ReversePearlTokenItem;
import sfcraft.items.SimplePolymerItem;

import java.util.function.Function;

public class SFItems {
    public static final Item AMETHYST_CAULDRON_BLANK = register(
            "amethyst_cauldron_blank",
            properties -> new SimplePolymerItem(properties, Items.RAW_IRON),
            new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ITEM_NAME, Component.literal("紫水晶炼药锅胚体"))
    );
    public static final Item AMETHYST_CAULDRON = register(
            "amethyst_cauldron",
            properties -> new PolymerBlockItem(SFBlocks.AMETHYST_CAULDRON, properties, Items.CAULDRON),
            new Item.Properties()
                    .component(DataComponents.ITEM_NAME, Component.literal("紫水晶炼药锅"))
    );
    public static final Item GRAVITY_CRYSTAL = register(
            "gravity_crystal",
            properties -> new PolymerBlockItem(SFBlocks.GRAVITY_CRYSTAL, properties, Items.RESPAWN_ANCHOR),
            new Item.Properties()
                    .component(DataComponents.ITEM_NAME, Component.literal("重力水晶"))
    );
    public static final Item BOMB = register(
            "bomb",
            properties -> new BombItem(properties, BombEntity.BombType.NORMAL, Items.FIRE_CHARGE),
            new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ITEM_NAME, Component.literal("炸弹"))
    );
    public static final Item OBSIDIAN_BOMB = register(
            "obsidian_bomb",
            properties -> new BombItem(properties, BombEntity.BombType.OBSIDIAN, Items.ECHO_SHARD),
            new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ITEM_NAME, Component.literal("黑曜石炸弹"))
    );
    public static final Item BLAZE_BOMB = register(
            "blaze_bomb",
            properties -> new BombItem(properties, BombEntity.BombType.BLAZE, Items.MAGMA_CREAM),
            new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ITEM_NAME, Component.literal("烈焰炸弹"))
    );
    public static final Item PEARL_TOKEN = register(
            "pearl_token",
            PearlTokenItem::new,
            new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ITEM_NAME, Component.literal("珍珠信物"))
    );
    public static final Item LUNCH_BOX = register(
            "lunch_box",
            LunchBoxItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.ITEM_NAME, Component.literal("午餐盒"))
                    // 0 营养 + canAlwaysEat:仅用于触发原版进食流程,真正的营养来自盒内食物
                    .component(DataComponents.FOOD, new FoodProperties(0, 0.0F, true))
                    .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)
    );
    public static final Item REVERSE_PEARL_TOKEN = register(
            "reverse_pearl_token",
            ReversePearlTokenItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.ITEM_NAME, Component.literal("反向珍珠信物"))
    );

    public static void initialize() {
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }
}
