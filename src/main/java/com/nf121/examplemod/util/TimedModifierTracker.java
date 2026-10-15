package com.nf121.examplemod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TimedModifierTracker {
    private final Map<ResourceLocation, TimedAttributeModifier> activeModifiers = new HashMap<>();

    /**
     * 添加或刷新有时限的属性修饰符
     */
    public void addModifier(LivingEntity entity, TimedAttributeModifier modifier) {
        AttributeInstance instance = entity.getAttribute(modifier.getAttribute());
        if (instance == null) return;

        ResourceLocation id = modifier.getId();

        // 如果已经存在同名修饰符，先移除原版上的旧修饰符，避免冲突
        if (instance.hasModifier(id)) {
            instance.removeModifier(id);
        }

        // 添加瞬态修饰符（不需要写入实体 NBT，由我们的系统自行管理）
        instance.addTransientModifier(modifier.toVanillaModifier());
        activeModifiers.put(id, modifier);
    }

    /**
     * 实体 Tick 时调用，倒计时并自动清理过期属性
     */
    public void tick(LivingEntity entity) {
        if (activeModifiers.isEmpty()) return;

        Iterator<Map.Entry<ResourceLocation, TimedAttributeModifier>> it = activeModifiers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, TimedAttributeModifier> entry = it.next();
            TimedAttributeModifier modifier = entry.getValue();

            if (modifier.tick()) { // 已超时
                AttributeInstance instance = entity.getAttribute(modifier.getAttribute());
                if (instance != null) {
                    instance.removeModifier(entry.getKey());
                }
                it.remove();
            }
        }
    }
}