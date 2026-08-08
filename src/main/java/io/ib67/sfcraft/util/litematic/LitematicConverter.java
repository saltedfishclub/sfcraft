package io.ib67.sfcraft.util.litematic;

import com.google.common.collect.Lists;
import io.ib67.sfcraft.util.TypedNbtList;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;

/**
 * A cleaned up version of https://github.com/GoldenDelicios/Lite2Edit
 * Uses {@link CompoundTag} from Minecraft.
 */
public class LitematicConverter implements AutoCloseable {
    protected final InputStream input;
    protected final NbtAccounter sizeTracker;
    private final CompoundTag preParsedRoot;

    public LitematicConverter(InputStream input, NbtAccounter sizeTracker) {
        this.input = input;
        this.sizeTracker = sizeTracker;
        this.preParsedRoot = null;
    }

    /**
     * For converters whose root tag was already parsed and validated (see
     * {@link LitematicConverterFactory}): {@link #read} uses this root instead of
     * re-parsing the stream, and {@link #close} leaves the caller's input alone.
     */
    protected LitematicConverter(CompoundTag preParsedRoot, NbtAccounter sizeTracker) {
        this.input = null;
        this.sizeTracker = sizeTracker;
        this.preParsedRoot = preParsedRoot;
    }

    @SneakyThrows
    public void read(
            BiConsumer<String, CompoundTag> schematicOutput
    ) {
        var root = preParsedRoot != null ? preParsedRoot : NbtIo.readCompressed(input, sizeTracker);
        var dataVersion = root.getInt("MinecraftDataVersion")
                .orElseThrow(() -> new IllegalStateException("MinecraftDataVersion not found"));
        // Template hooks: subclasses may adapt to the input format version
        // (see LitematicConverterV4) without re-implementing the whole pipeline.
        readInputVersion(root);
        var regionsNbt = root.getCompound("Regions")
                .orElseThrow(() -> new IllegalStateException("Regions not found"));
        for (String regionName : regionsNbt.keySet()) {
            var compound = regionsNbt.getCompound(regionName)
                    .orElseThrow(() -> new IllegalStateException("Region " + regionName + " not found"));
            var schematic = convertRegionToSchematic(readRegionDataVersion(compound, dataVersion), compound);
            schematicOutput.accept(regionName, schematic);
        }
    }

    /**
     * Template hook: the litematic input format version, read from the root tag.
     * Subclasses may log, validate or reject unsupported versions here.
     */
    protected int readInputVersion(CompoundTag root) {
        return root.getInt("Version").orElse(0);
    }

    /**
     * Template hook: the data version to bake into each region's schematic output.
     * The base implementation always uses the root {@code MinecraftDataVersion};
     * newer litematic formats store a per-region {@code DataVersion} that takes
     * precedence (litematica: {@code regionTag.getIntOrDefault("DataVersion", mainDataVersion)}).
     */
    protected int readRegionDataVersion(CompoundTag region, int fallback) {
        return fallback;
    }

    protected CompoundTag convertRegionToSchematic(int dataVersion, CompoundTag region) {
        var worldEditTag = new CompoundTag();

        // prepare metadata
        var size = readSizeTuple(region);
        worldEditTag.put("Metadata", convertToWeMeta(size, region));
        worldEditTag.putInt("DataVersion", dataVersion);

        // dimensional data.
        worldEditTag.putShort("Height", (short) Math.abs(size.y));
        worldEditTag.putShort("Length", (short) Math.abs(size.z));
        worldEditTag.putShort("Width", (short) Math.abs(size.x));

        // block & tile entities
        var paletteNbt = region.getListOrEmpty("BlockStatePalette");
        var wePalette = convertToWEPalette(paletteNbt);
        worldEditTag.putInt("PaletteMax", wePalette.keySet().size());
        worldEditTag.put("Palette", wePalette);
        var tileEntities = region.getListOrEmpty("TileEntities");
        worldEditTag.put("BlockEntities", convertToWETileEntities(tileEntities));
        worldEditTag.putInt("Version", 2);
        worldEditTag.putIntArray("Offset", new int[3]);
        worldEditTag.putByteArray("BlockData", convertToWEBlocks(size, region));
        var schematicsRoot = new CompoundTag();
        schematicsRoot.put("Schematic", worldEditTag);
        return schematicsRoot;
    }

    protected SizeTuple readSizeTuple(CompoundTag region) {
        var size = (CompoundTag) region.get("Size");
        if (size == null) {
            throw new IllegalStateException("Size not found");
        }
        var sizeX = size.getInt("x")
                .orElseThrow(() -> new IllegalStateException("Size X not found"));
        var sizeY = size.getInt("y")
                .orElseThrow(() -> new IllegalStateException("Size Y not found"));
        var sizeZ = size.getInt("z")
                .orElseThrow(() -> new IllegalStateException("Size Z not found"));
        return new SizeTuple(sizeX, sizeY, sizeZ);
    }

