package com.example.examplemod.mixin.villna;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class MixinPlayerArmorDurability {

    @ModifyVariable(
            method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float modifyArmorDurabilityDamage(float damage, DamageSource source) {
        Player player = (Player) (Object) this;

        // 1. 获取玩家当前拥有的伤害吸收量（黄心 / Absorption）
        float absorption = player.getAbsorptionAmount();

        // 2. 计算被吸收后的剩余有效伤害
        float effectiveDamage = damage;
        if (absorption > 0.0f) {
            // 如果伤害被吸收值完全吃掉，直接返回 0，不扣除任何耐久
            if (absorption >= effectiveDamage) {
                return 0.0f;
            }
            // 如果黄心没能完全抵消伤害，扣除被吸收的部分，只用穿透护盾的“剩余伤害”损耗耐久
            effectiveDamage -= absorption;
        }

        // 3. 获取玩家最大生命值（做防除以 0 的容错保护）
        float maxHealth = Math.max(1.0f, player.getMaxHealth());

        // 4. 将剩余伤害折算为等效的原版伤害（以 20 血为基准比例）
        float scaledDamage = (effectiveDamage / maxHealth) * 20.0f;

        // 5. 单次耐久损耗硬上限（最高等效 20 点伤害，即单次最多扣 5 点耐久）
        scaledDamage = Math.min(scaledDamage, 20.0f);

        return scaledDamage;
    }
}