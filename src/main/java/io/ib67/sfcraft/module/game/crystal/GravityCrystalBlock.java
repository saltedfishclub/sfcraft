package io.ib67.sfcraft.module.game.crystal;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class GravityCrystalBlock extends BaseEntityBlock implements PolymerBlock {
    public static final IntegerProperty CHARGE = RespawnAnchorBlock.CHARGE;
    public static final MapCodec<GravityCrystalBlock> CODEC = simpleCodec(GravityCrystalBlock::new);

    // GravityCrystalModule 注册方块实体类型后回填(方块与其方块实体类型互相引用,无法在构造器闭合)
    private BlockEntityType<GravityCrystalBlockEntity> blockEntityType;
    private BiFunction<BlockPos, BlockState, GravityCrystalBlockEntity> blockEntityFactory;

    public GravityCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(CHARGE, 0));
    }

    void wireBlockEntity(BlockEntityType<GravityCrystalBlockEntity> type,
                         BiFunction<BlockPos, BlockState, GravityCrystalBlockEntity> factory) {
        this.blockEntityType = type;
        this.blockEntityFactory = factory;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.RESPAWN_ANCHOR.defaultBlockState()
                .setValue(RespawnAnchorBlock.CHARGE, state.getValue(CHARGE));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.apply(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, blockEntityType, GravityCrystalBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.AMETHYST_SHARD)) return InteractionResult.PASS;
        int charge = state.getValue(CHARGE);
        if (charge >= RespawnAnchorBlock.MAX_CHARGES) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(pos, state.setValue(CHARGE, charge + 1), Block.UPDATE_ALL);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
