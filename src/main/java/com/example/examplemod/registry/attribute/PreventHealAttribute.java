package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class PreventHealAttribute {

    private static final String TAG_NO_HEAL_UNTIL = "examplemod_no_heal_until";
    private static final int DURATION_TICKS = 200; // 10秒禁疗

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "prevent_heal");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("防止目标自疗 (≥1生效)", 0.0, 0.0, 10.0).setSyncable(true))
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
        if (target == player || event.getNewDamage() <= 0) return;

        if (player.getAttributeValue(ATTRIBUTE) >= 1.0) {
            target.getPersistentData().putLong(TAG_NO_HEAL_UNTIL, target.level().getGameTime() + DURATION_TICKS);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        long until = entity.getPersistentData().getLong(TAG_NO_HEAL_UNTIL);
        if (until > entity.level().getGameTime()) {
            event.setCanceled(true);
        }
    }
}