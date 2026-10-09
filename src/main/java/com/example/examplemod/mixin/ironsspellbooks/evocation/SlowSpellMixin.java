package com.example.examplemod.mixin.ironsspellbooks.evocation;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.evocation.SlowSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SlowSpell.class, remap = false)
public class SlowSpellMixin {

    private static final ResourceLocation CHRONO_DEBUFF = ResourceLocation.fromNamespaceAndPath("examplemod", "chrono_stagnation");

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectChronoStasis(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var chronoAttr = entity.getAttribute(ModAttributes.SLOW_CHRONO_STAGNATION.getDelegate());
        if (chronoAttr != null && chronoAttr.getValue() >= 1.0) {
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
                var targetEntity = targetData.getTarget((ServerLevel) world);
                if (targetEntity != null) {
                    // 1. 大幅削减目标的施法速度（延长 200% 吟唱时间）
                    AttributeInstance castSpeed = targetEntity.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
                    if (castSpeed != null) {
                        castSpeed.removeModifier(CHRONO_DEBUFF);
                        castSpeed.addTransientModifier(new AttributeModifier(CHRONO_DEBUFF, -2.0, AttributeModifier.Operation.ADD_VALUE));
                    }

                    // 2. 如果目标是玩家或施法生物，冻结其 CD
                    if (targetEntity instanceof ServerPlayer enemyPlayer) {
                        MagicData enemyMagic = MagicData.getPlayerMagicData(enemyPlayer);
                        // 冻结当前冷却队列（暂停流逝 6 秒）
                        enemyMagic.getPlayerCooldowns().getSpellCooldowns().forEach((id, cd) -> {
                            enemyMagic.getPlayerCooldowns().addCooldown(id, cd.getSpellCooldown() + 120);
                        });
                    }
                }
            }
        }
    }
}