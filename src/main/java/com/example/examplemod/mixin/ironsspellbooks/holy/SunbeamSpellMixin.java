package com.example.examplemod.mixin.ironsspellbooks.holy;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.holy.SunbeamSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SunbeamSpell.class, remap = false)
public class SunbeamSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectDawnFlare(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var flareAttr = entity.getAttribute(ModAttributes.SUNBEAM_DAWN_FLARE.getDelegate());
        if (flareAttr != null && flareAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            Vec3 flareOrigin = entity.position().add(entity.getForward().scale(4.0));
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                var pos = castTargetingData.getTargetPosition(serverLevel);
                if (pos != null) flareOrigin = pos;
            }

            final Vec3 center = flareOrigin;
            float holyPower = (float) entity.getAttributeValue(AttributeRegistry.HOLY_SPELL_POWER);

            // 持续 5 秒（100 刻），每 10 刻执行一次风暴判决
            for (int tick = 0; tick < 100; tick += 10) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(tick, () -> {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2, center.z, 25, 2.5, 0.2, 2.5, 0.1);
                    serverLevel.sendParticles(ParticleHelper.WISP, center.x, center.y + 1.0, center.z, 15, 2.0, 1.0, 2.0, 0.05);

                    AABB stormBox = new AABB(center, center).inflate(4.0);

                    // 1. 敌对惩戒：使用 1.21 标准 EntityTypeTags.UNDEAD 判定，同时兼容假人与原版亡灵
                    for (LivingEntity enemy : serverLevel.getEntitiesOfClass(LivingEntity.class, stormBox, e -> e != entity && !e.isAlliedTo(entity))) {
                        float tickDamage = holyPower * 0.4f;

                        // 严格匹配 1.21 亡灵生物判定标准（包括打测试假人、僵尸、骷髅等）
                        boolean isUndead = enemy.getType().is(EntityTypeTags.UNDEAD) 
                                || enemy.isInvertedHealAndHarm(); // 双重保险：支持反转治疗与伤害的亡灵/假人机制

                        if (isUndead) {
                            tickDamage *= 2.0f; // 亡灵/反转生物享受 200% 圣裁真实暴击
                        }

                        var src = ((SunbeamSpell) (Object) this).getDamageSource(entity);
                        src.setIFrames(0); // 清零受击无敌帧，确保高频跳字完全命中
                        DamageSources.applyDamage(enemy, tickDamage, src);
                    }

                    // 2. 友方滋养：持续恢复法力
                    for (LivingEntity ally : serverLevel.getEntitiesOfClass(LivingEntity.class, stormBox, a -> a == entity || a.isAlliedTo(entity))) {
                        if (ally instanceof ServerPlayer allyPlayer) {
                            MagicData allyMagic = MagicData.getPlayerMagicData(allyPlayer);
                            allyMagic.addMana(holyPower * 0.15f);
                        }
                    }
                }));
            }
        }
    }
}