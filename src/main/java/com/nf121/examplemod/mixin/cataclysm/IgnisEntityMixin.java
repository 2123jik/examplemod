//package com.example.examplemod.mixin.cataclysm;
//
//import com.example.examplemod.config.LaserConfig;
//import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity;
//import com.github.L_Ender.cataclysm.entity.projectile.Death_Laser_Beam_Entity;
//import com.github.L_Ender.cataclysm.init.ModEntities;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.Entity;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(Ignis_Entity.class)
//public abstract class IgnisEntityMixin {
//
//    @Inject(method = "positionRider", at = @At("HEAD"))
//    private void injectLaserAtBladeTip(Entity passenger, Entity.MoveFunction moveFunc, CallbackInfo ci) {
//        Ignis_Entity ignis = (Ignis_Entity) (Object) this;
//
//        // 仅在服务端生成实体
//        if (ignis.level().isClientSide) return;
//
//        if (ignis.getAnimation() == Ignis_Entity.POKED_ATTACK) {
//            int animTick = ignis.getAnimationTick();
//
//            // 严格在被挑到最顶端的设定时刻（默认第 25 tick）爆发
//            if (animTick == LaserConfig.START_TICK.get()) {
//                float angle = (0.01745329251F * ignis.yBodyRot);
//
//                // 1. 读取配置：计算穿透目标身体的剑锋最尖端世界坐标
//                double forward = LaserConfig.BLADE_FORWARD.get();
//                double up = LaserConfig.BLADE_UP.get();
//
//                double tipX = ignis.getX() + forward * Mth.sin((float) (Math.PI + angle));
//                double tipZ = ignis.getZ() + forward * Mth.cos(angle);
//                double tipY = ignis.getY() + up;
//
//                // 2. 角度换算：偏航角对齐正前向，仰角由配置决定
//                float beamYaw = (float) ((ignis.yBodyRot + 90.0F) * Math.PI / 180.0F);
//                float beamPitch = (float) Math.toRadians(LaserConfig.PITCH_DEGREES.get());
//
//                // 3. 持续时间：顶端悬挂持续刻 (46 - 25 = 21 tick)
//                int duration = Math.max(1, LaserConfig.END_TICK.get() - LaserConfig.START_TICK.get());
//
//                // 4. 创建激光实体
//                Death_Laser_Beam_Entity laser = new Death_Laser_Beam_Entity(
//                        ModEntities.DEATH_LASER_BEAM.get(),
//                        ignis.level(),
//                        ignis,
//                        tipX, tipY, tipZ, // 从剑尖发射
//                        beamYaw,
//                        beamPitch,
//                        duration,
//                        LaserConfig.DAMAGE.get().floatValue(),
//                        LaserConfig.HP_DAMAGE.get().floatValue()
//                );
//
//                laser.setFire(true);
//                ignis.level().addFreshEntity(laser);
//            }
//        }
//    }
//}