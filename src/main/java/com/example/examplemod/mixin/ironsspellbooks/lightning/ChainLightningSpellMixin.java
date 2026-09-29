package com.example.examplemod.mixin.ironsspellbooks.lightning;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import io.redspace.ironsspellbooks.spells.lightning.ChainLightningSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;

@Mixin(value = ChainLightningSpell.class, remap = false)
public class ChainLightningSpellMixin {

    private static final ResourceLocation SUPERCONDUCTOR_SHRED = ResourceLocation.fromNamespaceAndPath("examplemod", "superconductor_shred");

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectSuperconductor(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var condAttr = entity.getAttribute(ModAttributes.CHAIN_LIGHTNING_SUPERCONDUCTOR.getDelegate());
        if (condAttr != null && condAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
                var firstTarget = targetData.getTarget(serverLevel);
                if (firstTarget != null) {
                    ci.cancel(); // 阻断原版单向单次链条

                    int maxBounces = ((ChainLightningSpell) (Object) this).getMaxConnections(spellLevel, entity);
                    float baseDamage = ((ChainLightningSpell) (Object) this).getDamage(spellLevel, entity);
                    double searchRadius = 10.0;

                    // 检索有效连击目标池
                    AABB box = firstTarget.getBoundingBox().inflate(searchRadius);
                    List<LivingEntity> pool = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != entity && !e.isAlliedTo(entity) && e.isAlive());
                    if (!pool.contains(firstTarget)) pool.add(firstTarget);

                    // 折返闭环弹跳循环
                    LivingEntity currentSource = entity;
                    for (int i = 0; i < maxBounces; i++) {
                        final int bounceIndex = i;
                        final LivingEntity src = currentSource;
                        // 循环选择目标实现来回交替电击
                        final LivingEntity nextTarget = pool.get(bounceIndex % pool.size());
                        currentSource = nextTarget;

                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(bounceIndex * 2, () -> {
                            if (!nextTarget.isAlive()) return;

                            // 1. 每跳增伤 20%
                            float bounceDamage = baseDamage * (1.0f + (bounceIndex * 0.20f));

                            var dmgSrc = ((ChainLightningSpell) (Object) this).getDamageSource(entity);
                            dmgSrc.setIFrames(0); // 清零无敌帧
                            DamageSources.applyDamage(nextTarget, bounceDamage, dmgSrc);

                            // 2. 电弧高压粒子连接
                            Vec3 from = src.getBoundingBox().getCenter();
                            Vec3 to = nextTarget.getBoundingBox().getCenter();
                            serverLevel.sendParticles(new ZapParticleOption(to), from.x, from.y, from.z, 1, 0, 0, 0, 0);
                            serverLevel.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.6f + (bounceIndex * 0.05f));

                            // 3. 剥离 10% 全法术抗性（至多叠加 5 层）
                            AttributeInstance resist = nextTarget.getAttribute(AttributeRegistry.SPELL_RESIST);
                            if (resist != null) {
                                double currentPenalty = -0.10 * Math.min(5, (bounceIndex / 2) + 1);
                                resist.removeModifier(SUPERCONDUCTOR_SHRED);
                                resist.addTransientModifier(new AttributeModifier(SUPERCONDUCTOR_SHRED, currentPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                            }
                        }));
                    }
                }
            }
        }
    }
}