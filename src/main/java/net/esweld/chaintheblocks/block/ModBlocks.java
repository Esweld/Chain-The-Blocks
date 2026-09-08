package net.esweld.chaintheblocks.block;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.block.custom.ChainBlock;
import net.esweld.chaintheblocks.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ChainTheBlocks.MOD_ID);

    public static final RegistryObject<Block> CHAIN_BLOCK = registerBlock("chain_block",
            () -> new ChainBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                    .sound(SoundType.CHAIN)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .lightLevel(state -> state.hasProperty(ChainBlock.LIGHT) ? state.getValue(ChainBlock.LIGHT) : 0)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().fireResistant()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
