package io.ib67.sfcraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.nbt.*;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A cleaned up version of https://github.com/GoldenDelicios/Lite2Edit
 * Uses {@link NbtCompound} from Minecraft.
 */
public class LitematicConverter implements AutoCloseable {
    public static final String NBT_LITEMATICA_ROOT = "";
    private final InputStream input;
    private final NbtSizeTracker sizeTracker;
    private NbtCompound root;

    public LitematicConverter(InputStream input, NbtSizeTracker sizeTracker) {
        this.input = input;
        this.sizeTracker = sizeTracker;
    }

    @SneakyThrows
    public void read(
            BiConsumer<String, NbtCompound> schematicOutput
    ) {
        if (root != null) throw new IllegalStateException("Litematica file is already read");
        root = NbtIo.readCompressed(input, sizeTracker).getCompound(NBT_LITEMATICA_ROOT);
        var dataVersion = root.getInt("MinecraftDataVersion");
        var regionsNbt = root.getCompound("Regions");
        var i = 0;
        for (String regionName : regionsNbt.getKeys()) {
            var compound = regionsNbt.getCompound(regionName);
            var j = i++;
            var schematic = convertRegionToSchematic(dataVersion, compound);
            schematicOutput.accept(regionName, schematic);
        }
    }

    private NbtCompound convertRegionToSchematic(int dataVersion, NbtCompound region) {
        var worldEditTag = new NbtCompound();

        // prepare metadata
        var size = readSizeTuple(region);
        worldEditTag.put("Metadata", convertToWeMeta(size, region));
        worldEditTag.putInt("DataVersion", dataVersion);

        // dimensional data.
        worldEditTag.putShort("Height", (short) Math.abs(size.y));
        worldEditTag.putShort("Length", (short) Math.abs(size.z));
        worldEditTag.putShort("Width", (short) Math.abs(size.x));

        // block & tile entities
        var paletteNbt = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        var wePalette = convertToWEPalette(paletteNbt);
        worldEditTag.putInt("PaletteMax", wePalette.getKeys().size());
        worldEditTag.put("Palette", wePalette);
        var tileEntities = region.getList("TileEntities", NbtElement.COMPOUND_TYPE);
        worldEditTag.put("BlockEntities", convertToWETileEntities(tileEntities));
        worldEditTag.putInt("Version", 2);
        worldEditTag.putIntArray("Offset", new int[3]);
        worldEditTag.putByteArray("BlockData", convertToWEBlocks(size.x * size.y * size.z, region));
        var schematicsRoot = new NbtCompound();
        schematicsRoot.put("Schematic", worldEditTag);
        return schematicsRoot;
    }

    private SizeTuple readSizeTuple(NbtCompound region) {
        var size = (NbtCompound) region.get("Size");
        var sizeX = size.getInt("x");
        var sizeY = size.getInt("y");
        var sizeZ = size.getInt("z");
        return new SizeTuple(sizeX, sizeY, sizeZ);
    }

    private NbtList convertToWETileEntities(NbtList tileEntities) {
        var weTEs = new NbtList();
        for (NbtElement _tileEntity : tileEntities) {
            var tE = (NbtCompound) _tileEntity;
            tE.putIntArray("Pos", new int[]{
                    tE.getInt("x"),
                    tE.getInt("y"),
                    tE.getInt("z")
            });
            tE.putString("Id", tE.getString("id"));
            // other properties
            tE.remove("x");
            tE.remove("y");
            tE.remove("z");
            tE.remove("id");
            weTEs.add(tE);
        }
        return weTEs;
    }

    private NbtCompound convertToWEPalette(NbtList paletteNbt) {
        var wePalette = new NbtCompound();
        for (int i = 0; i < paletteNbt.size(); i++) {
            var entry = (NbtCompound) paletteNbt.get(i);
            var name = new StringBuilder(entry.getString("Name"));
            var properties = entry.getCompound("Properties");
            if (!properties.isEmpty()) {
                name.append("[");
                var props = new ArrayList<String>();
                for (String key : properties.getKeys()) {
                    props.add(key + "=" + properties.getString(key));
                }
                name.append(props.stream().collect(Collectors.joining(",")));
                name.append("]");
            }
            wePalette.putInt(name.toString(), i);
        }
        return wePalette;
    }

    private NbtElement convertToWeMeta(SizeTuple size, NbtCompound region) {
        var pos = (NbtCompound) region.get("Position");
        var nbt = new NbtCompound();
        nbt.putInt("WEOffsetX", pos.getInt("x") + size.x < 0 ? size.x + 1 : 0);
        nbt.putInt("WEOffsetY", pos.getInt("y") + size.y < 0 ? size.y + 1 : 0);
        nbt.putInt("WEOffsetZ", pos.getInt("z") + size.z < 0 ? size.z + 1 : 0);
        return nbt;
    }

    private byte[] convertToWEBlocks(int totalBlocks, NbtCompound region) {
        var buf = Unpooled.buffer();
        var blockStates = region.getLongArray("BlockStates");

        int bitsPerBlock = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE).size();
        bitsPerBlock = Math.max(2, Integer.SIZE - Integer.numberOfTrailingZeros(bitsPerBlock - 1));

        int bitCounts = 0, blockIterated = 0;
        long bitMask, bits = 0;
        for (long blockState : blockStates) {
            int remainingBits = bitCounts + 64;
            if (bitCounts != 0) {
                bitMask = (1L << (bitsPerBlock - bitCounts)) - 1;
                long newBits = (blockState & bitMask) << bitCounts;
                bits = bits | newBits;
                blockState = blockState >>> (bitsPerBlock - bitCounts);
                remainingBits -= bitsPerBlock;
                writeBlocks(buf, (short) bits);
                blockIterated++;
            }

            bitMask = (1L << bitsPerBlock) - 1;
            while(remainingBits >= bitsPerBlock){
                bits = blockState & bitMask;
                blockState = blockState >>> bitsPerBlock;
                remainingBits -= bitsPerBlock;
                if(blockIterated >= totalBlocks) break;
                writeBlocks(buf, (short) bits);
                blockIterated++;
            }
            bits = blockState;
            bitCounts = remainingBits;
        }
        return buf.array();
    }

    private static int writeBlocks(ByteBuf buf, short block) {
        int b = block >>> 7;
        if (b == 0) {
            buf.writeByte(block);
            return 1;
        } else {
            buf.writeByte(block | 128);
            buf.writeByte(block);
            return 2;
        }
    }

    @Override
    public void close() throws Exception {
        input.close();
    }

    record SizeTuple(
            int x, int y, int z
    ) {
    }
}
