package com.example.examplemod.mixin.ironsspellbooks.ice;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.BlizzardAoe;
import io.redspace.ironsspellbooks.spells.ice.BlizzardSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlizzardSpell.class, remap = false)
public class BlizzardSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectPermafrostAbyss(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var permafrostAttr = entity.getAttribute(ModAttributes.BLIZZARD_PERMAFROST.getDelegate());
        if (permafrostAttr != null && permafrostAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            // 获取刚召唤出的 BlizzardAoe 实体
            serverLevel.getEntitiesOfClass(BlizzardAoe.class, entity.getBoundingBox().inflate(36.0), aoe -> aoe.getOwner() == entity)
                    .forEach(blizzard -> {
                        // 1. 取消漂移，将其钉死在原地形成绝对风暴眼
                        blizzard.setDeltaMovement(Vec3.ZERO);

                        Vec3 center = blizzard.position();
                        float radius = blizzard.getRadius();
                        int lifetime = blizzard.getDuration();

                        // 2. 调度持续的引力潮汐（每 4 刻将边缘敌人拉向暴风眼）
                        for (int t = 0; t < lifetime; t += 4) {
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(t, () -> {
                                if (!blizzard.isAlive()) return;
                                AABB pullBox = new AABB(center, center).inflate(radius + 1.5);
                                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, pullBox)) {
                                    if (victim != entity && !victim.isAlliedTo(entity)) {
                                        // 计算朝向中心的强力寒流吸扯向量
                                        Vec3 pull = center.subtract(victim.position()).normalize().scale(0.28);
                                        victim.setDeltaMovement(victim.getDeltaMovement().add(pull));
                                        victim.hurtMarked = true;
                                    }
                                }
                            }));
                        }
                    });
        }
    }
}