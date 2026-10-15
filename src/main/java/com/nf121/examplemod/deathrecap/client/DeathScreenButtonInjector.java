package com.nf121.examplemod.deathrecap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class DeathScreenButtonInjector {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // 当打开的是原版死亡界面时
        if (event.getScreen() instanceof DeathScreen deathScreen) {
            int buttonWidth = 200;
            int buttonHeight = 20;
            int x = deathScreen.width / 2 - buttonWidth / 2;
            // 原版“返回标题界面”下方 28 像素的位置
            int y = deathScreen.height / 4 + 120;

            // 注入【查看死亡回放快照】按钮
            Button recapBtn = Button.builder(Component.literal("§c§l▶ 查看死亡快照 / 回放 ◀"), b -> {
                if (!ClientDeathRecapCache.LAST_RECORDS.isEmpty()) {
                    // 打开快照浏览器
// 传入 deathScreen 作为 previousScreen
                    Minecraft.getInstance().setScreen(new DeathRecapScreen(ClientDeathRecapCache.LAST_RECORDS, deathScreen));                }
            })
            .bounds(x, y, buttonWidth, buttonHeight)
            .build();

            event.addListener(recapBtn);
        }
    }
}