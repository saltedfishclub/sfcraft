package io.ib67.sfcraft.util;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class TypedNbtList extends NbtList {
    public TypedNbtList(List<NbtElement> list, byte type) {
        super(list, type);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeByte(this.getHeldType());
        output.writeInt(this.size());

        for (NbtElement nbtElement : this) {
            nbtElement.write(output);
        }
    }
}
