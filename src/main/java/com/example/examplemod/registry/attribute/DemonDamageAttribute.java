package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class DemonDamageAttribute {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "damage_to_demons");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("% 对恶魔伤害", 0.0, 0.0, 5000.0).setSyncable(true))
        );
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!isDemon(event.getEntity())) return;

        double percent = player.getAttributeValue(ATTRIBUTE);
        if (percent > 0) {
            event.setAmount(event.getAmount() * (1.0f + (float) (percent / 100.0)));
        }
    }

    private static boolean isDemon(LivingEntity target) {
        if (target.fireImmune() && !target.getType().is(EntityTypeTags.UNDEAD)) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(Locale.ROOT);
        return id.contains("piglin") || id.contains("hoglin") || id.contains("ghast")
                || id.contains("blaze") || id.contains("demon") || id.contains("devil")
                || id.contains("imp") || id.contains("hell") || id.contains("infernal");
    }
}