package sfcraft.blocks;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLookup;
import org.jetbrains.annotations.Nullable;
import sfcraft.SFBlockEntities;
import xyz.nucleoid.packettweaker.PacketContext;

public class AccelerateBubble extends BlockWithEntity implements PolymerBlock {
    public static final IntProperty LEVEL = IntProperty.of("level", 0, 3);

    public AccelerateBubble(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(AccelerateBubble::new);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, SFBlockEntities.BUBBLE_BLOCK_ENTITY, BubbleBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BubbleBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.OCHRE_FROGLIGHT.getDefaultState();
    }

    public static class BubbleBlockEntity extends BlockEntity {
        private final Box boundingBox;
        public BubbleBlockEntity(BlockPos pos, BlockState state) {
            super(SFBlockEntities.BUBBLE_BLOCK_ENTITY, pos, state);
            boundingBox = new Box(pos).expand(4);
        }

        public static void tick(World world, BlockPos pos, BlockState state, BubbleBlockEntity be) {
            if (world.isClient) return;

            world.getOtherEntities(null,
                    be.boundingBox
            ).forEach((entity) -> {
                var vel = entity.getVelocity();
                if (vel.lengthSquared() < 1e-9) return;
                entity.addVelocity(entity.getVelocity().normalize().multiply(4));
            });
        }
    }
}
