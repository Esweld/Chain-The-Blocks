package net.esweld.chaintheblocks.item;

import com.google.common.util.concurrent.ClosingFuture;
import net.esweld.chaintheblocks.ChainTheBlocks;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChainTheBlocks.MOD_ID);

    // how to add item: public static final RegistryObject<Item> NAME = ITEMS.register("Name",
    //      () ->)
    public static final RegistryObject<Item> CHAINBLOCK = ITEMS.register("chainblock",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
