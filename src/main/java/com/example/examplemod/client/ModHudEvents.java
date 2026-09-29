package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class ModHudEvents {

    // 1. 监听 MOD 总线：注册自定义 HUD 图层
    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "status_hud"),
                (guiGraphics, deltaTracker) -> StatusHudRenderer.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
            );
        }
    }

    // 2. 监听 GAME 总线：隐藏原版生命值与饱食度
    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {
        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            // 拦截原版血条、饱食度渲染
            if (VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName()) || 
                VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
                event.setCanceled(true);
            }
        }
    }
}