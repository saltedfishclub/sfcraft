package io.ib67.sfcraft.util.litematic;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link LitematicConverterV4} (the converter selected by
 * {@link LitematicConverterFactory} for the sample, a v7 litematic) against
 * {@code iron.litematic} and validates the output the same way WorldEdit's
 * {@code SpongeSchematicV3Reader} would consume it. {@link #runConverter} goes
 * through the factory, so this suite doubles as a regression test for the
 * version-based selection path.
 */
class LitematicConverterV4Test {

    private static final int EXPECTED_VOLUME = 3078;   // |19 * -9 * -18|
    private static final int EXPECTED_TOTAL_BLOCKS = 409; // litematic Metadata.TotalBlocks

    @Test
    void convertsSampleWithoutError() throws Exception {
        var result = runConverter();
        assertNotNull(result.schematic(), "converter produced no output");
        assertNotNull(result.name(), "converter produced no region name");
        System.out.println("[test] region name       = " + result.name());
        System.out.println("[test] schematic keys    = " + result.schematic().keySet());
    }

    @Test
    void producesValidSpongeV3Structure() throws Exception {
        var result = runConverter();
        CompoundTag schematic = result.schematic();
        CompoundTag schematicTag = requireCompound(schematic, "Schematic");

        assertEquals(3, schematicTag.getInt("Version").orElseThrow(), "Version must be 3 (Sponge v3)");
        assertEquals(19, (int) schematicTag.getShort("Width").orElseThrow(), "Width (|x|)");
        assertEquals(9, (int) schematicTag.getShort("Height").orElseThrow(), "Height (|y|)");
        assertEquals(18, (int) schematicTag.getShort("Length").orElseThrow(), "Length (|z|)");
        assertEquals(3, schematicTag.getIntArray("Offset").orElseThrow().length, "Offset must be int[3]");

        // Metadata.WorldEdit.Origin must be the minimum corner: (0, 0, 0) for this file
        var metadata = requireCompound(schematicTag, "Metadata");
        var worldEdit = requireCompound(metadata, "WorldEdit");
        assertArrayEquals(new int[]{0, 0, 0}, worldEdit.getIntArray("Origin").orElseThrow(), "Origin should be min corner");

        // Blocks container
        var blocks = requireCompound(schematicTag, "Blocks");
        assertTrue(blocks.contains("Palette"), "Blocks.Palette missing");
        assertTrue(blocks.contains("Data"), "Blocks.Data missing");
        assertTrue(blocks.contains("BlockEntities"), "Blocks.BlockEntities missing");
    }

    @Test
    void paletteMatchesSource() throws Exception {
        var result = runConverter();
        var blocks = requireCompound(requireCompound(result.schematic(), "Schematic"), "Blocks");
        var palette = requireCompound(blocks, "Palette");

        // 22 palette entries in the source litematic
        assertEquals(22, palette.size(), "palette entry count");

        // Regression: property values must not be rendered as Optional[...] (getString returns Optional)
        for (String key : palette.keySet()) {
            assertFalse(key.contains("Optional["),
                    "palette key must not contain Optional[...]: " + key);
            assertFalse(key.contains("null"),
                    "palette key must not contain 'null': " + key);
        }

        // air must be at id 0 (litematica always puts air first; WorldEdit needs a sane default)
        assertEquals("minecraft:air", palette.keySet().stream()
                .filter(k -> palette.getInt(k).orElseThrow() == 0)
                .findFirst().orElseThrow(() -> new AssertionError("no palette entry with id 0")),
                "id 0 must be minecraft:air");

        System.out.println("[test] palette:");
        var sorted = new TreeMap<Integer, String>();
        palette.keySet().forEach(k -> sorted.put(palette.getInt(k).orElseThrow(), k));
        sorted.forEach((id, name) -> System.out.printf("[test]   %2d -> %s%n", id, name));
    }

    @Test
    void blockDataDecodesToValidPaletteIds() throws Exception {
        var result = runConverter();
        var blocks = requireCompound(requireCompound(result.schematic(), "Schematic"), "Blocks");
        var palette = requireCompound(blocks, "Palette");
        byte[] data = blocks.getByteArray("Data").orElseThrow();

        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        List<Integer> ids = new ArrayList<>();
        while (buf.isReadable()) {
            ids.add(buf.readVarInt());
        }

        assertEquals(EXPECTED_VOLUME, ids.size(), "block count must equal |size| volume");

        int paletteMax = palette.size();
        long nonAir = 0;
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            assertTrue(id >= 0 && id < paletteMax,
                    "block #" + i + " has palette id " + id + " but palette only has " + paletteMax + " entries");
            if (id != 0) nonAir++;
        }

        // The source litematic metadata says 409 non-air blocks total
        assertEquals(EXPECTED_TOTAL_BLOCKS, nonAir, "non-air block count");

        // Sanity: the ground floor (y=0) at z=0 is a dirt_path strip in the source data
        // WorldEdit index = (y * length + z) * width + x, so data index 0 = (0,0,0)
        int first = ids.get(0);
        String firstName = palette.keySet().stream()
                .filter(k -> palette.getInt(k).orElseThrow() == first)
                .findFirst().orElseThrow();
        System.out.println("[test] block(0,0,0)      = id " + first + " " + firstName);
        assertEquals("minecraft:dirt_path", firstName, "bottom-front-left block should be dirt_path (source ground)");
    }

    @Test
    void worldEditRoundTripLayout() throws Exception {
        // Re-implement WorldEdit's SpongeSchematicV3Reader decoding and check the
        // produced layout matches the source litematic layout (min-corner ordered).
        var result = runConverter();
        CompoundTag schematicTag = requireCompound(result.schematic(), "Schematic");
        int width = schematicTag.getShort("Width").orElseThrow();
        int height = schematicTag.getShort("Height").orElseThrow();
        int length = schematicTag.getShort("Length").orElseThrow();

        var blocks = requireCompound(schematicTag, "Blocks");
        var palette = requireCompound(blocks, "Palette");
        byte[] data = blocks.getByteArray("Data").orElseThrow();

        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int[][] grid = new int[height][width * length];
        int index = 0;
        while (buf.isReadable()) {
            int id = buf.readVarInt();
            int y = index / (width * length);
            int z = (index % (width * length)) / width;
            int x = index % width;
            grid[y][z * width + x] = id;
            index++;
        }

        // Ground floor y=0 must be a solid path/grass layer (matches source analysis)
        int groundNonAir = 0;
        for (int i = 0; i < width * length; i++) {
            if (grid[0][i] != 0) groundNonAir++;
        }
        System.out.println("[test] y=0 non-air      = " + groundNonAir + " of " + (width * length));
        assertTrue(groundNonAir > 300, "ground floor should be mostly solid (source has 333 non-air at y=0)");

        // Bed must sit at y=5 like the source (bed foot id=16, head id=17)
        int bedAt = -1;
        for (int y = 0; y < height; y++) {
            for (int i = 0; i < width * length; i++) {
                if (grid[y][i] == 16 || grid[y][i] == 17) bedAt = y;
            }
        }
        System.out.println("[test] bed layer y     = " + bedAt);
        assertEquals(5, bedAt, "bed must be on the same layer as in the source (y=5)");
    }

    // --- V4-specific behavior ------------------------------------------------

    /** Missing 'Version' tag is tolerated (assumed modern format); must not throw. */
    @Test
    void toleratesMissingVersionTag() throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.put("Regions", new CompoundTag());
        try (var converter = new LitematicConverterV4(toInputStream(root), new NbtAccounter(10_240_000, 64))) {
            converter.read((n, t) -> fail("empty Regions must yield no regions"));
        }
    }

    /** Format version 1 uses the legacy {TileNBT, x, y, z} TileEntities wrapper; reject it. */
    @Test
    void rejectsLegacyVersion1() throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.putInt("Version", 1);
        root.put("Regions", new CompoundTag());
        var ex = assertThrows(IllegalStateException.class, () -> {
            try (var converter = new LitematicConverterV4(toInputStream(root), new NbtAccounter(10_240_000, 64))) {
                converter.read((n, t) -> {
                });
            }
        });
        assertTrue(ex.getMessage().contains("version 1"), "error must mention version 1: " + ex.getMessage());
    }

    /**
     * Litematica format >= 4 writes a per-region DataVersion. V4 must honor it
     * (falling back to the root MinecraftDataVersion), while V3 always uses the
     * root value. Run the same bytes through both to prove the difference.
     */
    @Test
    void honorsPerRegionDataVersion() throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.putInt("Version", 7);
        var regions = new CompoundTag();
        regions.put("r", minimalRegion(4902));
        root.put("Regions", regions);
        byte[] bytes = toByteArray(root);

        CompoundTag[] v4Out = new CompoundTag[1];
        try (var converter = new LitematicConverterV4(new ByteArrayInputStream(bytes), new NbtAccounter(10_240_000, 64))) {
            converter.read((n, t) -> v4Out[0] = t);
        }
        assertEquals(4902, requireCompound(v4Out[0], "Schematic").getInt("DataVersion").orElseThrow(),
                "V4 must prefer the per-region DataVersion");

        CompoundTag[] v3Out = new CompoundTag[1];
        try (var converter = new LitematicConverterV3(new ByteArrayInputStream(bytes), new NbtAccounter(10_240_000, 64))) {
            converter.read((n, t) -> v3Out[0] = t);
        }
        assertEquals(4903, requireCompound(v3Out[0], "Schematic").getInt("DataVersion").orElseThrow(),
                "V3 falls back to the root MinecraftDataVersion");
    }

    // --- helpers -----------------------------------------------------------

    private record ConversionResult(String name, CompoundTag schematic) {
    }

    private ConversionResult runConverter() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/iron.litematic")) {
            assertNotNull(in, "test resource /iron.litematic missing");
            CompoundTag[] out = new CompoundTag[1];
            String[] name = new String[1];
            try (var converter = LitematicConverterFactory.forFile(in, new NbtAccounter(10_240_000, 64))) {
                converter.read((n, tag) -> {
                    name[0] = n;
                    out[0] = tag;
                });
            }
            return new ConversionResult(name[0], out[0]);
        }
    }

    /** A minimal but structurally valid 1x1x1 region, all air, with the given per-region DataVersion. */
    private static CompoundTag minimalRegion(int dataVersion) {
        var region = new CompoundTag();
        var size = new CompoundTag();
        size.putInt("x", 1);
        size.putInt("y", 1);
        size.putInt("z", 1);
        region.put("Size", size);
        var pos = new CompoundTag();
        pos.putInt("x", 10);
        pos.putInt("y", 20);
        pos.putInt("z", 30);
        region.put("Position", pos);
        var palette = new ListTag();
        var air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        region.put("BlockStatePalette", palette);
        region.putLongArray("BlockStates", new long[]{0});
        region.put("TileEntities", new ListTag());
        region.putInt("DataVersion", dataVersion);
        return region;
    }

    private static InputStream toInputStream(CompoundTag root) throws Exception {
        return new ByteArrayInputStream(toByteArray(root));
    }

    private static byte[] toByteArray(CompoundTag root) throws Exception {
        var out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);
        return out.toByteArray();
    }

    private static CompoundTag requireCompound(CompoundTag parent, String key) {
        return parent.getCompound(key).orElseThrow(() -> new AssertionError("missing compound: " + key));
    }

    /** compile-time sanity: NbtIo.writeCompressed should accept the produced tag (used by SchematicUploader) */
    @Test
    void outputCanBeSerialized() throws Exception {
        var result = runConverter();
        var tmp = java.nio.file.Files.createTempFile("schematic", ".schematic");
        try {
            NbtIo.writeCompressed(result.schematic(), tmp);
            assertTrue(java.nio.file.Files.size(tmp) > 0);
            System.out.println("[test] wrote " + java.nio.file.Files.size(tmp) + " bytes to " + tmp);
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }
}
