package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ModClientModelEvents {

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        // 统一映射：将你的便携物品直接重定向到对应的原版物品模型
        redirectItemModel(models, "portable_furnace", "furnace");
        redirectItemModel(models, "portable_stonecutter", "stonecutter");
        redirectItemModel(models, "portable_anvil", "anvil");
        redirectItemModel(models, "portable_brewing_stand", "brewing_stand");
    }

    private static void redirectItemModel(Map<ModelResourceLocation, BakedModel> models, String myItemName, String vanillaItemName) {
        // 原版物品模型位置: minecraft:<name>#inventory
        ModelResourceLocation vanillaLocation = new ModelResourceLocation(
                ResourceLocation.withDefaultNamespace(vanillaItemName),
                "inventory"
        );

        // 你的自定义物品模型位置: examplemod:<name>#inventory
        ModelResourceLocation myLocation = new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, myItemName),
                "inventory"
        );

        BakedModel vanillaModel = models.get(vanillaLocation);
        if (vanillaModel != null) {
            // 将你的物品指向原版的已烘焙模型
            models.put(myLocation, vanillaModel);
        }
    }
}