package io.ib67.sfcraft.util.litematic;

import lombok.extern.log4j.Log4j2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;

import java.io.InputStream;

/**
 * Version-aware litematic → Sponge v3 converter, targeting the current
 * litematica schematic format (version 4+; the sample {@code iron.litematic}
 * is version 7).
 *
 * <p>Output format is identical to {@link LitematicConverterV3}; this class only
 * adds the input-side version handling via the template hooks in
 * {@link LitematicConverter}:
 * <ul>
 *     <li>{@link #readInputVersion}: logs the detected format version and
 *     rejects version 1, whose TileEntities use the legacy
 *     {@code {TileNBT, x, y, z}} wrapper instead of flat fields.</li>
 *     <li>{@link #readRegionDataVersion}: honors the per-region
 *     {@code DataVersion} tag (written by litematica since format v4), falling
 *     back to the root {@code MinecraftDataVersion}.</li>
 * </ul>
 */
@Log4j2
public class LitematicConverterV4 extends LitematicConverterV3 {
    public LitematicConverterV4(InputStream input, NbtAccounter sizeTracker) {
        super(input, sizeTracker);
    }

    /** See {@link LitematicConverter#LitematicConverter(CompoundTag, NbtAccounter)}. */
    protected LitematicConverterV4(CompoundTag preParsedRoot, NbtAccounter sizeTracker) {
        super(preParsedRoot, sizeTracker);
    }

    @Override
    protected int readInputVersion(CompoundTag root) {
        var version = super.readInputVersion(root);
        if (version < 2) {
            if (version == 0) {
                log.warn("Litematic input has no 'Version' tag; assuming modern format (>= 2)");
            } else {
                throw new IllegalStateException(
                        "Unsupported litematic input version " + version
                                + ": version 1 TileEntities use the legacy {TileNBT, x, y, z} wrapper format, "
                                + "which this converter does not support");
            }
        }
        log.info("Detected litematic input format version {}", version);
        return version;
    }

    @Override
    protected int readRegionDataVersion(CompoundTag region, int fallback) {
        // litematica format >= 4 stores a per-region DataVersion (see
        // LitematicaSchematic.readRegions: getIntOrDefault("DataVersion", mainDataVersion))
        return region.getInt("DataVersion").orElse(fallback);
    }
}
