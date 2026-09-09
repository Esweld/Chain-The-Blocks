package net.esweld.chaintheblocks.client;

import net.esweld.chaintheblocks.blockentity.ChainBlockEntity;
import net.esweld.chaintheblocks.wrapping.ChainWrapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChainBlockBakedModel implements BakedModel {
    private static final float INNER_SCALE = 0.996F;
    private static final float INNER_SHIFT = (1.0F - INNER_SCALE) / 2.0F;

    private static final ItemOverrides OVERRIDES = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel original, ItemStack stack,
                                  @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (original instanceof ChainBlockBakedModel base && ChainWrapping.isFilledChainItem(stack)) {
                return new ChainBlockBakedModel(base.chain, true);
            }
            return original;
        }
    };

    private final BakedModel chain;
    private final boolean filledItem;

    public ChainBlockBakedModel(BakedModel chain) {
        this(chain, false);
    }

    private ChainBlockBakedModel(BakedModel chain, boolean filledItem) {
        this.chain = chain;
        this.filledItem = filledItem;
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
        BlockPos pos = data.get(ChainBlockEntity.POS);
        if (inner != null && !inner.isAir()) {
            if (inner.getRenderShape() == RenderShape.MODEL) {
                BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(inner);
                ChunkRenderTypeSet innerTypes = innerModel.getRenderTypes(inner, rand, ModelData.EMPTY);
                if (type == null || innerTypes.contains(type)) {
                    for (BakedQuad quad : innerModel.getQuads(inner, side, rand, ModelData.EMPTY, type)) {
                        quads.add(adjustInnerQuad(quad, inner, pos));
                    }
                }
            }
            if (!inner.getFluidState().isEmpty() && (type == null || type == RenderType.translucent())) {
                addFluidQuads(quads, inner, pos, side);
            }
        }
        if (type == null || type == RenderType.cutout()) {
            quads.addAll(chain.getQuads(state, side, rand, ModelData.EMPTY, type));
        }
        return quads;
    }

    private static void addFluidQuads(List<BakedQuad> quads, BlockState inner, @Nullable BlockPos pos,
                                      @Nullable Direction side) {
        FluidState fluid = inner.getFluidState();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        Level level = Minecraft.getInstance().level;
        ResourceLocation tex = (level != null && pos != null)
                ? ext.getStillTexture(fluid, level, pos) : ext.getStillTexture();
        int color = (level != null && pos != null)
                ? ext.getTintColor(fluid, level, pos) : ext.getTintColor();
        if (tex == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
        if (side != null) {
            quads.add(fluidFace(side, sprite, color));
        } else {
            for (Direction dir : Direction.values()) {
                quads.add(fluidFace(dir, sprite, color));
            }
        }
    }

    private static BakedQuad fluidFace(Direction dir, TextureAtlasSprite sprite, int rgb) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float min = INNER_SHIFT;
        float max = 1.0F - INNER_SHIFT;
        float[][] corners = switch (dir) {
            case DOWN -> new float[][]{{min, min, max}, {min, min, min}, {max, min, min}, {max, min, max}};
            case UP -> new float[][]{{min, max, min}, {min, max, max}, {max, max, max}, {max, max, min}};
            case NORTH -> new float[][]{{max, max, min}, {max, min, min}, {min, min, min}, {min, max, min}};
            case SOUTH -> new float[][]{{min, max, max}, {min, min, max}, {max, min, max}, {max, max, max}};
            case WEST -> new float[][]{{min, max, min}, {min, min, min}, {min, min, max}, {min, max, max}};
            case EAST -> new float[][]{{max, max, max}, {max, min, max}, {max, min, min}, {max, max, min}};
        };
        int packed = packColor(rgb);
        int[] verts = new int[32];
        int nx = dir.getStepX();
        int ny = dir.getStepY();
        int nz = dir.getStepZ();
        int normal = ((nz) & 0xFF) << 16 | ((ny) & 0xFF) << 8 | (nx & 0xFF);
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            verts[o] = Float.floatToRawIntBits(corners[i][0]);
            verts[o + 1] = Float.floatToRawIntBits(corners[i][1]);
            verts[o + 2] = Float.floatToRawIntBits(corners[i][2]);
            verts[o + 3] = packed;
            verts[o + 4] = Float.floatToRawIntBits(uv[i][0]);
            verts[o + 5] = Float.floatToRawIntBits(uv[i][1]);
            verts[o + 6] = 0;
            verts[o + 7] = normal;
        }
        return new BakedQuad(verts, 0, dir, sprite, false);
    }

    private static int packColor(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int a = (rgb >>> 24) == 0 ? 255 : (rgb >>> 24);
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static BakedQuad adjustInnerQuad(BakedQuad quad, BlockState inner, @Nullable BlockPos pos) {
        int[] verts = quad.getVertices().clone();
        int tint = 0xFFFFFF;
        if (quad.getTintIndex() != -1) {
            Level level = Minecraft.getInstance().level;
            tint = Minecraft.getInstance().getBlockColors().getColor(inner, level, pos, quad.getTintIndex());
            if (tint == -1) {
                tint = 0xFFFFFF;
            }
        }
        int tr = (tint >> 16) & 255;
        int tg = (tint >> 8) & 255;
        int tb = tint & 255;

        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            float x = Float.intBitsToFloat(verts[o]);
            float y = Float.intBitsToFloat(verts[o + 1]);
            float z = Float.intBitsToFloat(verts[o + 2]);
            verts[o] = Float.floatToRawIntBits(x * INNER_SCALE + INNER_SHIFT);
            verts[o + 1] = Float.floatToRawIntBits(y * INNER_SCALE + INNER_SHIFT);
            verts[o + 2] = Float.floatToRawIntBits(z * INNER_SCALE + INNER_SHIFT);

            if (quad.getTintIndex() != -1) {
                int packed = verts[o + 3];
                int r = packed & 255;
                int g = (packed >> 8) & 255;
                int b = (packed >> 16) & 255;
                int a = (packed >> 24) & 255;
                r = r * tr / 255;
                g = g * tg / 255;
                b = b * tb / 255;
                verts[o + 3] = (a << 24) | (b << 16) | (g << 8) | r;
            }
        }
        return new BakedQuad(verts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState inner = data.get(ChainBlockEntity.INNER);
        ChunkRenderTypeSet types = ChunkRenderTypeSet.of(RenderType.cutout());
        if (inner != null && !inner.isAir()) {
            if (inner.getRenderShape() == RenderShape.MODEL) {
                BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(inner);
                types = ChunkRenderTypeSet.union(innerModel.getRenderTypes(inner, rand, ModelData.EMPTY), types);
            }
            if (!inner.getFluidState().isEmpty()) {
                types = ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(RenderType.translucent()));
            }
        }
        return types;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return chain.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return filledItem;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return chain.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return OVERRIDES;
    }

    @Override
    public ItemTransforms getTransforms() {
        return chain.getTransforms();
    }
}

