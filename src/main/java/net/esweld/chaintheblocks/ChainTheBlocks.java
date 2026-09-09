package net.esweld.chaintheblocks;

import com.mojang.logging.LogUtils;
import net.esweld.chaintheblocks.block.ModBlocks;
import net.esweld.chaintheblocks.blockentity.ModBlockEntities;
import net.esweld.chaintheblocks.item.ModItems;
import net.esweld.chaintheblocks.wrapping.ChainInsertDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Map;

@Mod(ChainTheBlocks.MOD_ID)
public class ChainTheBlocks {
    public static final String MOD_ID = "chaintheblocks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ChainTheBlocks(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ChainTheBlocks::registerDispenserBehaviors);
    }

    @SuppressWarnings("unchecked")
    private static void registerDispenserBehaviors() {
        Map<Item, DispenseItemBehavior> registry = null;
        try {
            registry = ObfuscationReflectionHelper.getPrivateValue(DispenserBlock.class, null, "f_52661_");
        } catch (Exception ignored) {
        }
        if (registry == null) {
            try {
                var field = DispenserBlock.class.getDeclaredField("DISPENSER_REGISTRY");
                field.setAccessible(true);
                registry = (Map<Item, DispenseItemBehavior>) field.get(null);
            } catch (Exception e) {
                LOGGER.error("Could not hook dispenser behaviors for chain blocks", e);
                return;
            }
        }
        for (Item item : ForgeRegistries.ITEMS) {
            boolean wrap = item instanceof BlockItem
                    || (item instanceof BucketItem bucket
                    && (bucket.getFluid() == Fluids.WATER || bucket.getFluid() == Fluids.LAVA));
            if (wrap) {
                DispenseItemBehavior existing = registry.get(item);
                if (existing instanceof ChainInsertDispenseBehavior) {
                    continue;
                }
                DispenserBlock.registerBehavior(item, new ChainInsertDispenseBehavior(existing));
            }
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.CHAIN_BLOCK);
        }
    }
}