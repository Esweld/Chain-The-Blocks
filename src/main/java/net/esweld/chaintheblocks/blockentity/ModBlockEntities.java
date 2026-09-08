package net.esweld.chaintheblocks.blockentity;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChainTheBlocks.MOD_ID);

    public static final RegistryObject<BlockEntityType<ChainBlockEntity>> CHAIN_BLOCK =
            BLOCK_ENTITIES.register("chain_block",
                    () -> BlockEntityType.Builder.of(ChainBlockEntity::new, ModBlocks.CHAIN_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}