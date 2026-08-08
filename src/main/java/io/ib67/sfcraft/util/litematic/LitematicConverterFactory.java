package io.ib67.sfcraft.util.litematic;

import lombok.SneakyThrows;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.InputStream;

/**
 * Selects the appropriate {@link LitematicConverter} for a litematic file based
 * on its format version (root {@code Version} tag), so callers never hardcode a
 * converter class.
 *
 * <p>Version → converter mapping (extend the dispatch as new formats appear):
 * <ul>
 *     <li><b>1</b> (legacy, ~MC 1.12): rejected — TileEntities use the
 *     {@code {TileNBT, x, y, z}} wrapper format, which the flat-field conversion
 *     in the converters does not support.</li>
 *     <li><b>2–3</b> (flat TileEntities, single data version):
 *     {@link LitematicConverterV3}.</li>
 *     <li><b>4+</b> (per-region {@code DataVersion}, current litematica; the
 *     sample {@code iron.litematic} is version 7): {@link LitematicConverterV4}.</li>
 * </ul>
 * A missing {@code Version} tag is treated as modern (>= 2).
 *
 * <p>The format version lives inside the gzip'd root tag, so the file is parsed
 * once here; the parsed root is handed to the chosen converter (via its
 * pre-parsed-root constructor) instead of being read a second time. The caller's
 * stream is consumed and closed by this method.
 */
public final class LitematicConverterFactory {
    private LitematicConverterFactory() {
    }

    @SneakyThrows
    public static LitematicConverter forFile(InputStream input, NbtAccounter sizeTracker) {
        var root = NbtIo.readCompressed(input, sizeTracker);
        input.close();
        int version = root.getInt("Version").orElse(0);
        if (version == 1) {
            throw new IllegalStateException(
                    "Unsupported litematic input version 1: version 1 TileEntities use the legacy "
                            + "{TileNBT, x, y, z} wrapper format, which the converters do not support");
        }
        if (version >= 2 && version < 4) {
            return new LitematicConverterV3(root, sizeTracker);
        }
        // version 0 (missing tag) and 4+ (including the current v7 sample)
        return new LitematicConverterV4(root, sizeTracker);
    }
}
