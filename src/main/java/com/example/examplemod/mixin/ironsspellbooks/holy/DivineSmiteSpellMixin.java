package com.example.examplemod.mixin.ironsspellbooks.holy;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.holy.DivineSmiteSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DivineSmiteSpell.class, remap = false)
public class DivineSmiteSpellMixin {

    private static final ResourceLocation JUDGMENT_SHRED_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "smite_judgment_shred");

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectJudgmentDebuff(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var judgeAttr = entity.getAttribute(ModAttributes.DIVINE_SMITE_JUDGMENT.getDelegate());
        if (judgeAttr != null && judgeAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            // 延迟 2 刻检测判定区内的敌人（受击目标）
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(2, () -> {
                var victims = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.5), e -> e != entity && !e.isAlliedTo(entity));
                for (LivingEntity victim : victims) {
                    // 1. 削减 35% 全法术抗性 (Spell Resist)
                    AttributeInstance resist = victim.getAttribute(AttributeRegistry.SPELL_RESIST);
                    if (resist != null) {
                        resist.removeModifier(JUDGMENT_SHRED_ID);
                        resist.addTransientModifier(new AttributeModifier(JUDGMENT_SHRED_ID, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }

                    // 2. 标记圣裁状态（持续 8 秒 = 160 刻），供伤害事件做以战养疗转化
                    victim.getPersistentData().putUUID("JudgedByCaster", entity.getUUID());
                    victim.getPersistentData().putInt("JudgedTimer", 160);
                }
            }));
        }
    }
}