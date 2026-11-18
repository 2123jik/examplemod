package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class HolyAuraAttribute {

    public static final ResourceLocation FIRE_AURA_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "holy_fire_aura");
    public static final ResourceLocation FREEZE_AURA_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "holy_freeze_aura");
    public static final ResourceLocation SHOCK_AURA_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "holy_shock_aura");

    public static final DeferredHolder<Attribute, Attribute> FIRE_AURA = DeferredHolder.create(Registries.ATTRIBUTE, FIRE_AURA_ID);
    public static final DeferredHolder<Attribute, Attribute> FREEZE_AURA = DeferredHolder.create(Registries.ATTRIBUTE, FREEZE_AURA_ID);
    public static final DeferredHolder<Attribute, Attribute> SHOCK_AURA = DeferredHolder.create(Registries.ATTRIBUTE, SHOCK_AURA_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper -> {
            helper.register(FIRE_AURA_ID, new RangedAttribute("圣火灵气等级", 0.0, 0.0, 10.0).setSyncable(true));
            helper.register(FREEZE_AURA_ID, new RangedAttribute("神圣冰冻灵气等级", 0.0, 0.0, 10.0).setSyncable(true));
            helper.register(SHOCK_AURA_ID, new RangedAttribute("神圣冲击灵气等级", 0.0, 0.0, 10.0).setSyncable(true));
        });
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, FIRE_AURA);
        event.add(EntityType.PLAYER, FREEZE_AURA);
        event.add(EntityType.PLAYER, SHOCK_AURA);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;

        refreshAura(player, player.getAttributeValue(FIRE_AURA), "twilightdelight:fire_range");
        refreshAura(player, player.getAttributeValue(FREEZE_AURA), "twilightdelight:frozen_range");
        refreshAura(player, player.getAttributeValue(SHOCK_AURA), "irons_spellbooks:thunderstorm");
    }

    private static void refreshAura(Player player, double level, String effectId) {
        if (level < 1.0) return;
        int amp = Math.max(0, (int) Math.floor(level) - 1);
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectId))
                .ifPresent(h -> player.addEffect(new MobEffectInstance(h, 60, amp, true, false)));
    }
}