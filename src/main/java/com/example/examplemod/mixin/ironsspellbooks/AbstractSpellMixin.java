package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.event.explosion.ISchoolHolder;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(targets = "io.redspace.ironsspellbooks.api.spells.AbstractSpell")
public abstract class AbstractSpellMixin {

    // 缓存反射方法，弹幕命中时纳秒级完成取值
    @Unique
    private static Method shockwave$getSchoolMethod = null;

    // 拦截 AbstractSpell 源码第 380 行：getDamageSource(Entity projectile, Entity attacker)
    @Inject(
            method = "getDamageSource(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lio/redspace/ironsspellbooks/damage/SpellDamageSource;",
            at = @At("HEAD")
    )
    private void onGetDamageSource(Entity projectile, Entity attacker, CallbackInfoReturnable<?> cir) {
        if (projectile instanceof ISchoolHolder holder) {
            int color = shockwave$extractSchoolColor(this);
            if (color != -1) {
                holder.setSchoolColor(color);
            }
        }
    }

    @Unique
    private static int shockwave$extractSchoolColor(Object spell) {
        try {
            // 1. 从当前 spell 对象身上安全获取 SchoolType（无需 @Shadow，零签名冲突）
            if (shockwave$getSchoolMethod == null) {
                shockwave$getSchoolMethod = spell.getClass().getMethod("getSchoolType");
                shockwave$getSchoolMethod.setAccessible(true);
            }
            Object schoolType = shockwave$getSchoolMethod.invoke(spell);

            // 2. 从 SchoolType 中读取 displayStyle 颜色
            if (schoolType != null) {
                Field f = schoolType.getClass().getDeclaredField("displayStyle");
                f.setAccessible(true);
                Object style = f.get(schoolType);
                if (style instanceof net.minecraft.network.chat.Style s && s.getColor() != null) {
                    return s.getColor().getValue();
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }
}