package io.ib67.sfcraft.mixin.server.block;

import io.ib67.sfcraft.SFBlocks;
import io.ib67.sfcraft.mixin.common.bridge.PalettedContainerBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin {
//    @Unique
//    private PalettedContainer<BlockState> hackedPalettedContainer;
//
//    @Inject(
//            method = "<init>(Lnet/minecraft/world/chunk/PalettedContainer;Lnet/minecraft/world/chunk/ReadableContainer;)V",
//            at=@At("TAIL")
//    )
//    private void sf$loadHackedContainer$1(PalettedContainer blockStateContainer, ReadableContainer biomeContainer, CallbackInfo ci){
//        this.hackedPalettedContainer = new PalettedContainer<>(
//                SFBlocks.HACKED_STATE_IDS,
//                Blocks.AIR.getDefaultState(),
//                ((PalettedContainerBridge)blockStateContainer).getPaletteProvider()
//        );
//    }
//
//    @Inject(
//            method = "<init>(Lnet/minecraft/registry/Registry;)V",
//            at=@At("TAIL")
//    )
//    private void sf$loadHackedContainer$2(Registry biomeRegistry, CallbackInfo ci){
//        this.hackedPalettedContainer =
//                new PalettedContainer<>(SFBlocks.HACKED_STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
//
//    }

//    @Redirect(
//            method = "toPacket",
//            at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/ChunkSection;blockStateContainer:Lnet/minecraft/world/chunk/PalettedContainer;")
//    )
//    private PalettedContainer<BlockState> toPacket(ChunkSection section) {
//        return this.hackedPalettedContainer;
//    }
}
