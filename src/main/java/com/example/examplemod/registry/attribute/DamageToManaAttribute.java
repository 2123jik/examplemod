package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class DamageToManaAttribute {

    // 填 PD2 原版百分比(如 15.0)用 3.5 (受击掉10血约回5蓝)；若你在生成器填 50.0，改为 1.0
    private static final double MANA_SCALE = 3.5;

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "damage_to_mana");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("% 受损的生命转换成法力", 0.0, 0.0, 1000.0).setSyncable(true))
        );
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;

        float damageTaken = event.getNewDamage();
        if (damageTaken <= 0) return;

        double percent = player.getAttributeValue(ATTRIBUTE);
        if (percent <= 0) return;

        double manaGain = damageTaken * (percent / 100.0) * MANA_SCALE;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float newMana = (float) Math.min(maxMana, magicData.getMana() + manaGain);
        if (newMana > magicData.getMana()) {
            magicData.setMana(newMana);
        }
    }
}