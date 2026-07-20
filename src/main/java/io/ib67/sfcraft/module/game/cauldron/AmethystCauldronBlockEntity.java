package io.ib67.sfcraft.module.game.cauldron;

import io.ib67.sfcraft.config.GameConfigService;
import io.ib67.sfcraft.registry.CauldronRecipeRegistry;
import io.ib67.sfcraft.registry.cauldron.CauldronRecipe;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AmethystCauldronBlockEntity extends BlockEntity {
    @Getter
    private final List<ItemStack> contents = new ArrayList<>();
    private final CauldronRecipeRegistry recipes;
    private final GameConfigService config;
    @Nullable
    private CauldronRecipe activeRecipe;
    private int reactionTicksLeft;

    public AmethystCauldronBlockEntity(BlockEntityType<AmethystCauldronBlockEntity> type,
                                       CauldronRecipeRegistry recipes,
                                       GameConfigService config,
                                       BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.recipes = recipes;
        this.config = config;
    }

    public boolean isReacting() {
        return activeRecipe != null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AmethystCauldronBlockEntity cauldron) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long time = level.getGameTime();
        if (time % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.FIREFLY,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0.35, 0.25, 0.35, 0.02);
        }
        if (time % 4 == 0) {
            cauldron.absorbItems(serverLevel, pos);
        }
        if (cauldron.activeRecipe != null) {
            serverLevel.sendParticles(cauldron.activeRecipe.reactionParticle(),
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    3, 0.25, 0.15, 0.25, 0.0);
            if (--cauldron.reactionTicksLeft <= 0) {
                cauldron.finishReaction(serverLevel, pos, state);
            }
        } else {
            if (time % 4 == 0) cauldron.tryAutoStart(serverLevel, pos, state);
        }
    }

    private void absorbItems(ServerLevel level, BlockPos pos) {
        int maxItems = config.get().cauldron.maxItems;
        if (activeRecipe != null || contents.size() >= maxItems) return;
        for (var itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos))) {
            if (contents.size() >= maxItems) break;
            var stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;
            contents.add(stack.copy());
            itemEntity.discard();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.7F);
            setChanged();
        }
    }

    /**
     * 每 tick 检查: 锅内物品凑齐某配方且满足水/加热条件时自动开始反应。
     * 凑齐但缺条件时冒烟提示"就差水或加热"。
     */
    private void tryAutoStart(ServerLevel level, BlockPos pos, BlockState state) {
        if (contents.isEmpty()) return;
        var match = recipes.match(contents);
        if (match.isEmpty()) return;
        var recipe = match.get();
        boolean ready = (!recipe.needsWater() || state.getValue(AmethystCauldronBlock.HAS_WATER))
                && (!recipe.needsHeat() || isHeated(level, pos));
        if (!ready) {
            if (level.getGameTime() % 16 == 0) {
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        2, 0.2, 0.1, 0.2, 0.01);
            }
            return;
        }
        activeRecipe = recipe;
        reactionTicksLeft = recipe.reactionTicks();
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        setChanged();
    }

    /** 空手右键: 取回最后放入的一个原料(后进先出);反应进行中禁止取出。 */
    public InteractionResult retrieveLast(Player player) {
        if (level == null) return InteractionResult.PASS;
        if (activeRecipe != null) {
            sendHint(player, Component.translatable("message.sfcraft.cauldron.reacting"));
            return InteractionResult.FAIL;
        }
        if (contents.isEmpty()) return InteractionResult.PASS;
        var stack = contents.removeLast();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
        setChanged();
        return InteractionResult.SUCCESS_SERVER;
    }

    private void finishReaction(ServerLevel level, BlockPos pos, BlockState state) {
        var recipe = activeRecipe;
        activeRecipe = null;
        reactionTicksLeft = 0;
        if (recipe == null) return;
        var result = recipe.assembler().apply(contents);
        contents.clear();
        if (!result.isEmpty()) {
            Block.popResource(level, pos.above(), result);
        }
        if (recipe.needsWater()) {
            level.setBlock(pos, state.setValue(AmethystCauldronBlock.HAS_WATER, false), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    private static boolean isHeated(Level level, BlockPos pos) {
        var below = pos.below();
        var belowState = level.getBlockState(below);
        return belowState.is(Blocks.FIRE)
                || belowState.is(Blocks.SOUL_FIRE)
                || belowState.is(Blocks.MAGMA_BLOCK)
                || level.getFluidState(below).is(FluidTags.LAVA);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (var stack : contents) {
                Block.popResource(level, pos.above(), stack);
            }
            contents.clear();
        }
        super.preRemoveSideEffects(pos, state);
    }

    private static void sendHint(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(message);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        var list = out.list("items", ItemStack.CODEC);
        contents.forEach(list::add);
        out.putInt("reaction_ticks", reactionTicksLeft);
        out.putString("recipe", activeRecipe == null ? "" : activeRecipe.id());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        contents.clear();
        in.listOrEmpty("items", ItemStack.CODEC).forEach(contents::add);
        reactionTicksLeft = in.getIntOr("reaction_ticks", 0);
        activeRecipe = recipes.byId(in.getStringOr("recipe", ""));
    }
}
