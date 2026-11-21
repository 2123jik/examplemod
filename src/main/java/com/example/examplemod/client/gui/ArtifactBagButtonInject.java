package com.example.examplemod.client.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModPayloads;
import dev.xkmc.l2artifacts.content.search.common.AbstractScrollerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ArtifactBagButtonInject {

    private ArtifactBagButtonInject() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractScrollerScreen<?> screen) {
            int buttonWidth = 72;
            int buttonHeight = 16;
            int x = screen.getGuiLeft() + screen.getXSize() - buttonWidth - 8;
            int y = screen.getGuiTop() + 6;

            Button upgradeButton = Button.builder(
                    Component.literal("一键升阶"),
                    btn -> PacketDistributor.sendToServer(new ModPayloads.AutoUpgradeArtifactsPayload())
            ).bounds(x, y, buttonWidth, buttonHeight).build();

            event.addListener(upgradeButton);
        }
    }
}