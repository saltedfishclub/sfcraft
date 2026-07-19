package sfcraft;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import sfcraft.blocks.AmethystCauldronBlock;
import sfcraft.blocks.GravityCrystalBlock;

import java.util.function.Function;

public class SFBlocks {
    public static final Block AMETHYST_CAULDRON = register(
            "amethyst_cauldron",
            AmethystCauldronBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 10)
    );
    public static final Block GRAVITY_CRYSTAL = register(
            "gravity_crystal",
            GravityCrystalBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> Math.min(15, state.getValue(GravityCrystalBlock.CHARGE) * 4))
    );

    public static void initialize() {
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
