package net.esweld.chaintheblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChainBlockRenderer implements BlockEntityRenderer<ChainBlockEntity> {
    private final BlockEntityRenderDispatcher dispatcher;

    public ChainBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(ChainBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        if (!be.needsEntityRenderer()) {
            return;
        }
        BlockState inner = be.getContainedState();
        Level level = be.getLevel();
        if (inner == null || level == null || !(inner.getBlock() instanceof EntityBlock entityBlock)) {
            return;
        }

        BlockEntity dummy = entityBlock.newBlockEntity(be.getBlockPos(), inner);
        if (dummy == null) {
            return;
        }
        dummy.setLevel(level);
        CompoundTag tag = be.copyContainedBeTag();
        if (tag != null) {
            dummy.load(tag);
        }

        BlockEntityRenderer<BlockEntity> innerRenderer = dispatcher.getRenderer(dummy);
        if (innerRenderer == null) {
            return;
        }

        pose.pushPose();
        float scale = 0.875F;
        float offset = (1.0F - scale) / 2.0F;
        pose.translate(offset, offset, offset);
        pose.scale(scale, scale, scale);
        innerRenderer.render(dummy, partialTick, pose, buffer, light, overlay);
        pose.popPose();
    }
}
