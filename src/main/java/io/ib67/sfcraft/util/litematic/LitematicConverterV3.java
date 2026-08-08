package io.ib67.sfcraft.util.litematic;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import java.io.InputStream;

@Log4j2
public class LitematicConverterV3 extends LitematicConverter {
    public LitematicConverterV3(InputStream input, NbtAccounter sizeTracker) {
        super(input, sizeTracker);
    }

    /** See {@link LitematicConverter#LitematicConverter(CompoundTag, NbtAccounter)}. */
    protected LitematicConverterV3(CompoundTag preParsedRoot, NbtAccounter sizeTracker) {
        super(preParsedRoot, sizeTracker);
    }

    @Override
    protected CompoundTag convertRegionToSchematic(int dataVersion, CompoundTag region) {
        var schematicsTag = new CompoundTag();

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
        var paletteNbt = region.getListOrEmpty("BlockStatePalette");
        var wePalette = convertToWEPalette(paletteNbt);
        var tileEntities = region.getListOrEmpty("TileEntities");

        var blocksNbt = new CompoundTag();
        blocksNbt.put("Palette", wePalette);
        var weBlockData = convertToWEBlocks(size, region);
        blocksNbt.putByteArray("Data", weBlockData);
        blocksNbt.put("BlockEntities", convertToWETileEntities(tileEntities));
        validateData(wePalette, weBlockData, Math.abs(size.x()), Math.abs(size.z()));
        schematicsTag.put("Blocks", blocksNbt);
        var schematicsRoot = new CompoundTag();
        schematicsRoot.put("Schematic", schematicsTag);
        return schematicsRoot;
    }

    private void validateData(
            CompoundTag wePalette,
            byte[] weBlockData,
            int width,
            int length
    ) {
        log.info("Validating schematic");
        // find invalid palette ids
        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(weBlockData));
        var paletteIds = new Int2ObjectOpenHashMap<>();
        var touchedIds = new IntOpenHashSet();
        for (String key : wePalette.keySet()) {
            paletteIds.put(
                    wePalette.getInt(key)
                            .orElseThrow(() -> new IllegalStateException("Cannot find key " + key + " in palette from worldedit schematic"))
                    , key
            );
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
        paletteIds.keySet().intStream().filter(it -> !touchedIds.contains(it))
                .forEach(it -> log.error("Unused palette id: {}, blockState: {}", it, paletteIds.get(it)));
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
    protected Tag convertToWeMeta(SizeTuple size, CompoundTag region) {
        var pos = (CompoundTag) region.get("Position");
        if (pos == null) {
            throw new IllegalStateException("Cannot find Position");
        }
        var worldEditNbt = new CompoundTag();
        worldEditNbt.putIntArray("Origin", new int[]{
                pos.getInt("x").orElseThrow() + (size.x() < 0 ? size.x() + 1 : 0),
                pos.getInt("y").orElseThrow() + (size.y() < 0 ? size.y() + 1 : 0),
                pos.getInt("z").orElseThrow() + (size.z() < 0 ? size.z() + 1 : 0)
        });
        var Metadata = new CompoundTag();
        Metadata.put("WorldEdit", worldEditNbt);
        Metadata.putLong("Date", System.currentTimeMillis());

        return Metadata;
    }
}
