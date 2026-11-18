package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
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
public class ElementalShredAttribute {

    // 如果你的 *_shred_flat 每级=1% 抗性削减就填 1；如果每级=10% 抗性削减就把 DIVISOR 改成 10.0
    private static final double AMP_DIVISOR = 1.0;
    private static final int DURATION_TICKS = 100; // 5秒

    public static final ResourceLocation FIRE_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "fire_penetration");
    public static final ResourceLocation ICE_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "ice_penetration");
    public static final ResourceLocation LIGHTNING_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "lightning_penetration");
    public static final ResourceLocation POISON_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "poison_penetration");

    public static final DeferredHolder<Attribute, Attribute> FIRE = DeferredHolder.create(Registries.ATTRIBUTE, FIRE_ID);
    public static final DeferredHolder<Attribute, Attribute> ICE = DeferredHolder.create(Registries.ATTRIBUTE, ICE_ID);
    public static final DeferredHolder<Attribute, Attribute> LIGHTNING = DeferredHolder.create(Registries.ATTRIBUTE, LIGHTNING_ID);
    public static final DeferredHolder<Attribute, Attribute> POISON = DeferredHolder.create(Registries.ATTRIBUTE, POISON_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper -> {
            helper.register(FIRE_ID, new RangedAttribute("% 降低敌人火焰抗性", 0.0, 0.0, 500.0).setSyncable(true));
            helper.register(ICE_ID, new RangedAttribute("% 降低敌人冰冷抗性", 0.0, 0.0, 500.0).setSyncable(true));
            helper.register(LIGHTNING_ID, new RangedAttribute("% 降低敌人闪电抗性", 0.0, 0.0, 500.0).setSyncable(true));
            helper.register(POISON_ID, new RangedAttribute("% 降低敌人毒素抗性", 0.0, 0.0, 500.0).setSyncable(true));
        });
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, FIRE);
        event.add(EntityType.PLAYER, ICE);
        event.add(EntityType.PLAYER, LIGHTNING);
        event.add(EntityType.PLAYER, POISON);
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity target = event.getEntity();
        if (target == player || event.getNewDamage() <= 0 || !target.isAlive()) return;

        applyShred(target, player.getAttributeValue(FIRE), "examplemod:fire_shred_flat");
        applyShred(target, player.getAttributeValue(ICE), "examplemod:ice_shred_flat");
        applyShred(target, player.getAttributeValue(LIGHTNING), "examplemod:lightning_shred_flat");
        applyShred(target, player.getAttributeValue(POISON), "examplemod:nature_shred_flat");
    }

    private static void applyShred(LivingEntity target, double value, String effectId) {
        if (value <= 0) return;
        int amp = Math.max(0, (int) Math.round(value / AMP_DIVISOR) - 1);
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectId))
                .ifPresent(h -> target.addEffect(new MobEffectInstance(h, DURATION_TICKS, amp)));
    }
}