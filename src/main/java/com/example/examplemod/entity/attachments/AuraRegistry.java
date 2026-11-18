package com.example.examplemod.entity.attachments;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.effect.OakskinData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.holy.HasteSpell;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AuraRegistry {

    /**
     * 预留倍率：默认 1.0f 表示 1:1 直接锁定 spell.getManaCost(effectiveLevel)
     * 觉得后期锁蓝太少可以调成 1.5f 或 2.0f
     */
    public static final float RESERVE_MULTIPLIER = 1.0F;

    @FunctionalInterface
    public interface AmplifierCalculator {
        int calculate(AbstractSpell spell, int effectiveLevel, LivingEntity caster);
    }

    public record AuraSpec(
            Holder<MobEffect> effect,
            AmplifierCalculator amplifierCalculator,
            Consumer<LivingEntity> onRemoveCleanup
    ) {
        public AuraSpec(Holder<MobEffect> effect, AmplifierCalculator amplifierCalculator) {
            this(effect, amplifierCalculator, entity -> {});
        }
    }

    private static final Map<ResourceLocation, AuraSpec> AURAS = new HashMap<>();

    static {
        // 1. 充能：amplifier = level - 1
        register("irons_spellbooks", "charge",
                new AuraSpec(MobEffectRegistry.CHARGED, (spell, level, caster) -> Math.max(0, level - 1)));

        // 2. 急迫：amplifier 直接调用 HasteSpell#getAmplifier（随法强倍率走）
        register("irons_spellbooks", "haste",
                new AuraSpec(MobEffectRegistry.HASTENED, (spell, level, caster) -> {
                    if (spell instanceof HasteSpell hasteSpell) {
                        return hasteSpell.getAmplifier(level, caster);
                    }
                    return (int) (7.0F * spell.getEntityPowerMultiplier(caster));
                }));

        // 3. 橡树皮肤：amplifier 固定 2；关闭时清理 OakskinData
        register("irons_spellbooks", "oakskin",
                new AuraSpec(MobEffectRegistry.OAKSKIN,
                        (spell, level, caster) -> 2,
                        OakskinData::remove));

        // 4. 蜘蛛形态：amplifier = level - 1
        register("irons_spellbooks", "spider_aspect",
                new AuraSpec(MobEffectRegistry.SPIDER_ASPECT, (spell, level, caster) -> Math.max(0, level - 1)));

        // 5. 位面视觉：amplifier = level - 1
        register("irons_spellbooks", "planar_sight",
                new AuraSpec(MobEffectRegistry.PLANAR_SIGHT, (spell, level, caster) -> Math.max(0, level - 1)));

        // 6. 暴食：amplifier = level - 1（自身蓝耗恒为35，但等级加成依然强化回蓝比率）
        register("irons_spellbooks", "gluttony",
                new AuraSpec(MobEffectRegistry.GLUTTONY, (spell, level, caster) -> Math.max(0, level - 1)));

        // 7. 天使之翼：amplifier 固定 0
        register("irons_spellbooks", "angel_wing",
                new AuraSpec(MobEffectRegistry.ANGEL_WINGS, (spell, level, caster) -> 0));

        // 8. 真隐身：amplifier 固定 0
        register("irons_spellbooks", "invisibility",
                new AuraSpec(MobEffectRegistry.TRUE_INVISIBILITY, (spell, level, caster) -> 0));
    }

    private static void register(String namespace, String path, AuraSpec spec) {
        AURAS.put(ResourceLocation.fromNamespaceAndPath(namespace, path), spec);
    }

    @Nullable
    public static AuraSpec get(ResourceLocation spellId) {
        return AURAS.get(spellId);
    }

    public static boolean isAuraSpell(ResourceLocation spellId) {
        return AURAS.containsKey(spellId);
    }
}