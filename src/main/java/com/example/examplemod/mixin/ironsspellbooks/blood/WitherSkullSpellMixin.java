package com.example.examplemod.mixin.ironsspellbooks.blood;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.WitherSkullProjectile;
import io.redspace.ironsspellbooks.spells.blood.WitherSkullSpell;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WitherSkullSpell.class, remap = false)
public class WitherSkullSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectWitherBarrage(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var barrageAttr = entity.getAttribute(ModAttributes.WITHER_BARRAGE.getDelegate());
        if (barrageAttr != null && barrageAttr.getValue() >= 1.0) {
            float speed = (6 + spellLevel) * .08f;
            float damage = ((WitherSkullSpell) (Object) this).getSpellPower(spellLevel, entity) * 0.4f;

            // 扇形发射 3 枚血颅
            int[] yawOffsets = new int[]{-15, 0, 15};
            for (int offset : yawOffsets) {
                Vec3 shootDir = entity.getLookAngle().yRot(offset * Mth.DEG_TO_RAD);
                Vec3 spawn = entity.getEyePosition().add(shootDir);

                WitherSkullProjectile skull = new WitherSkullProjectile(level, entity);
                skull.setDamage(damage);
                skull.speed = speed;
                skull.moveTo(spawn.x, spawn.y - skull.getBoundingBox().getYsize() / 2, spawn.z, entity.getYRot() + 180 + offset, entity.getXRot());
                skull.shoot(shootDir);
                level.addFreshEntity(skull);
            }

            ci.cancel(); // 阻断单发
        }
    }
}