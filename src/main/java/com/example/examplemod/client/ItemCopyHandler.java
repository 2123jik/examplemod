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

@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ItemCopyHandler {


    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {

        if (event.getKeyCode() == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {

                Slot slot = containerScreen.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) {
                    copyItemId(slot.getItem());
                    event.setCanceled(true); 
                }
            }
        }
    }


    @SubscribeEvent
    public static void onGameKeyPressed(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.screen == null && mc.player != null) {
                ItemStack mainHand = mc.player.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    copyItemId(mainHand);
                }
            }
        }
    }


    private static void copyItemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String idStr = id.toString();


        Minecraft.getInstance().keyboardHandler.setClipboard(idStr);


        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.literal("§a[已复制 ID] §f" + idStr),
                    true 
            );
        }
    }
}