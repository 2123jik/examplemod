package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ContainerScreenOverlay {

    private static final int OVERLAY_Z = 300;

    @SubscribeEvent
    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
        var guiGraphics = event.getGuiGraphics();
        var screen = event.getContainerScreen();
        Font font = Minecraft.getInstance().font;

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            int x = slot.x;
            int y = slot.y;

            // --- 核心修改：食物属性渲染 (左上角: 饥饿值 / 右上角: 饱和度) ---
            FoodProperties foodProps = stack.get(DataComponents.FOOD);
            if (foodProps != null) {
                float scale = 0.5f; // 缩放比例：0.75倍 (可根据需要调整为 0.5f 或 0.6f)

                // 1. 左上角：饥饿回复值 (Nutrition)
                int nutrition = foodProps.nutrition();
                String hungerText = String.valueOf(nutrition);

                guiGraphics.pose().pushPose();
                // 平移到目标渲染位置
                guiGraphics.pose().translate(x + 1, y + 1, OVERLAY_Z);
                // 应用缩放
                guiGraphics.pose().scale(scale, scale, 1.0f);
                // 此时在缩放后的局部坐标 (0, 0) 处绘制文字，颜色为亮橙色
                guiGraphics.drawString(font, hungerText, 0, 0, 0xFFEAA154, true);
                guiGraphics.pose().popPose();


                int saturation =(int) foodProps.saturation();
                String satText = String.valueOf(saturation);

                // 计算文本在原版字号下的宽度，并乘上缩放比例得出实际渲染宽度
                float satWidth = font.width(satText) * scale;
                // 右上角起始点的 X 坐标计算 (Slot右边缘 16 像素，减去文字宽度，再空出 1 像素间距)
                float satX = x + 16 - satWidth - 1;

                guiGraphics.pose().pushPose();
                // 平移到计算后的右上角位置
                guiGraphics.pose().translate(satX, y + 1, OVERLAY_Z);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                // 在局部坐标 (0, 0) 处绘制文字，颜色为金黄色
                guiGraphics.drawString(font, satText, 0, 0, 0xFFE3BB22, true);
                guiGraphics.pose().popPose();

                // 如果是食物，则跳过后续的 Apotheosis 渲染，防止图标叠加冲突
                continue;
            }
            ItemEnchantments storedEnchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (storedEnchants != null && !storedEnchants.isEmpty()) {
                float enchantScale = 0.5f; // 缩放比例

                // 16px 映射到 0.5 缩放的局部坐标系宽度为 32px
                int maxFontWidth = (int) (16.0f / enchantScale);

                // 1. 拼接所有附魔名称
                // 用极简间隔分割（例如用单个空格、或点号，甚至不加分隔符）
                StringBuilder sb = new StringBuilder();
                for (var entry : storedEnchants.entrySet()) {
                    String name = entry.getKey().value().description().getString().replace(" ", "");
                    if (!sb.isEmpty()) {
                        sb.append(" "); // 附魔之间的微小间隔，觉得宽可以去掉或换成其他字符
                    }
                    sb.append(name);
                }

                // 2. 将整串文本按 32px 宽度自动流式折行
                var lines = font.split(Component.literal(sb.toString()), maxFontWidth);

                // 紧密行距：局部坐标系下设为 7.0f（缩放后相当于实际 3.5px 行高）
                float localLineStep = 7.0f;
                float maxLocalY = 16.0f / enchantScale; // 局部坐标系最大高度 32.0f

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x, y, OVERLAY_Z);
                guiGraphics.pose().scale(enchantScale, enchantScale, 1.0f);

                float localY = 0.0f;
                for (var line : lines) {
                    // 检查渲染后是否越界
                    if (localY + 7.0f > maxLocalY) {
                        break;
                    }

                    // 绘制当前行（关闭阴影）
                    guiGraphics.drawString(font, line, 0, (int) localY, 0xFF55FFFF, false);

                    localY += localLineStep;
                }

                guiGraphics.pose().popPose();
                continue;
            }
            // --- 原有的 Apotheosis / Iron's Spells 渲染逻辑 ---
            var rarityHolder = stack.get(Apoth.Components.RARITY);
            if (rarityHolder != null && rarityHolder.isBound()) {
                renderPip(guiGraphics, x + 1, y + 1, rarityHolder.get().color().getValue() | 0xFF000000);
            }

            if (stack.getItem() instanceof Scroll) {
                Integer scrollColor = getScrollRarityColor(stack);
                if (scrollColor != null) renderPip(guiGraphics, x + 1, y + 1, scrollColor);
            }

            Purity purity = stack.get(Apoth.Components.PURITY);
            if (purity != null && purity.getColor() != null) {
                int size = 3;
                renderPip(guiGraphics, slot.x + 16 - size - 1, y + 1, purity.getColor().getValue() | 0xFF000000);
            }
        }
    }

    private static Integer getScrollRarityColor(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.getBoolean("is_unidentified") && tag.contains("spell_rarity")) {
                try {
                    SpellRarity rarity = SpellRarity.valueOf(tag.getString("spell_rarity").toUpperCase(Locale.ROOT));
                    Integer color = rarity.getChatFormatting().getColor();
                    return color != null ? (color | 0xFF000000) : null;
                } catch (Exception ignored) {}
            }
        }

        if (ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer container = ISpellContainer.get(stack);
            if (container != null && !container.isEmpty()) {
                SpellData spellData = container.getSpellAtIndex(0);
                if (spellData != null && spellData != SpellData.EMPTY) {
                    Integer color = spellData.getRarity().getChatFormatting().getColor();
                    return color != null ? (color | 0xFF000000) : null;
                }
            }
        }
        return null;
    }

    private static void renderPip(GuiGraphics guiGraphics, int x, int y, int argbColor) {
        int size = 3;
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, OVERLAY_Z - 1, 0x60000000);
        guiGraphics.fill(x, y, x + size, y + size, OVERLAY_Z, argbColor);
    }
}
