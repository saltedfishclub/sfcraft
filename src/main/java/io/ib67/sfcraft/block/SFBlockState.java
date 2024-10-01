package io.ib67.sfcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;

public class SFBlockState extends BlockState {
    public static final Codec<BlockState> CODEC = createCodec(Registries.BLOCK.getCodec(),
            it -> ((SFBlock) it).getMappedBlock().getDefaultState()).stable();

    public SFBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> reference2ObjectArrayMap, MapCodec<BlockState> mapCodec) {
        super(block, reference2ObjectArrayMap, mapCodec);
    }

    protected BlockState asBlockState() {
        return this;
    }
}
