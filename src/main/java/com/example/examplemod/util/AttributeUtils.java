package com.example.examplemod.util;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

public class AttributeUtils {

    /**
     * 自动匹配并获取属性的描述文本，若无描述则返回 null
     */
    @Nullable
    public static String getAttributeDescription(ResourceLocation id, Attribute attr) {
        if (attr == null) return null;

        String baseKey = attr.getDescriptionId();

        // 1. 标准策略（命中率 99%，能完美匹配你给出的所有 key）
        // 如: "attribute.name.generic.luck.desc"
        //     "neoforge.entity_reach.desc"
        //     "apothic_attributes:draw_speed.desc"
        String descKey = baseKey + ".desc";
        if (I18n.exists(descKey)) {
            return I18n.get(descKey);
        }

        // 2. 备选策略 A：有的模组会基于 ResourceLocation 命名（namespace.path.desc）
        String fallbackKey1 = id.getNamespace() + "." + id.getPath() + ".desc";
        if (I18n.exists(fallbackKey1)) {
            return I18n.get(fallbackKey1);
        }

        // 3. 备选策略 B：attribute.namespace.path.desc
        String fallbackKey2 = "attribute." + id.getNamespace() + "." + id.getPath() + ".desc";
        if (I18n.exists(fallbackKey2)) {
            return I18n.get(fallbackKey2);
        }

        return null;
    }
}