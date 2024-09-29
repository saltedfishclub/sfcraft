package io.ib67.sfcraft.mixin.server.subserver;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow public abstract boolean isClient();
    @Shadow @Final private RegistryKey<World> registryKey;

    @Unique private GameRules sfcraft$customGameRule;

    @Inject(method = "getGameRules", at = @At("HEAD"), cancellable = true)
    private void beforeGetGameRules(CallbackInfoReturnable<GameRules> cir) {
        if (!isClient() && registryKey.getValue().getNamespace().equals(SFCraft.MOD_ID)) {
            if (sfcraft$customGameRule == null) {
                sfcraft$customGameRule = SFCraft.getInjector().getInstance(RoomModule.class).readGameRuleForRoom((ServerWorld) (Object) this);
            }
            cir.setReturnValue(sfcraft$customGameRule);
        }
    }
}
