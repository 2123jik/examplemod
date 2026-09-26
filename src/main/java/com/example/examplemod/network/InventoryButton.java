package com.example.examplemod.network;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class InventoryButton {
    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        int width = 32;
        int height = 16;

        int x = screen.getGuiLeft() + (176 - width) / 2;
        int y = screen.getGuiTop() + 168;

        event.addListener(
                Button.builder(
                                Component.translatable(
                                        "examplemod.button.open_shop"
                                ),
                                button -> PacketDistributor.sendToServer(
                                        OpenShopSelectorPayload.INSTANCE
                                )
                        )
                        .bounds(x, y, width, height)
                        .build()
        );
        event.addListener(
                Button.builder(
                                Component.translatable(
                                        "examplemod.button.sleep"
                                ),
                                button -> PacketDistributor.sendToServer(
                                        SleepButtonPayload.INSTANCE
                                )
                        )
                        .bounds(x+width, y, width, height)
                        .build()
        );
    }
}
