// EquipmentCabinetClient.java
package com.example.examplemod.client.screen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import icyllis.modernui.mc.neoforge.MenuScreenFactory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static com.example.examplemod.registry.ModRegistry.CABINET_MENU;

@EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT
)
public final class EquipmentCabinetClient {
    private EquipmentCabinetClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                CABINET_MENU.get(),
                MenuScreenFactory.create(EquipmentCabinetFragment::new)
        );
    }
    // 使用 JVM 原生 VarHandle，零性能开销修改 protected 字段
    private static final VarHandle IMAGE_WIDTH;
    private static final VarHandle IMAGE_HEIGHT;
    private static final VarHandle LEFT_POS;
    private static final VarHandle TOP_POS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(AbstractContainerScreen.class, MethodHandles.lookup());
            IMAGE_WIDTH = lookup.findVarHandle(AbstractContainerScreen.class, "imageWidth", int.class);
            IMAGE_HEIGHT = lookup.findVarHandle(AbstractContainerScreen.class, "imageHeight", int.class);
            LEFT_POS = lookup.findVarHandle(AbstractContainerScreen.class, "leftPos", int.class);
            TOP_POS = lookup.findVarHandle(AbstractContainerScreen.class, "topPos", int.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access AbstractContainerScreen fields", e);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            // 只针对我们自己的装备柜界面生效
            if (containerScreen.getMenu() instanceof EquipmentCabinetMenu) {
                int width = containerScreen.width;
                int height = containerScreen.height;

                // 将底层容器尺寸撑满屏幕
                IMAGE_WIDTH.set(containerScreen, width);
                IMAGE_HEIGHT.set(containerScreen, height);
                LEFT_POS.set(containerScreen, 0);
                TOP_POS.set(containerScreen, 0);
            }
        }
    }
}
