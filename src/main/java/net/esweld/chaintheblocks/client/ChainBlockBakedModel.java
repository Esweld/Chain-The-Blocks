package net.esweld.chaintheblocks.client;

import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChainBlockBakedModel implements BakedModel {
    private final BakedModel chain;

    public ChainBlockBakedModel(BakedModel chain) {
        this.chain = chain;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return chain.getQuads(state, side, rand);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType type) {
        List<BakedQuad> quads = new ArrayList<>();
        BlockState inner = data.get(ChainBlockEntity.INNER);
        if (inner != null && !inner.isAir() && inner.getRenderShape() == RenderShape.MODEL) {
            BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(inner);
            ChunkRenderTypeSet innerTypes = innerModel.getRenderTypes(inner, rand, ModelData.EMPTY);
            if (type == null || innerTypes.contains(type)) {
                quads.addAll(innerModel.getQuads(inner, side, rand, ModelData.EMPTY, type));
            }
        }
        if (type == null || type == RenderType.cutout()) {
            quads.addAll(chain.getQuads(state, side, rand, ModelData.EMPTY, type));
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState inner = data.get(ChainBlockEntity.INNER);
        ChunkRenderTypeSet chainTypes = ChunkRenderTypeSet.of(RenderType.cutout());
        if (inner != null && !inner.isAir() && inner.getRenderShape() == RenderShape.MODEL) {
            BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(inner);
            return ChunkRenderTypeSet.union(innerModel.getRenderTypes(inner, rand, ModelData.EMPTY), chainTypes);
        }
        return chainTypes;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return chain.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return chain.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return chain.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return chain.getOverrides();
    }
}
