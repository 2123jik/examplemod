package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.entity.attachments.ClientAuraData;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ManaBarOverlay.class, remap = false)
public abstract class ManaBarOverlayMixin {

    /**
     * 1. 拦截第 2 次 blit（即蓝色填充条），保留原版左->右蓝条，并在最右->左叠加暗紫色锁定段
     * 原代码：guiHelper.blit(TEXTURE, barX, barY, spriteX, spriteY + 21, fillWidth, 21);
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            )
    )
    private void examplemod$renderReservedManaSegment(
            GuiGraphics guiHelper,
            ResourceLocation texture,
            int barX,
            int barY,
            int spriteX,
            int vOffset,
            int fillWidth,
            int height
    ) {
        // 先原封不动画左边的可用蓝条
        guiHelper.blit(texture, barX, barY, spriteX, vOffset, fillWidth, height);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        float reservedMana = ClientAuraData.getReservedMana();
        if (reservedMana <= 0.0F) return;

        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0.0) return;

        // 依据源码：Anchor.XP 的 spriteX == 68 且宽 188；其余所有锚点 spriteX == 0 且宽 98
        int imageWidth = (spriteX == 68) ? 188 : 98;
        int lockedWidth = (int) Math.round((double) imageWidth * Math.min((double) reservedMana / maxMana, 1.0));

        if (lockedWidth > 0) {
            lockedWidth = Math.min(lockedWidth, imageWidth);
            int destX = barX + imageWidth - lockedWidth;
            int srcX = spriteX + imageWidth - lockedWidth;

            // 暗紫灰色半透明着色，从最右端向左截取 ISS 自己的填充条精灵
            RenderSystem.setShaderColor(0.42F, 0.22F, 0.55F, 0.92F);
            guiHelper.blit(texture, destX, barY, srcX, vOffset, lockedWidth, height);
            // 务必立刻还原颜色状态，否则后续文字和 HUD 全都会变紫！
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * 2. 把蓝条文字 "当前蓝/最大蓝" 改成 "当前蓝/可用上限"（例如 350/350 而不是 350/500）
     * 原代码：guiHelper.drawString(Minecraft.getInstance().font, manaFraction, textX, textY, TEXT_COLOR);
     */
    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"
            ),
            index = 1
    )
    private String examplemod$modifyManaText(String originalText) {
        float reservedMana = ClientAuraData.getReservedMana();
        if (reservedMana <= 0.0F) {
            return originalText;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return originalText;
        }

        int maxMana = (int) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        int usableMax = Math.max(0, maxMana - Math.round(reservedMana));
        int mana = ClientMagicData.getPlayerMana();

        // 如果你更想直观看到被锁了多少，也可以改成：mana + "/" + usableMax + " (-" + Math.round(reservedMana) + ")"
        return mana + "/" + usableMax;
    }
}