package sfcraft.blocks;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import sfcraft.SFBlockEntities;
import sfcraft.cauldron.CauldronRecipe;
import sfcraft.cauldron.CauldronRecipes;

import java.util.ArrayList;
import java.util.List;

public class AmethystCauldronBlockEntity extends BlockEntity {
    public static final int MAX_ITEMS = 4;

    private final List<ItemStack> contents = new ArrayList<>();
    @Nullable
    private CauldronRecipe activeRecipe;
    private int reactionTicksLeft;

    public AmethystCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(SFBlockEntities.AMETHYST_CAULDRON, pos, state);
    }

    public List<ItemStack> getContents() {
        return contents;
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
        cauldron.absorbItems(serverLevel, pos);
        if (cauldron.activeRecipe != null) {
            serverLevel.sendParticles(cauldron.activeRecipe.reactionParticle(),
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    3, 0.25, 0.15, 0.25, 0.0);
            if (--cauldron.reactionTicksLeft <= 0) {
                cauldron.finishReaction(serverLevel, pos, state);
            }
        }
    }

    private void absorbItems(ServerLevel level, BlockPos pos) {
        if (activeRecipe != null || contents.size() >= MAX_ITEMS) return;
        for (var itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos))) {
            if (contents.size() >= MAX_ITEMS) break;
            var stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;
            contents.add(stack.copy());
            itemEntity.discard();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.7F);
            setChanged();
        }
    }

    public InteractionResult tryStartReaction(ItemStack catalyst, Player player) {
        if (level == null || catalyst.isEmpty() || !CauldronRecipes.isCatalyst(catalyst)) {
            return InteractionResult.PASS;
        }
        if (activeRecipe != null) {
            sendHint(player, "反应正在进行中");
            return InteractionResult.FAIL;
        }
        var match = CauldronRecipes.match(contents, catalyst);
        if (match.isEmpty()) {
            sendHint(player, "锅内的原料不满足反应条件");
            return InteractionResult.FAIL;
        }
        var recipe = match.get();
        if (recipe.needsWater() && !getBlockState().getValue(AmethystCauldronBlock.HAS_WATER)) {
            sendHint(player, "反应需要锅内有水");
            return InteractionResult.FAIL;
        }
        if (recipe.needsHeat() && !isHeated(level, getBlockPos())) {
            sendHint(player, "反应需要在锅下方加热(火焰/岩浆块/熔岩)");
            return InteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials()) {
            catalyst.shrink(1);
        }
        activeRecipe = recipe;
        reactionTicksLeft = recipe.reactionTicks();
        level.playSound(null, getBlockPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
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

    public static boolean isHeated(Level level, BlockPos pos) {
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

    private static void sendHint(Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(message));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        var list = out.list("items", ItemStack.CODEC);
        contents.forEach(list::add);
        out.putInt("reaction_ticks", reactionTicksLeft);
        out.putInt("recipe", activeRecipe == null ? -1 : CauldronRecipes.indexOf(activeRecipe));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        contents.clear();
        in.listOrEmpty("items", ItemStack.CODEC).forEach(contents::add);
        reactionTicksLeft = in.getIntOr("reaction_ticks", 0);
        activeRecipe = CauldronRecipes.byIndex(in.getIntOr("recipe", -1));
    }
}
