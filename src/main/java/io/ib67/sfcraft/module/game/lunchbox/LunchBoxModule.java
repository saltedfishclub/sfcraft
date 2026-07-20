package io.ib67.sfcraft.module.game.lunchbox;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.module.game.RegistryHelper;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;

/**
 * 午餐盒:随身 9 格食物容器,右键直接吃盒内的食物,Shift+右键打开盒子。
 */
@Getter
public class LunchBoxModule extends ServerModule {
    private Item lunchBox;

    @Override
    public void onInitialize() {
        lunchBox = RegistryHelper.registerItem(
                "lunch_box",
                LunchBoxItem::new,
                new Item.Properties()
                        .stacksTo(1)
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/lunch_box"))
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.lunch_box"))
                        // 0 营养 + canAlwaysEat:仅用于触发原版进食流程,真正的营养来自盒内食物
                        .component(DataComponents.FOOD, new FoodProperties(0, 0.0F, true))
                        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)
        );
    }
}
