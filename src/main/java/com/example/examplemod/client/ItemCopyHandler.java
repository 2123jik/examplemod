package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ItemCopyHandler {

    // 场景 1：在任意 GUI 界面中（背包、箱子等），鼠标悬停物品时按 Ctrl + C
    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        // 判断按键是否是 C，且按住了 Ctrl (Mac 下自动适配 Command)
        if (event.getKeyCode() == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                // 获取当前鼠标悬停的槽位
                Slot slot = containerScreen.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) {
                    copyItemId(slot.getItem());
                    event.setCanceled(true); // 拦截事件，防止与原版操作冲突
                }
            }
        }
    }

    // 场景 2：游戏中不打开任何界面，手持物品时按 Ctrl + C
    @SubscribeEvent
    public static void onGameKeyPressed(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            Minecraft mc = Minecraft.getInstance();
            // 确保没有打开任何界面，且玩家存在
            if (mc.screen == null && mc.player != null) {
                ItemStack mainHand = mc.player.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    copyItemId(mainHand);
                }
            }
        }
    }

    // 统一复制与提示工具方法
    private static void copyItemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String idStr = id.toString();

        // 1. 复制到系统剪贴板
        Minecraft.getInstance().keyboardHandler.setClipboard(idStr);

        // 2. 在快捷栏上方展示提示消息（不污染聊天框）
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.literal("§a[已复制 ID] §f" + idStr),
                    true // true 表示在 Action Bar 显示
            );
        }
    }
}