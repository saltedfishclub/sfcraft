package io.ib67.sfcraft.registry.itemgroup;

import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.ItemGroupService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default {@link ItemGroupService} backed by Polymer's server-side creative tabs
 * ({@link PolymerCreativeModeTabUtils}). Items are kept in insertion order and de-duplicated;
 * {@link #freeze(ItemLike)} builds the tab and registers it under {@code sfcraft:general},
 * titled by the {@code itemGroup.sfcraft.general} translation key (resolved client-side via the
 * server resource pack).
 */
public class PolymerItemGroupService implements ItemGroupService {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "general");

    // LinkedHashSet: stable tab order (module registration order) with de-duplication.
    private final Set<ItemLike> items = new LinkedHashSet<>();
    private boolean frozen;

    @Override
    public void add(ItemLike item) {
        if (frozen) {
            throw new IllegalStateException("Item group is already frozen; cannot add " + item);
        }
        items.add(item);
    }

    @Override
    public void freeze(ItemLike icon) {
        if (frozen) {
            throw new IllegalStateException("Item group is already frozen");
        }
        frozen = true;
        var tab = PolymerCreativeModeTabUtils.builder()
                .title(Component.translatable("itemGroup.sfcraft.general"))
                .icon(() -> new ItemStack(icon))
                .displayItems((parameters, output) -> {
                    for (ItemLike item : items) {
                        output.accept(item);
                    }
                })
                .build();
        PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(ID, tab);
    }
}
