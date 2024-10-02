package io.ib67.sfcraft.mixin.server.block;

import io.ib67.sfcraft.SFBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.PaletteResizeListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ArrayPalette.class)
public class ArrayPaletteMixin {
    @Unique
    private IndexedIterable<?> networkIdList;
    @Inject(
            method = "<init>(Lnet/minecraft/util/collection/IndexedIterable;ILnet/minecraft/world/chunk/PaletteResizeListener;Ljava/util/List;)V",
            at=@At("TAIL")
    )
    private void sf$injectIdList(IndexedIterable idList, int bits, PaletteResizeListener listener, List list, CallbackInfo ci){
        if(idList == Block.STATE_IDS){
            networkIdList = SFBlocks.HACKED_STATE_IDS;
        }else{
            networkIdList = idList;
        }
    }

    @Redirect(
            method = "writePacket",
            at= @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/ArrayPalette;idList:Lnet/minecraft/util/collection/IndexedIterable;")
    )
    private IndexedIterable<?> sf$getNetworkIdList(ArrayPalette instance){
        return networkIdList;
    }
}
