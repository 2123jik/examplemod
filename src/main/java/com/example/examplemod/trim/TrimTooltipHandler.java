package com.example.examplemod.trim;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class TrimTooltipHandler {

    // 使用 NORMAL 优先级注入单行紧凑展示
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void renderCompactTrimTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();
        var registries = event.getContext().registries();
        if (registries == null) return;

        List<ModDataComponents.TrimStep> history = stack.get(ModDataComponents.TRIM_HISTORY.get());
        ArmorTrim singleTrim = stack.get(DataComponents.TRIM);

        // 如果既没有多步工序历史，也没有单层 Trim，直接退出
        if ((history == null || history.isEmpty()) && singleTrim == null) {
            return;
        }

        // 构建行首标头："纹饰：" (灰白微亮)
        MutableComponent line = Component.translatable("tooltip." + ExampleMod.MODID + ".trims_header")
                .withStyle(ChatFormatting.GRAY);

        List<Component> stepComponents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            // 1. 优先展示我们模组的时序历史（包含所有刻印工序）
            for (ModDataComponents.TrimStep step : history) {
                Component stepText = buildSingleTrimComponent(registries, step.pattern(), step.material());
                if (stepText != null) {
                    stepComponents.add(stepText);
                }
            }
        } else {
            // 2. 没有多步历史（普通原版装备），展示原版单层 Trim
            Component singleText = buildFromArmorTrim(singleTrim);
            if (singleText != null) {
                stepComponents.add(singleText);
            }
        }

        if (stepComponents.isEmpty()) return;

        // 组装到同一行，两个纹饰之间用空格隔开
        for (int i = 0; i < stepComponents.size(); i++) {
            line.append(stepComponents.get(i));
            if (i < stepComponents.size() - 1) {
                line.append("  ");
            }
        }

        // 插入在第一行（名字）后面
        int insertPos = Math.min(1, tooltip.size());
        tooltip.add(insertPos, line);
    }

    /**
     * 根据时序中的 patternId 和 materialId 从注册表中还原对应的本地化材质与名称
     * 解决显示 "copper" / "iron" 等未翻译字符串的 Bug
     */
    private static Component buildSingleTrimComponent(HolderLookup.Provider registries, String patternId, String materialId) {
        var patternRegistry = registries.lookupOrThrow(Registries.TRIM_PATTERN);
        var materialRegistry = registries.lookupOrThrow(Registries.TRIM_MATERIAL);

        ResourceLocation pLoc = ResourceLocation.tryParse(patternId);
        ResourceLocation mLoc = ResourceLocation.tryParse(materialId);

        // =========================================================================
        // 1. 材质 (Material) 解析：还原 (铜质)、(铁质) 及官方原生颜色
        // =========================================================================
        MutableComponent matComp = null;

        // A. 尝试直接按 Key 匹配 TrimMaterial (针对 "minecraft:copper")
        if (mLoc != null) {
            var directMat = materialRegistry.get(ResourceKey.create(Registries.TRIM_MATERIAL, mLoc));
            if (directMat.isPresent()) {
                matComp = directMat.get().value().description().copy();
            }
        }

        // B. 若传入的是物品 ID (如 "minecraft:copper_ingot")，调用原版方法通过原料反查 TrimMaterial
        if (matComp == null && mLoc != null) {
            Item item = BuiltInRegistries.ITEM.get(mLoc);
            if (item != Items.AIR) {
                ItemStack itemStack = new ItemStack(item);
                Optional<Holder.Reference<TrimMaterial>> fromIngredient = TrimMaterials.getFromIngredient(registries, itemStack);
                if (fromIngredient.isPresent()) {
                    // 成功拿到官方 "(铜质)"、"(铁质)"，且自带官方颜色！
                    matComp = fromIngredient.get().value().description().copy();
                } else {
                    // C. 若此材料不是原版 Trim 材料（如模组特有金属锭），直接提取该物品的官方本地化翻译名！
                    matComp = item.getName(itemStack).copy();
                }
            }
        }

        // D. 极端防空回退
        if (matComp == null) {
            matComp = Component.translatable("item." + (mLoc != null ? mLoc.toLanguageKey() : "unknown"));
        }

        // =========================================================================
        // 2. 纹饰样式 (Pattern) 解析：提取纯净纹饰名（如 "哨兵"、"寻路者"）
        // =========================================================================
        String patternStr = null;

        // A. 尝试直接按 Key 匹配 TrimPattern (针对 "minecraft:sentry")
        if (pLoc != null) {
            var directPat = patternRegistry.get(ResourceKey.create(Registries.TRIM_PATTERN, pLoc));
            if (directPat.isPresent()) {
                patternStr = cleanTrimSuffix(directPat.get().value().description().getString());
            }
        }

        // B. 尝试移除模板后缀后二次匹配
        if (patternStr == null && patternId != null) {
            String stripped = patternId.replace("_armor_trim_smithing_template", "")
                    .replace("_smithing_template", "");
            ResourceLocation strippedLoc = ResourceLocation.tryParse(stripped);
            if (strippedLoc != null) {
                var strippedPat = patternRegistry.get(ResourceKey.create(Registries.TRIM_PATTERN, strippedLoc));
                if (strippedPat.isPresent()) {
                    patternStr = cleanTrimSuffix(strippedPat.get().value().description().getString());
                }
            }
        }

        // C. 若依然未命中，从模板物品反查
        if (patternStr == null && pLoc != null) {
            Item item = BuiltInRegistries.ITEM.get(pLoc);
            if (item != Items.AIR) {
                ItemStack itemStack = new ItemStack(item);
                Optional<Holder.Reference<TrimPattern>> fromTemplate = TrimPatterns.getFromTemplate(registries, itemStack);
                if (fromTemplate.isPresent()) {
                    patternStr = cleanTrimSuffix(fromTemplate.get().value().description().getString());
                } else {
                    patternStr = cleanTrimSuffix(item.getName(itemStack).getString());
                }
            }
        }

        if (patternStr == null) {
            patternStr = simplifyName(patternId);
        }

        // =========================================================================
        // 3. 紧凑单行复合组装： (材质原色) 纹饰名
        // =========================================================================
        return Component.empty()
                .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                .append(matComp)
                .append(Component.literal(") ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(patternStr).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 原生 ArmorTrim 对象的转换
     */
    private static Component buildFromArmorTrim(ArmorTrim trim) {
        MutableComponent matComp = trim.material().value().description().copy();
        String patternStr = cleanTrimSuffix(trim.pattern().value().description().getString());

        return Component.empty()
                .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                .append(matComp)
                .append(Component.literal(") ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(patternStr).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 智能清洗“盔甲纹饰”等长后缀
     */
    private static String cleanTrimSuffix(String raw) {
        if (raw == null) return "";
        return raw.replace("盔甲纹饰", "")
                .replace("纹饰锻造模板", "")
                .replace("锻造模板", "")
                .replace(" Armor Trim", "")
                .replace("armor trim", "")
                .replace(" Smithing Template", "")
                .replace("smithing template", "")
                .trim();
    }

    private static String simplifyName(String id) {
        if (id == null) return "";
        int colon = id.indexOf(':');
        String name = colon >= 0 ? id.substring(colon + 1) : id;
        return name.replace("_ingot", "")
                .replace("_armor_trim_smithing_template", "")
                .replace("_smithing_template", "");
    }
}