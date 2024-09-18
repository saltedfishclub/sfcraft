package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(World.class)
public abstract class WorldMixin {
    @Unique
    private GameRules customGameRule;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void sf$initCustomGameRule(MutableWorldProperties properties,
                                       RegistryKey<World> registryRef,
                                       DynamicRegistryManager registryManager,
                                       RegistryEntry<DimensionType> dimensionEntry,
                                       Supplier<Profiler> profiler,
                                       boolean isClient,
                                       boolean debugWorld,
                                       long biomeAccess,
                                       int maxChainedNeighborUpdates, CallbackInfo ci) {
        var id = registryRef.getValue();
        if (id.getNamespace().equals("sfcraft")) { // room
            customGameRule = SFCraft.getInjector().getInstance(RoomModule.class).readGameRuleForRoom(registryRef);
        } else {
            customGameRule = properties.getGameRules();
        }
    }

    /**
     * @reason for per-world rules
     * @author icybear
     */
    @Overwrite
    public GameRules getGameRules() {
        return customGameRule;
    }
}
