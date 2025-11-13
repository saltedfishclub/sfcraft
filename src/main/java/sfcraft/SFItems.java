package sfcraft;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import io.ib67.sfcraft.SFCraft;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class SFItems {
    public static final Item BLOCK_ACCELERATE_BUBBLE = register(
            "accelerate_bubble",
            s -> new PolymerBlockItem(
                    SFBlocks.ACCELERATE_BUBBLE,
                    s,
                    Items.OCHRE_FROGLIGHT
            ),
            new BlockItem.Settings().useBlockPrefixedTranslationKey()
    );

    public static void initialize(){}

    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SFCraft.MOD_ID, name));
        Item item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);

        return item;
    }
}
