package io.ib67.sfcraft.mixin.server.block;

import io.ib67.sfcraft.SFBlocks;
import io.ib67.sfcraft.SFRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.collection.IdList;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Debug(export = true)
@Mixin(BlockUpdateS2CPacket.class)
public class BlockUpdateS2CPacketMixin {
//    @Redirect(
//            method = "<clinit>",
//            at= @At(value = "FIELD", target = "Lnet/minecraft/block/Block;STATE_IDS:Lnet/minecraft/util/collection/IdList;")
//    )
//    private static IdList<BlockState> sf$hijackCodecIdList(){
//        return SFBlocks.HACKED_STATE_IDS;
//    }
}
