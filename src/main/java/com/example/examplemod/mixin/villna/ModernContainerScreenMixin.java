package com.example.examplemod.mixin.villna;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractContainerScreen.class)
public abstract class ModernContainerScreenMixin {

    /**
     * 现代化的槽位高亮
     */
    @Overwrite
    public static void renderSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset, int color) {
        int highlightColor = 0xFFFFFFFF; // 纯白色高亮，不透明

        // 1. 保存当前矩阵状态
        guiGraphics.pose().pushPose();

        guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);

        int sx = x * 2;
        int sy = y * 2;
        int sSize = 16 * 2;
        int thickness = 1;

        // 上边 (Top)
        guiGraphics.fill(sx, sy, sx + sSize, sy + thickness, blitOffset, highlightColor);

        // 左边 (Left)
        guiGraphics.fill(sx, sy + thickness, sx + thickness, sy + sSize - thickness, blitOffset, highlightColor);

        guiGraphics.pose().popPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}