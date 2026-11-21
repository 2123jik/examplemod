package com.example.examplemod.client.jade;

import com.example.examplemod.compat.jade.CustomStatusEffectsServerProvider;
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
        List<MobEffectInstance> effects = CustomStatusEffectsServerProvider.INSTANCE
                .decodeFromData(accessor)
                .orElse(List.of());

        if (!effects.isEmpty()) {
            tooltip.add(new CustomEffectsElement(effects));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return CustomStatusEffectsServerProvider.UID;
    }
}