package net.esweld.chaintheblocks.wrapping;

import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class ChainWrapping {
    private ChainWrapping() {
    }

    public static boolean isOurChain(BlockState state) {
        return state.getBlock() instanceof ChainBlock && state.hasProperty(ChainBlock.FILLED);
    }

    public static boolean isEmptyChain(BlockState state) {
        return isOurChain(state) && !state.getValue(ChainBlock.FILLED);
    }

    public static boolean isFilledChain(BlockState state) {
        return isOurChain(state) && state.getValue(ChainBlock.FILLED);
    }

    public static boolean isTooLarge(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return true;
        }
        if (state.getBlock() instanceof PistonHeadBlock || state.getBlock() instanceof MovingPistonBlock) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.EXTENDED)
                && Boolean.TRUE.equals(state.getValue(BlockStateProperties.EXTENDED));
    }

    public static boolean canWrap(Level level, BlockPos pos, BlockState state) {
        if (!(ModBlocks.CHAIN_BLOCK.get() instanceof ChainBlock)) {
            return false;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (isOurChain(state)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        if (isTooLarge(state)) {
            return false;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
                || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.BARRIER)
                || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.MOVING_PISTON)) {
            return false;
        }
        return true;
    }

    public static boolean wrapBlock(Level level, BlockPos pos, @Nullable Player player) {
        BlockState target = level.getBlockState(pos);
        if (!canWrap(level, pos, target)) {
            return false;
        }
        CompoundTag tag = snapshotBe(level, pos);
        try {
            unpairChest(level, pos, target);
            clearContainerSoItDoesNotSpill(level, pos);
            BlockState chain = filledChainState();
            if (!level.setBlock(pos, chain, Block.UPDATE_ALL)) {
                restoreInner(level, pos, target, tag);
                return false;
            }
            if (level.getBlockEntity(pos) instanceof ChainBlockEntity chainBe) {
                chainBe.setContained(normalizeStoredState(target), tag);
                level.getLightEngine().checkBlock(pos);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                return true;
            }
            restoreInner(level, pos, target, tag);
            return false;
        } catch (RuntimeException ex) {
            restoreInner(level, pos, target, tag);
            throw ex;
        }
    }

    public static boolean moveBlockIntoChain(Level level, BlockPos source, BlockPos chainPos) {
        BlockState sourceState = level.getBlockState(source);
        if (!isEmptyChain(level.getBlockState(chainPos)) || !canWrap(level, source, sourceState)) {
            return false;
        }
        CompoundTag tag = snapshotBe(level, source);
        unpairChest(level, source, sourceState);
        clearContainerSoItDoesNotSpill(level, source);
        level.removeBlock(source, false);
        if (!fillChain(level, chainPos, normalizeStoredState(sourceState), tag)) {
            restoreInner(level, source, sourceState, tag);
            return false;
        }
        return true;
    }

    public static boolean insertFromItem(Level level, BlockPos chainPos, ItemStack stack,
                                         @Nullable Player player, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || hit == null) {
            return false;
        }
        if (!isEmptyChain(level.getBlockState(chainPos))) {
            return false;
        }
        BlockState toPlace;
        try {
            BlockPlaceContext ctx = new BlockPlaceContext(level, player,
                    player != null ? player.getUsedItemHand() : InteractionHand.MAIN_HAND, stack, hit);
            toPlace = blockItem.getBlock().getStateForPlacement(ctx);
        } catch (RuntimeException ex) {
            toPlace = null;
        }
        if (toPlace == null) {
            toPlace = blockItem.getBlock().defaultBlockState();
        }
        if (toPlace.isAir() || isTooLarge(toPlace) || isOurChain(toPlace)) {
            return false;
        }
        CompoundTag beTag = BlockItem.getBlockEntityData(stack);
        if (!fillChain(level, chainPos, normalizeStoredState(toPlace), beTag == null ? null : beTag.copy())) {
            return false;
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static BlockState emptyChainState() {
        return ModBlocks.CHAIN_BLOCK.get().defaultBlockState().setValue(ChainBlock.FILLED, false);
    }

    public static boolean restoreContained(Level level, BlockPos filledPos) {
        if (!(level.getBlockEntity(filledPos) instanceof ChainBlockEntity be) || !be.hasContained()) {
            return false;
        }
        BlockState inner = be.getContainedState();
        CompoundTag tag = be.copyContainedBeTag();
        restoreInner(level, filledPos, inner, tag);
        level.playSound(null, filledPos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static void restoreInner(Level level, BlockPos pos, BlockState inner, @Nullable CompoundTag tag) {
        if (inner == null) {
            return;
        }
        level.setBlock(pos, inner, Block.UPDATE_ALL);
        if (tag != null) {
            BlockEntity innerBe = level.getBlockEntity(pos);
            if (innerBe != null) {
                innerBe.load(tag);
                innerBe.setChanged();
            }
        }
        level.getLightEngine().checkBlock(pos);
    }

    private static boolean fillChain(Level level, BlockPos chainPos, BlockState stored, @Nullable CompoundTag tag) {
        if (!isEmptyChain(level.getBlockState(chainPos))) {
            return false;
        }
        if (!level.setBlock(chainPos, filledChainState(), Block.UPDATE_ALL)) {
            return false;
        }
        if (level.getBlockEntity(chainPos) instanceof ChainBlockEntity chainBe) {
            chainBe.setContained(stored, tag);
            level.getLightEngine().checkBlock(chainPos);
            level.playSound(null, chainPos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    private static BlockState filledChainState() {
        return ModBlocks.CHAIN_BLOCK.get().defaultBlockState().setValue(ChainBlock.FILLED, true);
    }

    @Nullable
    private static CompoundTag snapshotBe(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.saveWithoutMetadata();
    }

    private static void clearContainerSoItDoesNotSpill(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            container.clearContent();
        }
    }

    private static void unpairChest(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return;
        }
        Direction connected = ChestBlock.getConnectedDirection(state);
        BlockPos otherPos = pos.relative(connected);
        BlockState other = level.getBlockState(otherPos);
        if (other.hasProperty(ChestBlock.TYPE)) {
            level.setBlock(otherPos, other.setValue(ChestBlock.TYPE, ChestType.SINGLE), Block.UPDATE_ALL);
        }
    }

    public static BlockState normalizeStoredState(BlockState state) {
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        return state;
    }
}