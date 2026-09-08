package net.esweld.chaintheblocks.block.custom;

import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ChainBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    public ChainBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChainBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.FAIL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
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
