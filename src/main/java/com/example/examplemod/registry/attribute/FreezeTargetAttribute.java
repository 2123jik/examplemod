package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class FreezeTargetAttribute {

    private static final int TOMB_THRESHOLD = 200;

    // 联动效果 ID
    private static final ResourceLocation TF_FROSTED = ResourceLocation.parse("twilightforest:frosted");
    private static final ResourceLocation CATACLYSM_WETNESS = ResourceLocation.parse("cataclysm:wetness");

    // 无法冰冻霸体净化列表
    private static final List<ResourceLocation> FREEZE_EFFECTS = List.of(
            ResourceLocation.parse("minecraft:slowness"),
            ResourceLocation.parse("irons_spellbooks:chilled"),
            ResourceLocation.parse("irons_spellbooks:frostbite"),
            ResourceLocation.parse("irons_spellbooks:slowed"),
            ResourceLocation.parse("l2complements:frost"),
            ResourceLocation.parse("twilightforest:frosted"),
            ResourceLocation.parse("mowziesmobs:frozen"),
            ResourceLocation.parse("simplyswords:freeze")
    );

    // ==================== 1. ID 定义 ====================
    public static final ResourceLocation CANNOT_BE_FROZEN_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "cannot_be_frozen");
    public static final ResourceLocation FREEZE_TARGET_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "freeze_target");
    public static final ResourceLocation FREEZE_COOLDOWN_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "freeze_cooldown");

    // ==================== 2. Holder 绑定 ====================
    public static final DeferredHolder<Attribute, Attribute> CANNOT_BE_FROZEN =
            DeferredHolder.create(Registries.ATTRIBUTE, CANNOT_BE_FROZEN_ID);
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE =
            DeferredHolder.create(Registries.ATTRIBUTE, FREEZE_TARGET_ID);

    // 1.21.1 现代冷却计时器：纯内存瞬态 Attachment，性能极高且不污染存档
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> FREEZE_COOLDOWN =
            DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FREEZE_COOLDOWN_ID);

    // ==================== 3. 注册事件：统一按顺序注册 Attribute 与 Attachment ====================
    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        // 注册属性
        event.register(Registries.ATTRIBUTE, helper -> {
            helper.register(CANNOT_BE_FROZEN_ID, new RangedAttribute("无法冰冻 (≥1生效)", 0.0, 0.0, 10.0).setSyncable(true));
            helper.register(FREEZE_TARGET_ID, new RangedAttribute("附加冰冷值", 0.0, 0.0, 2048.0).setSyncable(true));
        });

        // 注册 1.21.1 数据附加器 (默认值为 0)
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> {
            helper.register(FREEZE_COOLDOWN_ID, AttachmentType.builder(() -> 0).build());
        });
    }

    // ==================== 4. 附加属性 ====================
    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, CANNOT_BE_FROZEN);
        event.add(EntityType.PLAYER, ATTRIBUTE);
    }

    // ==================== 5. 战斗与伤害逻辑 ====================

    // 冰霜法术：保底 7 点 + 施法者属性 (潮湿则翻倍)
    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide() || isImmune(target)) return;

        if (event.getSpellDamageSource() != null && event.getSpellDamageSource().spell() != null) {
            if (event.getSpellDamageSource().spell().getSchoolType() == SchoolRegistry.ICE.get()) {
                int extraFreeze = 0;
                if (event.getSpellDamageSource().getEntity() instanceof LivingEntity caster) {
                    if (caster.getAttributes().hasAttribute(ATTRIBUTE)) {
                        extraFreeze = (int) caster.getAttributeValue(ATTRIBUTE);
                    }
                }
                applyFreezeTicks(target, 7 + extraFreeze);
            }
        }
    }

    // 近战/箭矢：直接增加词条点数 (潮湿则翻倍)
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide() || isImmune(target) || !target.isAlive()) return;
        if (event.getNewDamage() <= 0) return;

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == target) return;

        var direct = event.getSource().getDirectEntity();
        if (direct != attacker && !(direct instanceof AbstractArrow)) return;

        if (!attacker.getAttributes().hasAttribute(ATTRIBUTE)) return;
        int freezeBonus = (int) attacker.getAttributeValue(ATTRIBUTE);
        if (freezeBonus > 0) {
            applyFreezeTicks(target, freezeBonus);
        }
    }

    // ==================== 6. Tick 逻辑（免冻霸体、光环吸收与冰墓冷却） ====================
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity self)) return;
        if (self.level().isClientSide() || !self.isAlive()) return;

        // 1. 免疫霸体生效中：清空冰冻值与减速/冰冻负面状态
        if (isImmune(self)) {
            if (self.getTicksFrozen() > 0) self.setTicksFrozen(0);
            for (ResourceLocation id : FREEZE_EFFECTS) {
                BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(holder -> {
                    if (self.hasEffect(holder)) self.removeEffect(holder);
                });
            }
            return;
        }

        // 2. 联动暮色 Frosted 效果（每秒注入寒气）
        if (self.tickCount % 20 == 0 && !self.isInLava()) {
            BuiltInRegistries.MOB_EFFECT.getHolder(TF_FROSTED).ifPresent(holder -> {
                MobEffectInstance effect = self.getEffect(holder);
                if (effect != null) {
                    int amp = effect.getAmplifier();
                    applyFreezeTicks(self, 15 + (amp * 2));
                }
            });
        }

        // 3. 1.21.1 Data Attachment 冷却判定
        int cooldown = self.getData(FREEZE_COOLDOWN);
        if (cooldown > 0) {
            self.setData(FREEZE_COOLDOWN, cooldown - 1);
            return;
        }

        if (self.getVehicle() instanceof IceTombEntity) return;

        // 4. 冰冻值达到 200 触发冰墓禁锢
        if (self.getTicksFrozen() >= TOMB_THRESHOLD) {
            if (self.isInLava()) {
                self.setTicksFrozen(0);
                return;
            }

            IceTombEntity tomb = new IceTombEntity(self.level(), self);
            tomb.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
            tomb.setDeltaMovement(self.getDeltaMovement());
            tomb.setEvil();

            int duration = 40;
            tomb.setLifetime(duration);

            self.level().addFreshEntity(tomb);
            self.startRiding(tomb, true);

            // 扣除 200 门槛，保留溢出冰冻值
            int overflow = self.getTicksFrozen() - TOMB_THRESHOLD;
            self.setTicksFrozen(Math.max(0, overflow));

            // 出墓后给 40 tick (2秒) 冷却真空期，防止无缝死锁
            self.setData(FREEZE_COOLDOWN, duration + 100);
        }
    }

    // ==================== 辅助方法 ====================
    private static void applyFreezeTicks(LivingEntity target, int amount) {
        if (isWet(target)) {
            amount *= 2;
        }
        target.setTicksFrozen(Math.min(2000, target.getTicksFrozen() + amount));
    }

    private static boolean isWet(LivingEntity entity) {
        if (entity.isInWaterOrRain()) return true;
        return BuiltInRegistries.MOB_EFFECT.getHolder(CATACLYSM_WETNESS)
                .map(entity::hasEffect)
                .orElse(false);
    }

    public static boolean isImmune(LivingEntity entity) {
        if (!entity.canFreeze()) return true;
        return entity.getAttributes().hasAttribute(CANNOT_BE_FROZEN)
                && entity.getAttributeValue(CANNOT_BE_FROZEN) >= 1.0;
    }
}