package io.ib67.sfcraft.module.game.lunchbox;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.module.game.RegistryHelper;
import io.ib67.sfcraft.registry.ItemGroupService;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;

/**
 * 午餐盒:随身食物收纳袋(伪装原版 bundle,按组数计容量,见 gameplay.json lunchBox 节),
 * 背包/容器 GUI 内点击装取,世界里右键直接吃盒内的食物。
 */
@Getter
public class LunchBoxModule extends ServerModule {
    @Inject
    private ItemGroupService itemGroups;
    @Inject
    private GameConfigService gameConfig;

    private Item lunchBox;

    @Override
    public void onInitialize() {
        lunchBox = RegistryHelper.registerItem(
                "lunch_box",
                properties -> new LunchBoxItem(properties, gameConfig),
                new Item.Properties()
                        .stacksTo(1)
                        .modelId(Identifier.fromNamespaceAndPath("sfcraft", "item/lunch_box"))
                        .component(DataComponents.ITEM_NAME, Component.translatable("item.sfcraft.lunch_box"))
                        // 0 营养 + canAlwaysEat:仅用于触发原版进食流程,真正的营养来自盒内食物
                        .component(DataComponents.FOOD, new FoodProperties(0, 0.0F, true))
                        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)
        );
        itemGroups.add(lunchBox);
    }
}
