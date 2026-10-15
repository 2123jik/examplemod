package com.nf121.examplemod.entity;

import com.nf121.examplemod.client.ContainerScreenOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MinecraftSurfaceView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class EquipmentItemIcon extends MinecraftSurfaceView {

    public EquipmentItemIcon(Context context, ItemStack copiedStack) {
        super(context);

        setRenderer(new Renderer() {
            private int surfaceWidth;
            private int surfaceHeight;

            @Override
            public void onSurfaceChanged(int width, int height) {
                surfaceWidth = width;
                surfaceHeight = height;
            }

            @Override
            public void onDraw(
                    GuiGraphics graphics,
                    int mouseX,
                    int mouseY,
                    float partialTick,
                    double guiScale,
                    float alpha
            ) {
                if (copiedStack.isEmpty() || surfaceWidth <= 0 || surfaceHeight <= 0) {
                    return;
                }

                float width = (float) (surfaceWidth / guiScale);
                float height = (float) (surfaceHeight / guiScale);

                float size = Math.min(width, height) * 0.9F;
                float scale = size / 16.0F;

                graphics.flush();

                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);

                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(
                            (width - size) / 2.0F,
                            (height - size) / 2.0F,
                            0.0F
                    );
                    graphics.pose().scale(scale, scale, 1.0F);

                    // 1. 渲染物品 3D 模型
                    graphics.renderItem(copiedStack, 0, 0);

                    // 2. 渲染耐久损耗条与堆叠数
                    Font font = Minecraft.getInstance().font;
                    graphics.renderItemDecorations(font, copiedStack, 0, 0);

                    // 3. 接入神化 / 宝石角标
                    ContainerScreenOverlay.renderItemOverlay(graphics, font, copiedStack, 0, 0);

                    graphics.flush();
                } finally {
                    graphics.pose().popPose();

                    // 【关键】：画完立即抹除深度值，绝对不留任何 3D 残渣给后续绘制
                    RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                    RenderSystem.disableDepthTest();
                    RenderSystem.defaultBlendFunc();
                }
            }
        });
    }
}