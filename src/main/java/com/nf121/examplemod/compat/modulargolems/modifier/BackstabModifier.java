package com.nf121.examplemod.compat.modulargolems.modifier;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.modulargolems.content.core.GolemType;
import dev.xkmc.modulargolems.content.core.StatFilterType;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import dev.xkmc.modulargolems.content.modifier.base.GolemModifier;
import dev.xkmc.modulargolems.init.registrate.GolemTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BackstabModifier extends GolemModifier {

    // 每级背刺增伤 50%
    private static final float DAMAGE_PER_LEVEL = 0.50f;

    public BackstabModifier(int maxLevel) {
        super(StatFilterType.ATTACK, maxLevel);
    }

    /**
     * 核心：限定仅犬型傀儡（Dog Golem）可装配此升级
     */
    @Override
    public boolean fitsOn(GolemType<?, ?> type) {
        return type == GolemTypes.TYPE_DOG.get();
    }

    @Override
    public void onHurtTarget(AbstractGolemEntity<?, ?> entity, DamageData.Offence cache, int level) {
        LivingEntity target = cache.getTarget();
        if (target == null) return;

        if (isBackstab(entity, target)) {
            // 通过 NeoForge 原生的 DamageContainer 直接提升最终计算伤害
            float currentDmg = cache.getContainer().getNewDamage();
            cache.getContainer().setNewDamage(currentDmg * (1.0f + DAMAGE_PER_LEVEL * level));

            // 触发暴击粒子特效（在服务端向客户端广播）
            if (!entity.level().isClientSide()) {
                entity.level().broadcastEntityEvent(target, (byte) 4);
            }
        }
    }

    /**
     * 背刺判定：处于目标后背约 120° 扇形区域内，且攻击者视线也大致面向前方
     */
    private boolean isBackstab(LivingEntity attacker, LivingEntity target) {
        Vec3 targetLook = target.getViewVector(1.0F).normalize();
        Vec3 attackerLook = attacker.getViewVector(1.0F).normalize();
        Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();

        // 1. targetLook 与 toAttacker 点乘 < -0.5：攻击者处于目标背部 ~120 度锥角内
        // 2. targetLook 与 attackerLook 点乘 > 0.3：攻击者也朝向目标背部（防止倒退撞击触发）
        return targetLook.dot(toAttacker) < -0.5 && targetLook.dot(attackerLook) > 0.3;
    }

    @Override
    public List<MutableComponent> getDetail(int level) {
        int percent = Math.round(DAMAGE_PER_LEVEL * level * 100);
        return List.of(
                Component.translatable("modifier.examplemod.backstab.desc", percent)
                        .withStyle(ChatFormatting.GREEN)
        );
    }
}