package com.example.examplemod.runeword;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class EquipmentQualityClientRenderer {

    // 直接对接数据包生成的 5 大品质 Tag
    private static final TagKey<Item> TAG_UNIQUE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/unique"));
    private static final TagKey<Item> TAG_SET    = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/set"));
    private static final TagKey<Item> TAG_RARE   = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/rare"));
    private static final TagKey<Item> TAG_MAGIC  = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/magic"));
    private static final TagKey<Item> TAG_WHITE  = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "base/all"));

    public enum Quality { UNIQUE, SET, RARE, MAGIC, BASE, NONE }

    public static Quality getQuality(ItemStack stack) {
        if (stack.isEmpty()) return Quality.NONE;

        if (stack.is(TAG_UNIQUE)) return Quality.UNIQUE;
        if (stack.is(TAG_SET))    return Quality.SET;
        if (stack.is(TAG_RARE))   return Quality.RARE;
        if (stack.is(TAG_MAGIC))  return Quality.MAGIC;
        if (stack.is(TAG_WHITE))  return Quality.BASE;

        return Quality.NONE;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        Quality quality = getQuality(event.getItemStack());
        if (quality == Quality.NONE) return;

        int bgStart, bgEnd, borderStart, borderEnd;

        switch (quality) {
            case UNIQUE -> { // 暗金色
                borderStart = 0xFFF0C850; borderEnd = 0xFF6A4B10;
                bgStart     = 0xF018120A; bgEnd     = 0xF02A1E0E;
            }
            case SET -> { // 翡翠绿色
                borderStart = 0xFF00FF7F; borderEnd = 0xFF004D25;
                bgStart     = 0xF005140A; bgEnd     = 0xF00B2914;
            }
            case RARE -> { // 稀有金黄
                borderStart = 0xFFFFE033; borderEnd = 0xFF7A6800;
                bgStart     = 0xF01A1805; bgEnd     = 0xF029240A;
            }
            case MAGIC -> { // 魔法幽蓝
                borderStart = 0xFF4D94FF; borderEnd = 0xFF103070;
                bgStart     = 0xF0060E1A; bgEnd     = 0xF00E1C33;
            }
            case BASE -> { // 底材玄铁银灰
                borderStart = 0xFFCCCCCC; borderEnd = 0xFF4A4A4A;
                bgStart     = 0xF0121212; bgEnd     = 0xF0202020;
            }
            default -> { return; }
        }

        event.setBackgroundStart(bgStart);
        event.setBackgroundEnd(bgEnd);
        event.setBorderStart(borderStart);
        event.setBorderEnd(borderEnd);
    }
}