package com.example.examplemod.mixin.ironsspellbooks.nature;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.acid_orb.AcidOrb;
import io.redspace.ironsspellbooks.spells.nature.AcidOrbSpell;
import net.minecraft.core.particles.ParticleTypes;
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

@Mixin(value = AcidOrbSpell.class, remap = false)
public class AcidOrbSpellMixin {

    private static final ResourceLocation ACID_SHRED_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "acid_corrosion_shred");

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectCorrosionZone(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var acidAttr = entity.getAttribute(ModAttributes.ACID_ORB_CORROSION.getDelegate());
        if (acidAttr != null && acidAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            // 获取刚发射出的 AcidOrb 实体打上强酸标记
            serverLevel.getEntitiesOfClass(AcidOrb.class, entity.getBoundingBox().inflate(6.0), orb -> orb.getOwner() == entity)
                    .forEach(orb -> {
                        // 延迟在着弹点铺设强酸死区
                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(20, () -> {
                            Vec3 impactPos = orb.position();
                            float zoneRadius = 5.0f;

                            serverLevel.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 2.5f, 0.6f);

                            // 持续 6 秒（120 刻），每 10 刻执行一次强酸腐蚀判定
                            for (int tick = 0; tick < 120; tick += 10) {
                                serverLevel.getServer().tell(new net.minecraft.server.TickTask(tick, () -> {
                                    serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, impactPos.x, impactPos.y + 0.2, impactPos.z, 25, zoneRadius * 0.5, 0.1, zoneRadius * 0.5, 0.05);

                                    AABB zoneBox = new AABB(impactPos, impactPos).inflate(zoneRadius);
                                    for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, zoneBox, e -> e != entity && !e.isAlliedTo(entity))) {
                                        // 永久削减 50% 全法术抗性 (Spell Resist)
                                        AttributeInstance resist = victim.getAttribute(AttributeRegistry.SPELL_RESIST);
                                        if (resist != null) {
                                            resist.removeModifier(ACID_SHRED_ID);
                                            resist.addTransientModifier(new AttributeModifier(ACID_SHRED_ID, -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                                        }

                                        // 标记强酸易伤状态
                                        victim.getPersistentData().putInt("AcidCorrosionZoneTimer", 30);
                                    }
                                }));
                            }
                        }));
                    });
        }
    }
}