package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.mixin.common.bridge.ServerWorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractFurnaceBlock.class)
public abstract class AbstractFurnaceBlockMixin extends BaseEntityBlock {
    protected AbstractFurnaceBlockMixin(Properties settings) {
        super(settings);
    }

    /**
     * @author icybear
     * @reason to replace their ticker
     */
    @Nullable
    @Overwrite
    public static <T extends BlockEntity> BlockEntityTicker<T> createFurnaceTicker(
            Level world, BlockEntityType<T> givenType, BlockEntityType<? extends AbstractFurnaceBlockEntity> expectedType
    ) {
        return world instanceof ServerLevel sw ? BaseEntityBlock.createTickerHelper(givenType, expectedType, AbstractFurnaceBlockMixin::tickMultiple) : null;
    }

    @Unique
    private static <E extends AbstractFurnaceBlockEntity> void tickMultiple(Level _world, BlockPos pos, BlockState state, E blockEntity) {
        var world = (ServerLevel) _world;
        AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity);
        int i = world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) world).getSleepStatus().areEnoughSleeping(i)) {
            AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity); //todo: config support?
            AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity);
        }
    }
}
