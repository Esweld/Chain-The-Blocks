package net.esweld.chaintheblocks.item;

import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ChainBlockItem extends BlockItem {
    public ChainBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static BlockHitResult clipSources(Player player) {
        return getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.SOURCE_ONLY);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        net.esweld.chaintheblocks.client.ChainItemClient.register(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = getBlockEntityData(stack);
        if (tag != null && tag.contains("ContainedState")) {
            BlockState inner = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("ContainedState"));
            tooltip.add(inner.getBlock().getName().withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && ChainWrapping.isFilledChainItem(context.getItemInHand())) {
            BlockHitResult fluid = clipSources(player);
            if (fluid.getType() == HitResult.Type.BLOCK) {
                BlockState at = context.getLevel().getBlockState(fluid.getBlockPos());
                if (at.getFluidState().isSource() || at.canBeReplaced()) {
                    return this.place(new BlockPlaceContext(player, context.getHand(), context.getItemInHand(), fluid));
                }
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = clipSources(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockState clicked = level.getBlockState(hit.getBlockPos());

        if (!ChainWrapping.isFilledChainItem(stack) && player.isShiftKeyDown()
                && ChainWrapping.canWrap(level, hit.getBlockPos(), clicked)) {
            if (!level.isClientSide && ChainWrapping.wrapBlock(level, hit.getBlockPos(), player)
                    && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        BlockPlaceContext placeCtx = new BlockPlaceContext(player, hand, stack, hit);
        InteractionResult result = this.place(placeCtx);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }
}