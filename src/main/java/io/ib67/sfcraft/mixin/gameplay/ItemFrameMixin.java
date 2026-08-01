package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.mapart.MapArtModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 地图画离开物品框即连框消失(不掉落画也不掉落框),见 {@link MapArtModule#onItemFrameDrop}。
 * 私有方法 {@code dropItem(ServerLevel, Entity, boolean)} 是内容物与框本体掉落物的唯一出口。
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void sfcraft$vanishMapArt(ServerLevel level, Entity causedBy, boolean withFrame, CallbackInfo ci) {
        if (SFCraft.getInjector().getInstance(MapArtModule.class)
                .onItemFrameDrop((ItemFrame) (Object) this)) {
            ci.cancel();
        }
    }
}
