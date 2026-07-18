package sfcraft;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import io.ib67.sfcraft.SFCraft;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import sfcraft.blocks.AmethystCauldronBlockEntity;
import sfcraft.blocks.GravityCrystalBlockEntity;

public class SFBlockEntities {
    public static final BlockEntityType<AmethystCauldronBlockEntity> AMETHYST_CAULDRON = register(
            "amethyst_cauldron",
            AmethystCauldronBlockEntity::new,
            SFBlocks.AMETHYST_CAULDRON
    );
    public static final BlockEntityType<GravityCrystalBlockEntity> GRAVITY_CRYSTAL = register(
            "gravity_crystal",
            GravityCrystalBlockEntity::new,
            SFBlocks.GRAVITY_CRYSTAL
    );

    public static void initialize() {
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> factory,
            Block... blocks
    ) {
        var id = Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, name);
        var type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
        PolymerBlockUtils.registerBlockEntity(type);
        return type;
    }
}
