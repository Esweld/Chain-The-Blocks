package net.esweld.chaintheblocks.block.custom;

import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.level.material.PushReaction;

import javax.annotation.Nullable;
import java.util.List;

public class ChainBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    public ChainBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, LIGHT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(FILLED) ? new ChainBlockEntity(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FILLED) ? PushReaction.BLOCK : PushReaction.PUSH_ONLY;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        CompoundTag tag = BlockItem.getBlockEntityData(context.getItemInHand());
        boolean filled = tag != null && tag.contains("ContainedState");
        int light = 0;
        if (filled) {
            BlockState inner = NbtUtils.readBlockState(
                    context.getLevel().holderLookup(Registries.BLOCK),
                    tag.getCompound("ContainedState"));
            light = Mth.clamp(inner.getLightEmission(), 0, 15);
        }
        return this.defaultBlockState().setValue(FILLED, filled).setValue(LIGHT, light);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FILLED)) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getItem() instanceof BlockItem) {
            if (!level.isClientSide) {
                ChainWrapping.insertFromItem(level, pos, stack, player, hit);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (state.getValue(FILLED) && level.getBlockEntity(pos) instanceof ChainBlockEntity be) {
            be.saveToItem(stack);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide && willHarvest && !player.getAbilities().instabuild) {
            popResource(level, pos, new ItemStack(this));
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        if (!level.isClientSide && !isMoving && newState.isAir()
                && level.getBlockEntity(pos) instanceof ChainBlockEntity be && be.hasContained()) {
            BlockState inner = be.getContainedState();
            CompoundTag tag = be.copyContainedBeTag();
            super.onRemove(state, level, pos, newState, isMoving);
            ChainWrapping.restoreInner(level, pos, inner, tag);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
