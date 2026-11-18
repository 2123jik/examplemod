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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ManaOnKillAttribute {

    // 填 PD2 原版数值(如 4~6)用 4.0；如果你在生成器里已经手乘过了(如 20)，改为 1.0
    private static final double MANA_SCALE = 4.0;

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "mana_on_kill");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("击杀回复法力", 0.0, 0.0, 10000.0).setSyncable(true))
        );
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;

        double val = player.getAttributeValue(ATTRIBUTE);
        if (val <= 0) return;

        double restore = val * MANA_SCALE;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float newMana = (float) Math.min(maxMana, magicData.getMana() + restore);
        if (newMana > magicData.getMana()) {
            magicData.setMana(newMana);
        }
    }
}