package io.ib67.sfcraft.util;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class TypedNbtList extends ListTag {
    private final byte heldType;
    public TypedNbtList(List<Tag> list, byte type) {
        super(list);
        this.heldType = type;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeByte(heldType);
        output.writeInt(this.size());

        for (Tag nbtElement : this) {
            nbtElement.write(output);
        }
    }
}
