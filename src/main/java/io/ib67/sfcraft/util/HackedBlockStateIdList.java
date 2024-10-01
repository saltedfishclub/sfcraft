package io.ib67.sfcraft.util;

import io.ib67.sfcraft.block.SFBlock;
import io.ib67.sfcraft.block.SFBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.IdList;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class HackedBlockStateIdList extends IdList<BlockState> {
    @Override
    public int getRawId(BlockState value) {
        if (value instanceof SFBlockState bs) {
            return super.getRawId(((SFBlock) bs.getBlock()).getMappedBlock().getDefaultState());
        }
        return super.getRawId(value);
    }
}
