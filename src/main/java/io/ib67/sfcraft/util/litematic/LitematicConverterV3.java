package io.ib67.sfcraft.util.litematic;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.extern.log4j.Log4j2;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3i;

import java.io.InputStream;

@Log4j2
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
        var weBlockData = convertToWEBlocks(size, region);
        blocksNbt.putByteArray("Data", weBlockData);
        blocksNbt.put("BlockEntities", convertToWETileEntities(tileEntities));
        validateData(wePalette, weBlockData, Math.abs(size.x()), Math.abs(size.z()));
        schematicsTag.put("Blocks", blocksNbt);
        var schematicsRoot = new NbtCompound();
        schematicsRoot.put("Schematic", schematicsTag);
        return schematicsRoot;
    }

    private void validateData(
            NbtCompound wePalette,
            byte[] weBlockData,
            int width,
            int length
    ) {
        // find invalid palette ids
        var buf = new PacketByteBuf(Unpooled.wrappedBuffer(weBlockData));
        var paletteIds = new Int2ObjectOpenHashMap<>();
        var touchedIds = new IntOpenHashSet();
        for (String key : wePalette.getKeys()) {
            paletteIds.put(wePalette.getInt(key), key);
        }
        int counter = 0;
        try {
            while (buf.isReadable()) {
                counter++;
                var id = buf.readVarInt();
                if (!paletteIds.containsKey(id)) {
                    log.error("Cannot find id {} at {}th block in palette, pos: {}", id, counter, decodePositionFromDataIndex(width, length, counter));
                } else {
                    touchedIds.add(id);
                }
            }
        } catch (Exception e) {
            log.error("Error validing schematic", e);
        }
        paletteIds.keySet().intStream().filter(it->!touchedIds.contains(it))
                .forEach(it-> log.error("Unused palette id: {}, blockState: {}", it, paletteIds.get(it)));
    }
    private static Vec3i decodePositionFromDataIndex(int width, int length, int index) {
        // index = (y * width * length) + (z * width) + x
        int y = index / (width * length);
        int remainder = index - (y * width * length);
        int z = remainder / width;
        int x = remainder - z * width;
        return new Vec3i(x, y, z);
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
