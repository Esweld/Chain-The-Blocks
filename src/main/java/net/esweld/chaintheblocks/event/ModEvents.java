package net.esweld.chaintheblocks.event;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

        if (stack.is(ModBlocks.CHAIN_BLOCK.get().asItem())) {
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
            return;
        }

//        if (ChainWrapping.isEmptyChain(clicked) && stack.getItem() instanceof BlockItem) {
//            if (!level.isClientSide) {
//                ChainWrapping.insertFromItem(level, pos, stack, player, event.getHitVec());
//            }
//            event.setCanceled(true);
//            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
//        }
    }
}