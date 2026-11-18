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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ManaStealAttribute {

    // 打出 25 点武器伤害 + 7% 吸蓝 -> 25 * 0.07 * 4.0 = 7 点铁魔法法力
    private static final double MANA_SCALE = 4.0;

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "mana_steal");
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper ->
                helper.register(ID, new RangedAttribute("% 击中时偷取法力", 0.0, 0.0, 1000.0).setSyncable(true))
        );
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        if (event.getEntity() == player) return;

        // 限制只对直接攻击(近战)或箭矢生效，不让法术无限自充
        var direct = event.getSource().getDirectEntity();
        if (direct != player && !(direct instanceof AbstractArrow)) return;

        float damageDealt = event.getNewDamage();
        if (damageDealt <= 0) return;

        double percent = player.getAttributeValue(ATTRIBUTE);
        if (percent <= 0) return;

        double manaGain = damageDealt * (percent / 100.0) * MANA_SCALE;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float newMana = (float) Math.min(maxMana, magicData.getMana() + manaGain);
        if (newMana > magicData.getMana()) {
            magicData.setMana(newMana);
        }
    }
}