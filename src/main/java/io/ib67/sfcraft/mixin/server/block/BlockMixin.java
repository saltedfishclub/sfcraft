package io.ib67.sfcraft.mixin.server.block;

import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.block.SFBlock;
import io.ib67.sfcraft.block.SFBlockState;
import io.ib67.sfcraft.util.HackedBlockStateIdList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.StateManager;
import net.minecraft.util.collection.IdList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(Block.class)
public class BlockMixin {
    @Shadow
    private RegistryEntry.Reference<Block> registryEntry;

    @Redirect(
            method="<clinit>",
            at= @At(value = "NEW",target = "()Lnet/minecraft/util/collection/IdList;")
    )
    private static IdList sf$hackIdList(){
        return new HackedBlockStateIdList();
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/DefaultedRegistry;createEntry(Ljava/lang/Object;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;")
    )
    public RegistryEntry.Reference sf$redirectCreateEntry(DefaultedRegistry instance, Object o) {
        if (this instanceof SFBlock) {
            return null;
        }
        return instance.createEntry(o);
    }

    @Redirect(
            method = "getRegistryEntry",
            at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;registryEntry:Lnet/minecraft/registry/entry/RegistryEntry$Reference;")
    )
    private RegistryEntry.Reference sf$redirectGetRegistryEntry(Block instance) {
        if (this instanceof SFBlock block) {
            return block.getMappedBlock().getRegistryEntry();
        }
        return this.registryEntry;
    }

    @Redirect(
            method = "asItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;fromBlock(Lnet/minecraft/block/Block;)Lnet/minecraft/item/Item;")
    )
    private Item sf$redirectAsItem(Block block) {
        if (block instanceof SFBlock) {
            return SFItems.getById(registryEntry.getKey().get().getValue());
        }
        return Item.fromBlock(block);
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/state/StateManager$Builder;build(Ljava/util/function/Function;Lnet/minecraft/state/StateManager$Factory;)Lnet/minecraft/state/StateManager;")
    )
    private StateManager<Block, BlockState>
    sf$redirectStateManager(StateManager.Builder<Block, BlockState> instance,
                            Function<Block, BlockState> defaultStateGetter,
                            StateManager.Factory<Block, BlockState> factory) {

        if (this instanceof SFBlock) return instance.build(Block::getDefaultState, SFBlockState::new);
        return instance.build(defaultStateGetter, factory);
    }

//    @Inject(
//            method = "getRawIdFromState",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private static void sf$redirectGetRawIdFromState(BlockState state, CallbackInfoReturnable<Integer> cir) {
//        if (state instanceof SFBlockState sfbs) {
//            System.out.println("sfblockstate!!!");
//            cir.setReturnValue(Block.getRawIdFromState(((SFBlock) sfbs.getBlock()).getMappedBlock().getDefaultState()));
//        }
//        if(state.getBlock() instanceof SFBlock){
//            System.out.println("SFBLOCK!!!!!");
//        }
//    }

//    @Inject(
//            method = "getDefaultState",
//            at=@At("HEAD"), cancellable = true
//    )
//    private void sf$redirectDefaultBlockState(CallbackInfoReturnable<BlockState> cir){
//        if(this instanceof SFBlock sfb){
//            cir.setReturnValue(sfb.getMappedBlock().getDefaultState());
//        }
//    }
}
