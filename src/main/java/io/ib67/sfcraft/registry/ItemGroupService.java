package io.ib67.sfcraft.registry;

import net.minecraft.world.level.ItemLike;

/**
 * Collects the mod's items into a single server-side creative inventory tab (later synced to
 * Polymer-compatible clients). Feature modules contribute their items via {@link #add(ItemLike)}
 * in {@code onInitialize()}; a single finalizer module calls {@link #freeze(ItemLike)} last —
 * after every other module has registered — to build and register the tab from what was collected.
 * <p>
 * Collection is passive: the service only records what modules push to it and does not itself
 * know about any concrete feature. Adding after {@link #freeze(ItemLike)} is an error.
 */
public interface ItemGroupService {
    /** Contribute an item to the creative tab. Must be called before {@link #freeze(ItemLike)}. */
    void add(ItemLike item);

    /**
     * Build and register the creative tab from everything collected so far, using {@code icon}
     * as the tab icon. Must be called exactly once, after all {@link #add(ItemLike)} calls.
     */
    void freeze(ItemLike icon);
}
