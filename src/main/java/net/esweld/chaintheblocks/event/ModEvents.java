package net.esweld.chaintheblocks.event;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.esweld.chaintheblocks.item.ChainBlockItem;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChainTheBlocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (ChainWrapping.isFilledChainItem(stack)) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        ItemStack held = stack;

        if (held.is(ModBlocks.CHAIN_BLOCK.get().asItem())) {
            BlockHitResult fluidHit = ChainBlockItem.clipSources(player);
            if (fluidHit.getType() == HitResult.Type.BLOCK) {
                BlockPos fluidPos = fluidHit.getBlockPos();
                BlockState fluidState = level.getBlockState(fluidPos);
                if (fluidState.getFluidState().isSource() && ChainWrapping.canWrap(level, fluidPos, fluidState)) {
                    if (!level.isClientSide && ChainWrapping.wrapBlock(level, fluidPos, player)
                            && !player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                    return;
                }
            }
        }

        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);

        if (ChainWrapping.isEmptyChain(clicked)
                && (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem)) {
            if (!level.isClientSide) {
                ChainWrapping.insertFromItem(level, pos, held, player, event.getHitVec());
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        if (!held.is(ModBlocks.CHAIN_BLOCK.get().asItem())) {
            return;
        }
        if (clicked.getBlock() instanceof ChainBlock) {
            return;
        }
        if (ChainWrapping.canWrap(level, pos, clicked)) {
            if (!level.isClientSide && ChainWrapping.wrapBlock(level, pos, player)
                    && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        } else if (ChainWrapping.isTooLarge(clicked)) {
            if (level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("block.chaintheblocks.chain_block.unwrappable"), true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        Direction dir = event.getDirection();
        BlockPos pistonPos = event.getPos();
        BlockPos inFront = pistonPos.relative(dir);
        BlockPos beyond = inFront.relative(dir);
        BlockState inFrontState = level.getBlockState(inFront);
        BlockState beyondState = level.getBlockState(beyond);
        BlockState pistonState = level.getBlockState(pistonPos);

        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
            handleDirectExtend(event, level, pistonPos, pistonState, dir, inFront, inFrontState, beyond, beyondState);
            return;
        }

        if (!pistonState.is(Blocks.STICKY_PISTON)
                || !pistonState.hasProperty(PistonBaseBlock.EXTENDED)
                || !pistonState.getValue(PistonBaseBlock.EXTENDED)) {
            return;
        }

        handleDirectRetract(event, level, pistonPos, pistonState, dir, inFront, beyond, beyondState);
    }

    private static void handleDirectExtend(PistonEvent.Pre event, Level level, BlockPos pistonPos,
                                           BlockState pistonState, Direction dir, BlockPos inFront,
                                           BlockState inFrontState, BlockPos beyond, BlockState beyondState) {
        if (ChainWrapping.isEmptyChain(inFrontState)) {
            event.setCanceled(true);
            if (level.isClientSide) {
                return;
            }
            if (ChainWrapping.isOurChain(beyondState)) {
                return;
            }
            if (ChainWrapping.canWrap(level, beyond, beyondState)) {
                if (ChainWrapping.wrapBlock(level, beyond, null)) {
                    level.removeBlock(inFront, false);
                    extendPiston(level, pistonPos, pistonState, dir);
                }
                return;
            }
            if (beyondState.isAir() || beyondState.canBeReplaced()) {
                level.removeBlock(inFront, false);
                level.setBlock(beyond, ChainWrapping.emptyChainState(), 3);
                extendPiston(level, pistonPos, pistonState, dir);
            }
            return;
        }

        if (ChainWrapping.isEmptyChain(beyondState)
                && !ChainWrapping.isOurChain(inFrontState)
                && ChainWrapping.canWrap(level, inFront, inFrontState)) {
            event.setCanceled(true);
            if (!level.isClientSide && ChainWrapping.moveBlockIntoChain(level, inFront, beyond)) {
                extendPiston(level, pistonPos, pistonState, dir);
            }
            return;
        }

        if (structureHasChain(event, level)) {
            event.setCanceled(true);
        }
    }

    private static void handleDirectRetract(PistonEvent.Pre event, Level level, BlockPos pistonPos,
                                            BlockState pistonState, Direction dir, BlockPos inFront,
                                            BlockPos beyond, BlockState beyondState) {
        if (ChainWrapping.isEmptyChain(beyondState)) {
            event.setCanceled(true);
            if (!level.isClientSide) {
                retractPiston(level, pistonPos, pistonState);
                level.setBlock(inFront, ChainWrapping.emptyChainState(), 3);
                if (!beyond.equals(inFront)) {
                    level.removeBlock(beyond, false);
                }
            }
            return;
        }

        if (!ChainWrapping.isFilledChain(beyondState)
                || !(level.getBlockEntity(beyond) instanceof ChainBlockEntity be)
                || !be.hasContained()) {
            return;
        }

        event.setCanceled(true);
        if (level.isClientSide) {
            return;
        }
        BlockState inner = be.getContainedState();
        CompoundTag tag = be.copyContainedBeTag();
        retractPiston(level, pistonPos, pistonState);
        level.setBlock(inFront, ChainWrapping.emptyChainState(), 3);
        ChainWrapping.restoreInner(level, beyond, inner, tag, false);
    }

    private static boolean structureHasChain(PistonEvent.Pre event, Level level) {
        var helper = event.getStructureHelper();
        if (helper == null || !helper.resolve()) {
            return false;
        }
        for (BlockPos pos : helper.getToPush()) {
            if (ChainWrapping.isOurChain(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static void retractPiston(Level level, BlockPos pistonPos, BlockState pistonState) {
        level.setBlock(pistonPos, pistonState.setValue(PistonBaseBlock.EXTENDED, false), 2);
        BlockPos headPos = pistonPos.relative(pistonState.getValue(PistonBaseBlock.FACING));
        if (level.getBlockState(headPos).is(Blocks.PISTON_HEAD)) {
            level.removeBlock(headPos, false);
        }
        level.playSound(null, pistonPos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.15F + 0.6F);
    }

    private static void extendPiston(Level level, BlockPos pistonPos, BlockState pistonState, Direction dir) {
        boolean sticky = pistonState.is(Blocks.STICKY_PISTON);
        level.setBlock(pistonPos, pistonState.setValue(PistonBaseBlock.EXTENDED, true), 3);
        BlockState head = Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.FACING, dir)
                .setValue(PistonHeadBlock.TYPE, sticky ? PistonType.STICKY : PistonType.DEFAULT);
        level.setBlock(pistonPos.relative(dir), head, 3);
        level.playSound(null, pistonPos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.25F + 0.6F);
    }
}