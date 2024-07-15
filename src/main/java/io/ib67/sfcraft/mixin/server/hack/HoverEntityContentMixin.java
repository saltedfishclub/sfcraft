package io.ib67.sfcraft.mixin.server.hack;

import io.ib67.sfcraft.SFCraftInitializer;
import io.ib67.sfcraft.SFEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.text.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoverEvent.EntityContent.class)
public class HoverEntityContentMixin {
    @Inject(method = "method_54198", at = @At("HEAD"), cancellable = true)
    private static void sf$mapToVanilla(HoverEvent.EntityContent content, CallbackInfoReturnable<EntityType> cir) {
        cir.setReturnValue(SFEntityType.mapToVanilla(content.entityType));
    }
}
