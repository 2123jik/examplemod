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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
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
                // 格子有效宽度为 14px (16px 减去左右各 1px 边距)，换算到 font 的局部坐标宽度
                int maxFontWidth = (int) (14.0f / enchantScale); // 0.5 缩放下为 28 像素宽度

                // 缩放后的单行高度 (原版字高 9 * 缩放 0.5 = 4.5 像素，行距取 4.5f ~ 5.0f 均可)
                float lineStep = font.lineHeight * enchantScale;
                float currentY = y + 1.0f;

                // 遍历附魔书内包含的所有附魔
                for (var entry : storedEnchants.entrySet()) {
                    var enchantHolder = entry.getKey();
                    int level = entry.getIntValue();

                    // 1. 获取附魔译名并彻底去除空格 (例如 "火焰 附加" -> "火焰附加")
                    String name = enchantHolder.value().description().getString().replace(" ", "");

                    // 2. 罗马数字改用阿拉伯数字：如果是多级附魔或等级大于1，则追加阿拉伯数字；单级附魔不追加
                    int maxLevel = enchantHolder.value().getMaxLevel();
                    String displayText = (maxLevel > 1 || level > 1) ? (name + level) : name;

                    // 3. 按像素宽度自动折行 (汉字/英文字符都会严格按像素宽度截断换行)
                    var lines = font.split(Component.literal(displayText), maxFontWidth);

                    for (var line : lines) {
                        // 防止行渲染溢出 Slot 底部 (Slot 范围是 y 到 y + 16)
                        if (currentY + lineStep > y + 15.5f) {
                            break;
                        }

                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(x + 1.0f, currentY, OVERLAY_Z);
                        guiGraphics.pose().scale(enchantScale, enchantScale, 1.0f);
                        guiGraphics.drawString(font, line, 0, 0, 0xFF55FFFF, true);
                        guiGraphics.pose().popPose();

                        currentY += lineStep;
                    }

                    // 如果格子空间已满，不再处理后续附魔
                    if (currentY + lineStep > y + 15.5f) {
                        break;
                    }
                }

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
