package sfcraft;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import io.ib67.sfcraft.SFCraft;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import sfcraft.blocks.AccelerateBubble;

public class SFBlockEntities {
    public static final BlockEntityType<AccelerateBubble.BubbleBlockEntity> BUBBLE_BLOCK_ENTITY =
            register("accelerate_bubble_be", AccelerateBubble.BubbleBlockEntity::new, SFBlocks.ACCELERATE_BUBBLE);

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.of(SFCraft.MOD_ID, name);
        var type = Registry.register(Registries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
        PolymerBlockUtils.registerBlockEntity(type);
        return type;
    }
    public static void initialize(){}

}