    protected ListTag convertToWETileEntities(ListTag tileEntities) {
        var weTEs = new TypedNbtList(Lists.newArrayList(), Tag.TAG_COMPOUND);

        for (Tag _tileEntity : tileEntities) {
            var weTE = new CompoundTag();
            var tE = (CompoundTag) _tileEntity;
            weTE.putIntArray("Pos", new int[]{
                    tE.getInt("x")
                            .orElseThrow(() -> new IllegalStateException("Pos X not found")),
                    tE.getInt("y")
                            .orElseThrow(() -> new IllegalStateException("Pos Y not found")),
                    tE.getInt("z")
                            .orElseThrow(() -> new IllegalStateException("Pos Z not found"))
            });
            weTE.putString("Id", tE.getString("id").orElseThrow(() -> new IllegalStateException("Id not found")));
            // other properties
            tE.remove("x");
            tE.remove("y");
            tE.remove("z");
            tE.remove("id");
            weTE.put("Data", tE);
            weTEs.add(weTE);
        }
        return weTEs;
    }

    protected CompoundTag convertToWEPalette(ListTag paletteNbt) {
        var wePalette = new CompoundTag();
        for (int i = 0; i < paletteNbt.size(); i++) {
            var entry = (CompoundTag) paletteNbt.get(i);
            var name = new StringBuilder(entry.getString("Name").orElseThrow(() -> new IllegalStateException("Name not found")));
            var _properties = entry.getCompound("Properties");
            if (_properties.isPresent()) {
                var properties = _properties.get();
                name.append("[");
                // Sort keys so the produced palette string is deterministic
                // (NbtCompound keySet order is unspecified in modern MC versions)
                var props = new ArrayList<String>();
                for (String key : properties.keySet().stream().sorted().toList()) {
                    props.add(key + "=" + properties.getString(key)
                            .orElseThrow(() -> new IllegalStateException("Property " + key + " not found")));
                }
                name.append(String.join(",", props));
                name.append("]");
            }
            wePalette.putInt(name.toString(), i);
        }
        return wePalette;
    }

    protected Tag convertToWeMeta(SizeTuple size, CompoundTag region) {
        var pos = (CompoundTag) region.get("Position");
        if (pos == null){
            throw new IllegalStateException("Pos not found");
        }
        var nbt = new CompoundTag();
        nbt.putInt("WEOffsetX", pos.getInt("x").orElseThrow() + (size.x < 0 ? size.x + 1 : 0));
        nbt.putInt("WEOffsetY", pos.getInt("y").orElseThrow() + (size.y < 0 ? size.y + 1 : 0));
        nbt.putInt("WEOffsetZ", pos.getInt("z").orElseThrow() + (size.z < 0 ? size.z + 1 : 0));
        return nbt;
    }

    protected byte[] convertToWEBlocks(SizeTuple size, CompoundTag region) {
        var blockCount = Math.abs(size.x * size.y * size.z);
        var blockStates = region.getLongArray("BlockStates").orElse(new long[0]);
        int bitsPerBlock = region.getListOrEmpty("BlockStatePalette").size();
        bitsPerBlock = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(bitsPerBlock - 1));
        int maxEntryValue = (1 << bitsPerBlock) - 1;
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        for (int index = 0; index < blockCount; index++) {
            int startBit = index * bitsPerBlock;
            int startLongIndex = startBit / 64;
            int startBitOffset = startBit % 64;
            int endBit = startBit + bitsPerBlock - 1;
            int endLongIndex = endBit / 64;

            int value;
            if (startLongIndex == endLongIndex) {
                value = (int) ((blockStates[startLongIndex] >>> startBitOffset) & maxEntryValue);
            } else {
                int bitsInFirstPart = 64 - startBitOffset; // 第一个 long 提取的位数
                long firstPart = blockStates[startLongIndex] >>> startBitOffset;
                long secondPart = blockStates[endLongIndex] & ((1L << (bitsPerBlock - bitsInFirstPart)) - 1);
                value = (int) ((firstPart | (secondPart << bitsInFirstPart)) & maxEntryValue);
            }
            buffer.writeVarInt(value);
        }
        // Only return the bytes actually written: the backing array of an
        // Unpooled.buffer() is a power-of-two slab that may be larger than the
        // written data, and trailing garbage would be read back as extra blocks
        // (as air) by WorldEdit's VarIntIterator.
        var result = new byte[buffer.writerIndex()];
        buffer.getBytes(0, result);
        return result;
    }

    @Override
    public void close() throws Exception {
        if (input != null) {
            input.close();
        }
    }

    protected record SizeTuple(
            int x, int y, int z
    ) {
    }
}
