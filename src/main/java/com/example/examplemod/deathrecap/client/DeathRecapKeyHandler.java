package com.example.examplemod.deathrecap.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class DeathRecapKeyHandler {

    // 注册 K 键
    public static final KeyMapping OPEN_RECAP_KEY = new KeyMapping(
            "key.examplemod.open_recap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.misc"
    );

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RECAP_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_RECAP_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            // 如果当前已经在快照界面，按 K 键直接关闭
            if (mc.screen instanceof DeathRecapScreen) {
                mc.screen.onClose();
            } else if (!ClientDeathRecapCache.LAST_RECORDS.isEmpty()) {
                // 传入当前 screen（如果正在游戏画面，mc.screen 就是 null）
                mc.setScreen(new DeathRecapScreen(ClientDeathRecapCache.LAST_RECORDS, mc.screen));
            }
        }
    }
}