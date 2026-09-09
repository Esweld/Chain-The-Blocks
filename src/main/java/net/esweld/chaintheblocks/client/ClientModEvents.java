package net.esweld.chaintheblocks.client;

import net.esweld.chaintheblocks.ChainTheBlocks;
import net.esweld.chaintheblocks.blockentity.ModBlockEntities;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ChainTheBlocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        List<ResourceLocation> keys = new ArrayList<>();
        for (ResourceLocation key : models.keySet()) {
            if (!"chaintheblocks".equals(key.getNamespace()) || !"chain_block".equals(key.getPath())) {
                continue;
            }
            keys.add(key);
        }
        for (ResourceLocation key : keys) {
            models.put(key, new ChainBlockBakedModel(models.get(key)));
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CHAIN_BLOCK.get(), ChainBlockRenderer::new);
    }
}