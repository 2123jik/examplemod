package com.example.examplemod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public final class CustomHudEventHandler {

    /**
     * 1. 安全拦截：彻底跳过原版 HUD 的所有图元与计算
     * 不修改任何字节码，让 ModernFix / ModernUI / Apotheosis 的 Mixin 和平共处
     */
    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        var layer = event.getName();

        // 原生图层拦截表 (命中即跳过)
        if (layer.equals(VanillaGuiLayers.PLAYER_HEALTH)     || // 生命值与吸收心
            layer.equals(VanillaGuiLayers.ARMOR_LEVEL)       || // 护甲
            layer.equals(VanillaGuiLayers.FOOD_LEVEL)        || // 饱食度
            layer.equals(VanillaGuiLayers.AIR_LEVEL)         || // 氧气气泡
            layer.equals(VanillaGuiLayers.EXPERIENCE_BAR)    || // 经验条与数字
            layer.equals(VanillaGuiLayers.JUMP_METER)        || // 坐骑蓄力条
            layer.equals(VanillaGuiLayers.VEHICLE_HEALTH)) {    // 载具血量
            event.setCanceled(true);
        }
    }

    /**
     * 2. 安全接管：在原版快捷栏绘制完毕的第一时间，插入我们的单批次极简 HUD
     */
    @SubscribeEvent
    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null && !mc.options.hideGui) {
                PinnacleAdaptiveHud.render(event.getGuiGraphics(), player);
            }
        }
    }
}