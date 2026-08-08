package io.ib67.sfcraft.util.litematic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link LitematicConverterFactory}: the format version (root
 * {@code Version} tag) must select the right converter class, reject the legacy
 * v1 format, and resolve the sample {@code iron.litematic} (a v7 file) to
 * {@link LitematicConverterV4}.
 */
class LitematicConverterFactoryTest {

    private static final NbtAccounter ACCOUNTER = new NbtAccounter(10_240_000, 64);

    @Test
    void selectsV3ForVersion2And3() throws Exception {
        for (int version : new int[]{2, 3}) {
            try (var converter = forVersion(version)) {
                assertEquals(LitematicConverterV3.class, converter.getClass(),
                        "litematic version " + version + " must map to LitematicConverterV3");
            }
        }
    }

    @Test
    void selectsV4ForVersion4And7() throws Exception {
        for (int version : new int[]{4, 7}) {
            try (var converter = forVersion(version)) {
                assertInstanceOf(LitematicConverterV4.class, converter,
                        "litematic version " + version + " must map to LitematicConverterV4");
            }
        }
    }

    @Test
    void treatsMissingVersionAsModern() throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.put("Regions", new CompoundTag());
        try (var converter = LitematicConverterFactory.forFile(toInputStream(root), ACCOUNTER)) {
            assertInstanceOf(LitematicConverterV4.class, converter, "missing Version tag must map to V4");
        }
    }

    @Test
    void rejectsLegacyVersion1() throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.putInt("Version", 1);
        root.put("Regions", new CompoundTag());
        var ex = assertThrows(IllegalStateException.class,
                () -> LitematicConverterFactory.forFile(toInputStream(root), ACCOUNTER));
        assertTrue(ex.getMessage().contains("version 1"), "error must mention version 1: " + ex.getMessage());
    }

    /** The sample is litematic version 7 → V4, and the produced schematic must be valid Sponge v3. */
    @Test
    void sampleResolvesToV4AndConverts() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/iron.litematic")) {
            assertNotNull(in, "test resource /iron.litematic missing");
            CompoundTag[] out = new CompoundTag[1];
            try (var converter = LitematicConverterFactory.forFile(in, ACCOUNTER)) {
                assertInstanceOf(LitematicConverterV4.class, converter, "sample is v7, must select V4");
                converter.read((n, tag) -> out[0] = tag);
            }
            assertNotNull(out[0], "converter produced no output");
            var schematic = out[0].getCompound("Schematic").orElseThrow();
            assertEquals(3, schematic.getInt("Version").orElseThrow(), "output must be Sponge v3");
            // sample region has no per-region DataVersion -> root MinecraftDataVersion (4903)
            assertEquals(4903, schematic.getInt("DataVersion").orElseThrow(), "DataVersion falls back to root");
        }
    }

    // --- helpers -----------------------------------------------------------

    private static LitematicConverter forVersion(int version) throws Exception {
        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", 4903);
        root.putInt("Version", version);
        var regions = new CompoundTag();
        regions.put("r", minimalRegion());
        root.put("Regions", regions);
        return LitematicConverterFactory.forFile(toInputStream(root), ACCOUNTER);
    }

    /** A minimal but structurally valid 1x1x1 region, all air. */
    private static CompoundTag minimalRegion() {
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
        return region;
    }

    private static InputStream toInputStream(CompoundTag root) throws Exception {
        var out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
