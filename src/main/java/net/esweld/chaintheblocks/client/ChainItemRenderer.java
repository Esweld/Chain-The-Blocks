package net.esweld.chaintheblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.client.model.data.ModelData;

public class ChainItemRenderer extends BlockEntityWithoutLevelRenderer {
    public ChainItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        BlockState inner = readInner(stack);
        if (inner != null && !inner.isAir()) {
            pose.pushPose();
            pose.translate(0.002F, 0.002F, 0.002F);
            pose.scale(0.996F, 0.996F, 0.996F);
            if (inner.getRenderShape() == RenderShape.MODEL) {
                Minecraft.getInstance().getBlockRenderer()
                        .renderSingleBlock(inner, pose, buffer, light, overlay, ModelData.EMPTY, null);
            } else if (inner.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED
                    && inner.getBlock() instanceof EntityBlock entityBlock) {
                renderInnerEntity(inner, pose, buffer, light, overlay, entityBlock);
            }
            pose.popPose();
        }

        BlockState cage = ModBlocks.CHAIN_BLOCK.get().defaultBlockState();
        Minecraft.getInstance().getBlockRenderer()
                .renderSingleBlock(cage, pose, buffer, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    private static void renderInnerEntity(BlockState inner, PoseStack pose, MultiBufferSource buffer,
                                          int light, int overlay, EntityBlock entityBlock) {
        BlockEntity dummy = entityBlock.newBlockEntity(BlockPos.ZERO, inner);
        if (dummy == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            dummy.setLevel(mc.level);
        }
        BlockEntityRenderer<BlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(dummy);
        if (renderer != null) {
            renderer.render(dummy, 0.0F, pose, buffer, light, overlay);
        }
    }

    private static BlockState readInner(ItemStack stack) {
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        if (tag == null || !tag.contains("ContainedState")) {
            return null;
        }
        var lookup = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.holderLookup(Registries.BLOCK)
                : BuiltInRegistries.BLOCK.asLookup();
        return stripOrientation(NbtUtils.readBlockState(lookup, tag.getCompound("ContainedState")));
    }

    private static BlockState stripOrientation(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING,
                    state.getBlock().defaultBlockState().getValue(BlockStateProperties.FACING));
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X);
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            state = state.setValue(BlockStateProperties.ROTATION_16, 0);
        }
        if (state.hasProperty(BlockStateProperties.ORIENTATION)) {
            state = state.setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);
        }
        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL);
        }
        if (state.hasProperty(BlockStateProperties.STAIRS_SHAPE)) {
            state = state.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
        }
        if (state.hasProperty(BlockStateProperties.HALF)) {
            state = state.setValue(BlockStateProperties.HALF, Half.BOTTOM);
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP) {
            state = state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        }
        return state;
    }
}