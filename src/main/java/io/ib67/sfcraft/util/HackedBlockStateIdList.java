package io.ib67.sfcraft.util;

import io.ib67.sfcraft.block.SFBlock;
import io.ib67.sfcraft.block.SFBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.IdList;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class HackedBlockStateIdList extends IdList<BlockState> {
    private final IdList<BlockState> upstream;

    public HackedBlockStateIdList() {
        upstream = Block.STATE_IDS;
    }

    @Override
    public int getRawId(BlockState value) {
        if (value instanceof SFBlockState bs) {
            System.out.println("Mapped.");
            return upstream.getRawId(((SFBlock) bs.getBlock()).getMappedBlock().getDefaultState());
        }
        return upstream.getRawId(value);
    }

    @Override
    public void set(BlockState value, int id) {
        upstream.set(value, id);
    }

    @Override
    public void add(BlockState value) {
        upstream.add(value);
    }

    @Override
    public Iterator<BlockState> iterator() {
        return upstream.iterator();
    }

    @Override
    public boolean containsKey(int index) {
        return upstream.containsKey(index);
    }

    @Override
    public int size() {
        return upstream.size();
    }

    @Override
    public BlockState getOrThrow(int index) {
        return upstream.getOrThrow(index);
    }

    @Override
    public int getRawIdOrThrow(BlockState value) {
        return upstream.getRawIdOrThrow(value);
    }

    @Override
    public void forEach(Consumer<? super BlockState> action) {
        upstream.forEach(action);
    }

    @Override
    public Spliterator<BlockState> spliterator() {
        return upstream.spliterator();
    }
}
