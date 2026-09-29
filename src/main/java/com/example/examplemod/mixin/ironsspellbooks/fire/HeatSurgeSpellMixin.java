package com.example.examplemod.mixin.ironsspellbooks.fire;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.fire.HeatSurgeSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeatSurgeSpell.class, remap = false)
public class HeatSurgeSpellMixin {

    private static final ResourceLocation CINDER_SHRED_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "cinder_resist_shred");

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectCinderDomain(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var domainAttr = entity.getAttribute(ModAttributes.HEAT_SURGE_CINDER_DOMAIN.getDelegate());
        if (domainAttr != null && domainAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            Vec3 domainCenter = entity.position();
            float radius = ((HeatSurgeSpell) (Object) this).getRadius(spellLevel, entity);

            // 持续 10 秒（200 刻），每 20 刻执行一次抗性侵蚀与粒子渲染
            for (int tick = 0; tick < 200; tick += 20) {
                final int currentTick = tick;
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(tick, () -> {
                    // 焦土粒子边缘线
                    serverLevel.sendParticles(ParticleTypes.FLAME, domainCenter.x, domainCenter.y + 0.1, domainCenter.z, 20, radius * 0.7, 0.1, radius * 0.7, 0.02);
                    serverLevel.sendParticles(ParticleHelper.EMBERS, domainCenter.x, domainCenter.y + 0.5, domainCenter.z, 15, radius * 0.5, 0.3, radius * 0.5, 0.05);

                    AABB domainBox = new AABB(domainCenter, domainCenter).inflate(radius);
                    for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, domainBox)) {
                        if (victim != entity && !victim.isAlliedTo(entity)) {
                            // 动态叠加强制法术抗性削减
                            AttributeInstance resist = victim.getAttribute(AttributeRegistry.SPELL_RESIST);
                            if (resist != null) {
                                double currentPenalty = -0.05 * Math.min(8, (currentTick / 20) + 1);
                                resist.removeModifier(CINDER_SHRED_ID);
                                resist.addTransientModifier(new AttributeModifier(CINDER_SHRED_ID, currentPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                            }
                        }
                    }
                }));
            }
        }
    }
}