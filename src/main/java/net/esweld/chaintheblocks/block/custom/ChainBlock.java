package net.esweld.chaintheblocks.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.extensions.IForgeBlock;

public class ChainBlock extends Block {
    public ChainBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel,
                                 BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);

    }
}
