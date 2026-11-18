package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class HitBlindAttribute {

    private static final ResourceLocation SUNDERING = ResourceLocation.parse("apothic_attributes:sundering");

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "hit_blind");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("% 几率致盲削弱目标", 0.0, 0.0, 100.0).setSyncable(true))
        );
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity target = event.getEntity();
        if (target == player || event.getNewDamage() <= 0 || !target.isAlive()) return;

        double chance = player.getAttributeValue(ATTRIBUTE) / 100.0;
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        BuiltInRegistries.MOB_EFFECT.getHolder(SUNDERING)
                .ifPresent(h -> target.addEffect(new MobEffectInstance(h, 80, 0)));
    }
}