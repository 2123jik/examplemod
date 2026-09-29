package com.example.examplemod.mixin.ironsspellbooks.blood;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import io.redspace.ironsspellbooks.spells.blood.BloodSlashSpell;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BloodSlashSpell.class, remap = false)
public class BloodSlashSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectCrossSlash(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var crossAttr = entity.getAttribute(ModAttributes.BLOOD_SLASH_CROSS.getDelegate());
        if (crossAttr != null && crossAttr.getValue() >= 1.0) {
            // 拦截原版单发，发射交叉双重血弧
            float damage = ((BloodSlashSpell) (Object) this).getSpellPower(spellLevel, entity) * 0.85f;
            Vec3 look = entity.getLookAngle();

            // 左右各偏转 20 度的双回旋飞刃
            float[] angles = new float[]{-20.0f, 20.0f};
            for (float angle : angles) {
                BloodSlashProjectile slash = new BloodSlashProjectile(world, entity);
                slash.setPos(entity.getEyePosition());
                
                // 计算偏转后的水平向量
                float rad = angle * Mth.DEG_TO_RAD;
                Vec3 rotatedAim = look.yRot(rad);
                
                slash.shoot(rotatedAim);
                slash.setDamage(damage);
                world.addFreshEntity(slash);
            }

            ci.cancel(); // 阻断原版单发逻辑
        }
    }
}