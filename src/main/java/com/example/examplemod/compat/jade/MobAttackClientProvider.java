package com.example.examplemod.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MobAttackClientProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("actual_attack")) return;

        double damage = data.getDouble("actual_attack");
        if (damage <= 0) return;

        // 直接显示攻击伤害：15.0
        tooltip.add(Component.translatable(
                "jade.examplemod.attack_damage",
                String.format("%.1f", damage)
        ).withStyle(ChatFormatting.RED));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("examplemod", "mob_attack");
    }
}