package io.ib67.sfcraft.util.litematic;

import io.ib67.sfcraft.util.TypedNbtList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import org.apache.commons.compress.utils.Lists;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A cleaned up version of https://github.com/GoldenDelicios/Lite2Edit
 * Uses {@link NbtCompound} from Minecraft.
 */
public class LitematicConverter implements AutoCloseable {
    protected final InputStream input;
    protected final NbtSizeTracker sizeTracker;

    public LitematicConverter(InputStream input, NbtSizeTracker sizeTracker) {
        this.input = input;
        this.sizeTracker = sizeTracker;
    }

    @SneakyThrows
    public void read(
            BiConsumer<String, NbtCompound> schematicOutput
    ) {
        var root = NbtIo.readCompressed(input, sizeTracker);
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

    protected NbtCompound convertRegionToSchematic(int dataVersion, NbtCompound region) {
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
        worldEditTag.putByteArray("BlockData", convertToWEBlocks(size, region));
        var schematicsRoot = new NbtCompound();
        schematicsRoot.put("Schematic", worldEditTag);
        return schematicsRoot;
    }

    protected SizeTuple readSizeTuple(NbtCompound region) {
        var size = (NbtCompound) region.get("Size");
        var sizeX = size.getInt("x");
        var sizeY = size.getInt("y");
        var sizeZ = size.getInt("z");
        return new SizeTuple(sizeX, sizeY, sizeZ);
    }

    protected NbtList convertToWETileEntities(NbtList tileEntities) {
        var weTEs = new TypedNbtList(Lists.newArrayList(), NbtElement.COMPOUND_TYPE);

        for (NbtElement _tileEntity : tileEntities) {
            var weTE = new NbtCompound();
            var tE = (NbtCompound) _tileEntity;
            weTE.putIntArray("Pos", new int[]{
                    tE.getInt("x"),
                    tE.getInt("y"),
                    tE.getInt("z")
            });
            weTE.putString("Id", tE.getString("id"));
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

    protected NbtCompound convertToWEPalette(NbtList paletteNbt) {
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

    protected NbtElement convertToWeMeta(SizeTuple size, NbtCompound region) {
        var pos = (NbtCompound) region.get("Position");
        var nbt = new NbtCompound();
        nbt.putInt("WEOffsetX", pos.getInt("x") + (size.x < 0 ? size.x + 1 : 0));
        nbt.putInt("WEOffsetY", pos.getInt("y") + (size.y < 0 ? size.y + 1 : 0));
        nbt.putInt("WEOffsetZ", pos.getInt("z") + (size.z < 0 ? size.z + 1 : 0));
        return nbt;
    }

    protected byte[] convertToWEBlocks(SizeTuple size, NbtCompound region) {
        var blockCount = Math.abs(size.x * size.y * size.z);
        var blockStates = region.getLongArray("BlockStates");
        int bitsPerBlock = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE).size();
        bitsPerBlock = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(bitsPerBlock - 1));
        int maxEntryValue = (1 << bitsPerBlock) - 1;
        var buffer = new PacketByteBuf(Unpooled.buffer());
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
        return buffer.array();
    }

    @Override
    public void close() throws Exception {
        input.close();
    }

    protected record SizeTuple(
            int x, int y, int z
    ) {
    }
}
