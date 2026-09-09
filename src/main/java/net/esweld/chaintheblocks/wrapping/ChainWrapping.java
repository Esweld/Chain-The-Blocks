package net.esweld.chaintheblocks.wrapping;

import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.material.Fluids;
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

    public static boolean isFilledChainItem(ItemStack stack) {
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        return tag != null && tag.contains("ContainedState");
    }

    public static boolean isTooLarge(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART)) {
            return true;
        }
        if (state.getBlock() instanceof PistonHeadBlock || state.getBlock() instanceof MovingPistonBlock) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.EXTENDED)
                && Boolean.TRUE.equals(state.getValue(BlockStateProperties.EXTENDED));
    }

    public static boolean canWrap(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || isOurChain(state) || isTooLarge(state)) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return !state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE)
                && !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_PORTAL)
                && !state.is(Blocks.END_GATEWAY) && !state.is(Blocks.END_PORTAL_FRAME)
                && !state.is(Blocks.BARRIER) && !state.is(Blocks.STRUCTURE_VOID)
                && !state.is(Blocks.MOVING_PISTON);
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
            suppressFurnaceXp(level, pos);
            BlockState stored = normalizeStoredState(target);
            if (!level.setBlock(pos, filledChainState(stored), Block.UPDATE_ALL)) {
                restoreInner(level, pos, target, tag, true);
                return false;
            }
            if (level.getBlockEntity(pos) instanceof ChainBlockEntity chainBe) {
                chainBe.setContained(stored, tag);
                level.getLightEngine().checkBlock(pos);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                return true;
            }
            restoreInner(level, pos, target, tag, true);
            return false;
        } catch (RuntimeException ex) {
            restoreInner(level, pos, target, tag, true);
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
        suppressFurnaceXp(level, source);
        level.removeBlock(source, false);
        if (!fillChain(level, chainPos, normalizeStoredState(sourceState), tag)) {
            restoreInner(level, source, sourceState, tag, true);
            return false;
        }
        return true;
    }

    public static boolean insertFromItem(Level level, BlockPos chainPos, ItemStack stack,
                                         @Nullable Player player, BlockHitResult hit) {
        if (!isEmptyChain(level.getBlockState(chainPos))) {
            return false;
        }

        BlockState toPlace;
        CompoundTag beTag = null;
        boolean bucket = false;

        if (stack.getItem() instanceof BucketItem bucketItem) {
            var fluid = bucketItem.getFluid();
            if (fluid == Fluids.WATER) {
                toPlace = Blocks.WATER.defaultBlockState();
                bucket = true;
            } else if (fluid == Fluids.LAVA) {
                toPlace = Blocks.LAVA.defaultBlockState();
                bucket = true;
            } else {
                return false;
            }
        } else if (stack.getItem() instanceof BlockItem blockItem && hit != null) {
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
            beTag = BlockItem.getBlockEntityData(stack);
        } else {
            return false;
        }

        if (toPlace.isAir() || isTooLarge(toPlace) || isOurChain(toPlace)) {
            return false;
        }
        if (toPlace.getBlock() instanceof LiquidBlock && !toPlace.getFluidState().isSource()) {
            return false;
        }
        if (!fillChain(level, chainPos, normalizeStoredState(toPlace), beTag == null ? null : beTag.copy())) {
            return false;
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (bucket && player != null && stack.isEmpty()) {
                player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.BUCKET));
            }
        }
        return true;
    }

    public static BlockState emptyChainState() {
        return ModBlocks.CHAIN_BLOCK.get().defaultBlockState()
                .setValue(ChainBlock.FILLED, false)
                .setValue(ChainBlock.LIGHT, 0);
    }

    public static void restoreInner(Level level, BlockPos pos, BlockState inner, @Nullable CompoundTag tag) {
        restoreInner(level, pos, inner, tag, true);
    }

    public static void restoreInner(Level level, BlockPos pos, BlockState inner, @Nullable CompoundTag tag,
                                    boolean neighbors) {
        if (inner == null) {
            return;
        }
        inner = resetSculk(inner);
        if (inner.getBlock() instanceof SculkSensorBlock) {
            tag = null;
        }
        int flags = neighbors ? Block.UPDATE_ALL : (Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(pos, inner, flags);
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
        if (!level.setBlock(chainPos, filledChainState(stored), Block.UPDATE_ALL)) {
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

    private static BlockState filledChainState(BlockState inner) {
        int light = inner == null ? 0 : Mth.clamp(inner.getLightEmission(), 0, 15);
        return ModBlocks.CHAIN_BLOCK.get().defaultBlockState()
                .setValue(ChainBlock.FILLED, true)
                .setValue(ChainBlock.LIGHT, light);
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

    private static void suppressFurnaceXp(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractFurnaceBlockEntity furnace)) {
            return;
        }
        for (var field : AbstractFurnaceBlockEntity.class.getDeclaredFields()) {
            if (!java.util.Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(furnace);
                if (value instanceof java.util.Map<?, ?> map) {
                    map.clear();
                }
            } catch (IllegalAccessException ignored) {
            }
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
            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        return resetSculk(state);
    }

    private static BlockState resetSculk(BlockState state) {
        if (state.hasProperty(BlockStateProperties.SCULK_SENSOR_PHASE)) {
            return state.setValue(BlockStateProperties.SCULK_SENSOR_PHASE, SculkSensorPhase.INACTIVE);
        }
        return state;
    }
}
