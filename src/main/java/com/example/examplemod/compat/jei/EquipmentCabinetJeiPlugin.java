package com.example.examplemod.compat.jei;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * JEI 对装备柜的界面避让适配。
 *
 * Modern UI 的装备柜占用整个屏幕，所以整个屏幕都是 JEI 排除区域。
 * 只对 EquipmentCabinetMenu 生效，普通箱子、背包、工作台不受影响。
 *
 * 需要项目编译依赖包含 JEI API。
 * JEI 通过 @JeiPlugin 自动发现，不需要在 ExampleMod 中手动注册。
 */
@JeiPlugin
public final class EquipmentCabinetJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "equipment_cabinet"
            );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {

            @Override
            public List<Rect2i> getGuiExtraAreas() {
                Screen screen = Minecraft.getInstance().screen;

                if (!(screen instanceof AbstractContainerScreen<?> container)) {
                    return List.of();
                }

                if (!(container.getMenu() instanceof EquipmentCabinetMenu)) {
                    return List.of();
                }

                if (screen.width <= 0 || screen.height <= 0) {
                    return List.of();
                }

                /*
                 * JEI 使用 Minecraft GUI 坐标。
                 *
                 * 必须使用 screen.width / screen.height，
                 * 不要使用 Modern UI 的 dp，也不要使用窗口 framebuffer 像素尺寸。
                 */
                return List.of(new Rect2i(
                        0,
                        0,
                        screen.width,
                        screen.height
                ));
            }
        });
    }
}
