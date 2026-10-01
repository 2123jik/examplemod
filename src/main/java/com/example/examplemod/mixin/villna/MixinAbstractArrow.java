package com.example.examplemod.mixin.villna;

import com.example.examplemod.network.ArrowCritPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractArrow.class)
public abstract class MixinAbstractArrow {

    @Shadow public abstract boolean isCritArrow();
    @Shadow private double baseDamage;
    @Shadow public abstract ItemStack getWeaponItem();

    @ModifyArg(
        method = "onHitEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        ),
        index = 1
    )
    private float captureAndNotifyCrit(float finalDamage) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        if (!arrow.level().isClientSide() && this.isCritArrow()) {
            if (arrow.getOwner() instanceof ServerPlayer shooter) {
                float speed = (float) arrow.getDeltaMovement().length();
                double d0 = this.baseDamage;
                DamageSource damageSource = arrow.damageSources().arrow(arrow, shooter);

                if (this.getWeaponItem() != null && arrow.level() instanceof ServerLevel serverLevel) {
                    d0 = (double) EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), arrow, damageSource, (float) d0);
                }

                // 未加 k 时的纯基础伤害
                float rawBase = (float) Mth.ceil(Mth.clamp((double) speed * d0, 0.0, (double) Integer.MAX_VALUE));

                if (rawBase > 0) {
                    float multiplier = finalDamage / rawBase;
                    float bonusDamage = finalDamage - rawBase;
                    Vec3 hitPos = arrow.position();

                    // 发送 S2C 网络包给射手客户端
                    PacketDistributor.sendToPlayer(
                        shooter, 
                        new ArrowCritPayload(multiplier, finalDamage, bonusDamage, hitPos.x, hitPos.y, hitPos.z)
                    );
                }
            }
        }
        return finalDamage;
    }
}