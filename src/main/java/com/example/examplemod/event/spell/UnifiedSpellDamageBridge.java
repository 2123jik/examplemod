package com.example.examplemod.event.spell;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModAttributes;
import com.example.examplemod.registry.ModMobEffects;
import com.example.examplemod.util.DamageEngine;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class UnifiedSpellDamageBridge {

    // 属性注册表 Holder 缓存
    private static final Map<ResourceLocation, Holder<Attribute>> ATTR_HOLDER_CACHE = new HashMap<>();

    public static Holder<Attribute> getResistanceAttributeFor(SchoolType school) {
        ResourceLocation schoolId = school.getId();
        ResourceLocation resistId = ResourceLocation.fromNamespaceAndPath(
                schoolId.getNamespace(),
                schoolId.getPath() + "_magic_resist"
        );
        return ATTR_HOLDER_CACHE.computeIfAbsent(resistId, loc -> BuiltInRegistries.ATTRIBUTE.getHolder(loc).orElse(null));
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        Entity directEntity = source.getDirectEntity();
        Entity owner = source.getEntity();

        SchoolType initialSchool = null;
        boolean isNativeSpell = false;

        // 1. 判定初始学派
        // 铁魔法法术（包括其烟花、尖牙法术）：无损提取原生学派！
        if (source instanceof SpellDamageSource spellSource) {
            initialSchool = spellSource.spell().getSchoolType();
            isNativeSpell = true;
        }

        // 外部实体查表（原版唤魔者尖牙、灾变实体、暮色实体等在此精准拦截！）
        if (initialSchool == null && directEntity != null) {
            Supplier<SchoolType> s = Init.ENTITY_TO_SCHOOL.get(BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType()));
            if (s != null) initialSchool = s.get();
        }

        // 外部伤害类型查表
        if (initialSchool == null) {
            ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
            if (key != null) {
                Supplier<SchoolType> s = Init.DAMAGE_TO_SCHOOL.get(key);
                if (s != null) initialSchool = s.get();
            }
        }

        // 原版元素 Tag 兜底
        if (initialSchool == null) {
            if (source.is(DamageTypeTags.IS_FIRE)) initialSchool = SchoolRegistry.FIRE.get();
            else if (source.is(DamageTypeTags.IS_FREEZING)) initialSchool = SchoolRegistry.ICE.get();
            else if (source.is(DamageTypeTags.IS_LIGHTNING)) initialSchool = SchoolRegistry.LIGHTNING.get();
        }

        // 起始 ID（没有任何魔法特征则为物理）
        String initialSchoolId = (initialSchool != null)
                ? initialSchool.getId().getPath()
                : ModAttributes.PHYSICAL_ID;


        // 3. 执行统一 DAG 转化与计算
        DamageEngine.SolveResult result =
                DamageEngine.solveAndCalculate(
                        event.getAmount(),
                        initialSchoolId,
                        owner,
                        target,
                        isNativeSpell
                );


// 无魔法分类且没有最终魔法切片，保留原伤害。
        if (initialSchool == null && result.activeFinalSchools().isEmpty()) {
            return;
        }

        ShredResult envShred =
                calculateEnvironmentalShred(target, source);

        if (envShred.school() != null) {
            for (SchoolType activeSchool : result.activeFinalSchools()) {
                if (activeSchool != envShred.school()) {
                    continue;
                }

                if (envShred.flat() > 0.0F) {
                    applyShredEffect(
                            target,
                            activeSchool,
                            ModMobEffects.ShredType.FLAT,
                            envShred.flat()
                    );
                }

                if (envShred.total() > 0.0F) {
                    applyShredEffect(
                            target,
                            activeSchool,
                            ModMobEffects.ShredType.TOTAL,
                            envShred.total()
                    );
                }
            }
        }
        event.setAmount(result.totalDamage());
    }

    private static void applyShredEffect(LivingEntity target, SchoolType school, ModMobEffects.ShredType type, float shredValue) {
        var effectHolder = ModMobEffects.getEffect(school, type);
        if (effectHolder == null) return;

        int newAmp = Math.clamp((int) Math.round(shredValue * 100.0f) - 1, 0, 255);
        MobEffectInstance current = target.getEffect(effectHolder);

        if (current != null && current.getAmplifier() >= newAmp && current.getDuration() >= 8) {
            return;
        }

        target.addEffect(new MobEffectInstance(effectHolder, 20, newAmp, false, false, true));
    }

    private static ShredResult calculateEnvironmentalShred(LivingEntity target, DamageSource source) {
        float flat = 0.0F;
        float total = 0.0F;
        SchoolType school = null;

        if (source.is(DamageTypes.LAVA) || target.isInLava()) {
            school = SchoolRegistry.FIRE.get();
            flat = 0.40F;
            total = 0.20F;
        } else if (source.is(DamageTypeTags.IS_FIRE)
                && target.isOnFire()) {
            school = SchoolRegistry.FIRE.get();
            flat = 0.25F;
        } else if (source.is(DamageTypes.HOT_FLOOR)) {
            school = SchoolRegistry.FIRE.get();
            flat = 0.15F;
        } else if (source.is(DamageTypes.FREEZE)) {
            school = SchoolRegistry.ICE.get();

            if (target.isFullyFrozen()) {
                flat = 0.35F;
                total = 0.15F;
            } else if (target.getTicksFrozen() > 0) {
                flat = 0.20F;
            }
        } else if (source.is(DamageTypeTags.IS_LIGHTNING)
                && target.isInWaterOrRain()) {
            school = SchoolRegistry.LIGHTNING.get();
            flat = 0.40F;
        }
        // 4. 自然环境（溺亡、脱水、中毒）
        else if (source.is(DamageTypes.DROWN) || (target.isUnderWater() && target.getAirSupply() <= 0)) {
            flat = 0.35f;
        } else if (source.is(DamageTypes.DRY_OUT)) {
            flat = 0.40f;
        } else if (source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("neoforge:poison")))
                || (source.is(DamageTypes.MAGIC) && target.hasEffect(MobEffects.POISON))) {
            flat = 0.25f;
        } else if (source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.CACTUS)) {
            flat = 0.15f;
        }
        // 5. 灵魂环境（凋零侵蚀）
        else if (source.is(DamageTypes.WITHER) || target.hasEffect(MobEffects.WITHER)) {
            flat = 0.35f;
        }
        // 6. 鲜血（饥饿致死）
        else if (source.is(DamageTypes.STARVE)) {
            flat = 0.40f;
        }
        // 7. 末影（世界边界、龙息）
        else if (source.is(DamageTypes.OUTSIDE_BORDER)) {
            flat = 0.50f;
            total = 0.20f;
        } else if (source.is(DamageTypes.DRAGON_BREATH)) {
            flat = 0.25f;
        }
        // 8. 诡秘（虚空深渊、幽匿声波）
        else if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            flat = 0.60f;
            total = 0.30f;
        } else if (source.is(DamageTypes.SONIC_BOOM)) {
            total = 0.15f;
        }
        // 9. 沙漠/大地（窒息活埋、落石钟乳石）
        else if (source.is(DamageTypes.IN_WALL)) {
            flat = 0.35f;
        } else if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE)) {
            flat = 0.25f;
        }

        return new ShredResult(school, flat, total);
    }

    private record ShredResult(
            SchoolType school,
            float flat,
            float total
    ) {}
}