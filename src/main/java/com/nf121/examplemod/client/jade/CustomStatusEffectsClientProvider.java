package com.nf121.examplemod.client.jade;

import com.nf121.examplemod.compat.jade.CustomStatusEffectsServerProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum CustomStatusEffectsClientProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        // 直接解码同 UID 服务端发过来的药水列表
        List<MobEffectInstance> effects = CustomStatusEffectsServerProvider.INSTANCE
                .decodeFromData(accessor)
                .orElse(List.of());

        if (!effects.isEmpty()) {
            // 添加单行 6 个的图标卡片
            tooltip.add(new CustomEffectsElement(effects));
        }
    }

    @Override
    public ResourceLocation getUid() {
        // 必须与服务端 UID 完全一致！这样客户端发包请求时服务端才会响应
        return CustomStatusEffectsServerProvider.UID;
    }
}