package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningLanceProjectile.class)
public abstract class LightningLanceProjectileMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void spawnLightningOnHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        LightningLanceProjectile self = (LightningLanceProjectile) (Object) this;

        // 仅在服务端生成实体，防止客户端重复生成或幽灵实体
        if (!self.level().isClientSide) {
            Entity target = entityHitResult.getEntity();
            
            // 创建闪电实体
            LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(self.level());
            if (lightningBolt != null) {
                // 将闪电设置到受击实体的坐标
                lightningBolt.moveTo(target.position());
                
                // 将闪电的触发者设置为法术的释放者（如果是玩家，可正常计入击杀/伤害来源）
                if (self.getOwner() instanceof ServerPlayer player) {
                    lightningBolt.setCause(player);
                }
                
                // 生成到世界中
                self.level().addFreshEntity(lightningBolt);
            }
        }
    }
}