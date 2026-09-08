package net.esweld.chaintheblocks.event;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChainTheBlocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);
        ItemStack stack = event.getItemStack();

        if (ChainWrapping.isEmptyChain(clicked) && stack.getItem() instanceof BlockItem) {
            if (!level.isClientSide) {
                ChainWrapping.insertFromItem(level, pos, stack, player, event.getHitVec());
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        if (!stack.is(ModBlocks.CHAIN_BLOCK.get().asItem())) {
            return;
        }
        if (clicked.getBlock() instanceof ChainBlock) {
            return;
        }
        if (ChainWrapping.canWrap(level, pos, clicked)) {
            if (!level.isClientSide && ChainWrapping.wrapBlock(level, pos, player)
                    && !player.getAbilities().instabuild) {
                stack.shrink(1);
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
            if (ChainWrapping.isEmptyChain(beyondState) && ChainWrapping.canWrap(level, inFront, inFrontState)) {
                event.setCanceled(true);
                if (!level.isClientSide && ChainWrapping.moveBlockIntoChain(level, inFront, beyond)) {
                    extendPiston(level, pistonPos, pistonState, dir);
                }
                return;
            }
            if (ChainWrapping.isEmptyChain(inFrontState) && ChainWrapping.canWrap(level, beyond, beyondState)) {
                event.setCanceled(true);
                if (!level.isClientSide && ChainWrapping.wrapBlock(level, beyond, null)) {
                    level.removeBlock(inFront, false);
                    extendPiston(level, pistonPos, pistonState, dir);
                }
            }
            return;
        }

        if (!pistonState.is(Blocks.STICKY_PISTON)
                || !pistonState.hasProperty(PistonBaseBlock.EXTENDED)
                || !pistonState.getValue(PistonBaseBlock.EXTENDED)) {
            return;
        }

        if (ChainWrapping.isFilledChain(beyondState)) {
            event.setCanceled(true);
            if (!level.isClientSide && ChainWrapping.restoreContained(level, beyond)) {
                level.setBlock(pistonPos, pistonState.setValue(PistonBaseBlock.EXTENDED, false), 2);
                level.setBlock(inFront, ChainWrapping.emptyChainState(), 3);
                level.playSound(null, pistonPos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F,
                        level.random.nextFloat() * 0.15F + 0.6F);
            }
            return;
        }

        if (ChainWrapping.isEmptyChain(beyondState)) {
            event.setCanceled(true);
            if (!level.isClientSide) {
                BlockState chain = beyondState;
                level.setBlock(pistonPos, pistonState.setValue(PistonBaseBlock.EXTENDED, false), 2);
                level.setBlock(inFront, chain, 3);
                if (!beyond.equals(inFront)) {
                    level.removeBlock(beyond, false);
                }
                level.playSound(null, pistonPos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F,
                        level.random.nextFloat() * 0.15F + 0.6F);
            }
        }
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
