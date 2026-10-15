package com.nf121.examplemod.client.screen;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.registry.ModPayloads;
import dev.xkmc.l2backpack.content.common.BaseOpenableScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 集中管理向所有容器/背包注入自定义交互按钮的逻辑
 */
@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public final class ContainerScreenButtons {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        // 1. 原生背包特有按钮 (商店 & 睡觉)
        if (screen instanceof InventoryScreen invScreen) {
            int width = 32, height = 16;
            int totalWidth = width * 2;
            int x = invScreen.getGuiLeft() + (176 - totalWidth) / 2;
            int y = invScreen.getGuiTop() + 168;

            event.addListener(Button.builder(Component.translatable("examplemod.button.open_shop"),
                    btn -> PacketDistributor.sendToServer(new ModPayloads.OpenShopSelectorPayload()))
                    .bounds(x, y, width, height).build());

            event.addListener(Button.builder(Component.translatable("examplemod.button.sleep"),
                    btn -> PacketDistributor.sendToServer(new ModPayloads.SleepButtonPayload()))
                    .bounds(x + width, y, width, height).build());
        }

        // 2. 容器通用整理至背饰按钮
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            // 排除 L2Backpack 自带界面
            if (screen instanceof BaseOpenableScreen<?>) return;

            // 排除包含末影箱容器的界面（Ender Backpack 常用 ChestScreen）
            if (containerScreen.getMenu() instanceof ChestMenu chestMenu) {
                if (chestMenu.getContainer() instanceof PlayerEnderChestContainer) {
                    return; // 严禁在末影箱/末影背包界面放置外部注入按钮
                }
            }

            int left = containerScreen.getGuiLeft();
            int top = containerScreen.getGuiTop();
            int width = containerScreen.getXSize();
            boolean isPlayerInv = (screen instanceof InventoryScreen);

            Button sortBtn = Button.builder(Component.literal("⮯"), btn -> {
                        boolean fromContainer = !isPlayerInv && !Screen.hasShiftDown();
                        PacketDistributor.sendToServer(new ModPayloads.SortToBackpackPayload(fromContainer));
                    })
                    .bounds(left + width - 20, top + 4, 14, 14)
                    .tooltip(Tooltip.create(Component.literal(isPlayerInv ? "物品存入背包" : "存入背包 (Shift: 存玩家背包)")))
                    .build();

            event.addListener(sortBtn);
        }
    }
}