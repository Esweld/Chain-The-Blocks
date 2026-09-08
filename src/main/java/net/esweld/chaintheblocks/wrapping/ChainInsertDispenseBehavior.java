package net.esweld.chaintheblocks.wrapping;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ChainInsertDispenseBehavior implements DispenseItemBehavior {
    private final DispenseItemBehavior fallback;

    public ChainInsertDispenseBehavior(DispenseItemBehavior fallback) {
        this.fallback = fallback == null ? new DefaultDispenseItemBehavior() : fallback;
    }

    @Override
    public ItemStack dispense(BlockSource source, ItemStack stack) {
        Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
        Level level = source.getLevel();
        BlockPos target = source.getPos().relative(facing);

        if (stack.getItem() instanceof BlockItem && ChainWrapping.isOurChain(level.getBlockState(target))) {
            if (ChainWrapping.isEmptyChain(level.getBlockState(target))) {
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(target), facing.getOpposite(), target, false);
                if (ChainWrapping.insertFromItem(level, target, stack, null, hit)) {
                    level.levelEvent(1000, source.getPos(), 0);
                    level.levelEvent(2000, source.getPos(), facing.get3DDataValue());
                    return stack;
                }
            }
            level.levelEvent(1001, source.getPos(), 0);
            return stack;
        }
        return fallback.dispense(source, stack);
    }
}
