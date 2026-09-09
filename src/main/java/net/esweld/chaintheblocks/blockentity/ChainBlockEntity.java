package net.esweld.chaintheblocks.blockentity;

import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ChainBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> INNER = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POS = new ModelProperty<>();

    private static final String STATE_KEY = "ContainedState";
    private static final String BE_KEY = "ContainedBE";

    @Nullable
    private BlockState containedState;
    @Nullable
    private CompoundTag containedBeTag;

    public ChainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHAIN_BLOCK.get(), pos, state);
    }

    public boolean hasContained() {
        return containedState != null && !containedState.isAir();
    }

    @Nullable
    public BlockState getContainedState() {
        return containedState;
    }

    @Nullable
    public CompoundTag copyContainedBeTag() {
        return containedBeTag == null ? null : containedBeTag.copy();
    }

    public boolean needsEntityRenderer() {
        return hasContained() && containedState.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public void setContained(@Nullable BlockState state, @Nullable CompoundTag beTag) {
        this.containedState = state;
        this.containedBeTag = beTag == null ? null : beTag.copy();
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide) {
            syncLight();
            BlockState current = getBlockState();
            level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            syncLight();
        }
        requestModelDataUpdate();
    }

    private void syncLight() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState current = getBlockState();
        if (!current.hasProperty(ChainBlock.FILLED) || !current.hasProperty(ChainBlock.LIGHT)) {
            return;
        }
        int light = 0;
        if (current.getValue(ChainBlock.FILLED) && containedState != null) {
            light = Mth.clamp(containedState.getLightEmission(), 0, 15);
        }
        if (current.getValue(ChainBlock.LIGHT) != light) {
            level.setBlock(worldPosition, current.setValue(ChainBlock.LIGHT, light),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public @NotNull ModelData getModelData() {
        if (!hasContained()) {
            return ModelData.EMPTY;
        }
        return ModelData.builder()
                .with(INNER, containedState)
                .with(POS, worldPosition)
                .build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (containedState != null) {
            tag.put(STATE_KEY, NbtUtils.writeBlockState(containedState));
            if (containedBeTag != null) {
                tag.put(BE_KEY, containedBeTag.copy());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(STATE_KEY)) {
            containedState = NbtUtils.readBlockState(
                    level != null ? level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(),
                    tag.getCompound(STATE_KEY));
            containedBeTag = tag.contains(BE_KEY) ? tag.getCompound(BE_KEY) : null;
        } else {
            containedState = null;
            containedBeTag = null;
        }
        requestModelDataUpdate();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}