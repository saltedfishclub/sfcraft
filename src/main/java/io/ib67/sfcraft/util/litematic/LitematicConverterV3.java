package io.ib67.sfcraft.util.litematic;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.InputStream;

public class LitematicConverterV3 extends LitematicConverter {
    public LitematicConverterV3(InputStream input, NbtSizeTracker sizeTracker) {
        super(input, sizeTracker);
    }
    
    @Override
    protected NbtCompound convertRegionToSchematic(int dataVersion, NbtCompound region) {
        var schematicsTag = new NbtCompound();

        // prepare metadata
        var size = readSizeTuple(region);
        schematicsTag.put("Metadata", convertToWeMeta(size, region));
        schematicsTag.putInt("DataVersion", dataVersion);
        schematicsTag.putInt("Version", 3);
        schematicsTag.putIntArray("Offset", new int[3]);

        // dimensional data.
        schematicsTag.putShort("Height", (short) Math.abs(size.y()));
        schematicsTag.putShort("Length", (short) Math.abs(size.z()));
        schematicsTag.putShort("Width", (short) Math.abs(size.x()));

        // block & tile entities
        var paletteNbt = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        var wePalette = convertToWEPalette(paletteNbt);
        var tileEntities = region.getList("TileEntities", NbtElement.COMPOUND_TYPE);

        var blocksNbt = new NbtCompound();
        blocksNbt.put("Palette", wePalette);
        blocksNbt.putByteArray("Data", convertToWEBlocks(Math.abs(size.x() * size.y() * size.z()), region));
        blocksNbt.put("BlockEntities", convertToWETileEntities(tileEntities));

        schematicsTag.put("Blocks", blocksNbt);
        var schematicsRoot = new NbtCompound();
        schematicsRoot.put("Schematic", schematicsTag);
        return schematicsRoot;
    }

    @Override
    protected NbtElement convertToWeMeta(SizeTuple size, NbtCompound region) {
        var pos = (NbtCompound) region.get("Position");
        var worldEditNbt = new NbtCompound();
        worldEditNbt.putIntArray("Origin", new int[]{
                pos.getInt("x") + (size.x() < 0 ? size.x() + 1 : 0),
                pos.getInt("y") + (size.y() < 0 ? size.y() + 1 : 0),
                pos.getInt("z") + (size.z() < 0 ? size.z() + 1 : 0)
        });
        var Metadata = new NbtCompound();
        Metadata.put("WorldEdit", worldEditNbt);
        Metadata.putLong("Date", System.currentTimeMillis());

        return Metadata;
    }
}
