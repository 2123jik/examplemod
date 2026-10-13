package com.example.examplemod.entity;

import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.spell.ModSpells;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CustomLightningLanceProjectile extends LightningLanceProjectile {

    public CustomLightningLanceProjectile(EntityType<? extends CustomLightningLanceProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public CustomLightningLanceProjectile(Level levelIn, LivingEntity shooter) {
        super(ModEntities.CUSTOM_LIGHTNING_LANCE.get(), levelIn);
        this.setOwner(shooter);
    }

    // 1. 不需要击中的粒子和特效：将原版产生大量 ELECTRICITY 粒子的方法置空
    @Override
    public void impactParticles(double x, double y, double z) {
        // 留空，不产生任何粒子
    }

    // 2. 命中实体：伤害继承自原法术伤害体系
    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        DamageSources.applyDamage(
                entityHitResult.getEntity(),
                this.damage, // 继承上面在 onCast 里传入的法术伤害
                ModSpells.CUSTOM_LIGHTNING_LANCE_SPELL.get().getDamageSource(this, this.getOwner())
        );
    }

    // 3. 击中命中点（方块或实体）：立即生成一道原版闪电
    @Override
    protected void onHit(@NotNull HitResult pResult) {
        if (!this.level().isClientSide) {
            Vec3 hitPos = pResult.getLocation();
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
            if (lightning != null) {
                lightning.moveTo(hitPos);
                // 伤害已由上面 onHitEntity 正确结算，setVisualOnly(true) 可提供真实雷击视觉和雷声且不会造成双重伤害
                lightning.setVisualOnly(true);
                if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                    lightning.setCause(serverPlayer);
                }
                this.level().addFreshEntity(lightning);
            }
        }

        // 调用 super 销毁实体并触发基础逻辑
        super.onHit(pResult);
    }
}