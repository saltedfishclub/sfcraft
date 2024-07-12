package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.mixin.server.bridge.ServerWorldBridge;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractFurnaceBlock.class)
public abstract class AbstractFurnaceBlockMixin extends BlockWithEntity {
    protected AbstractFurnaceBlockMixin(Settings settings) {
        super(settings);
    }

    /**
     * @author icybear
     * @reason to replace their ticker
     */
    @Overwrite
    public static <T extends BlockEntity> @Nullable BlockEntityTicker<T> validateTicker(
            World world, BlockEntityType<T> givenType, BlockEntityType<? extends AbstractFurnaceBlockEntity> expectedType
    ) {
        return world.isClient ? null : BlockWithEntity.validateTicker(givenType, expectedType, AbstractFurnaceBlockMixin::tickMultiple);
    }

    @Unique
    private static void tickMultiple(World world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);
        int i = world.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) world).getSleepManager().canSkipNight(i)) {
            AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity); //todo: config support?
            AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);
        }
    }
}
