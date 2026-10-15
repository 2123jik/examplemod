//package com.example.examplemod.mixin.cataclysm;
//
//import com.example.examplemod.config.LaserConfig;
//import com.github.L_Ender.cataclysm.client.tool.ControlledAnimation;
//import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity;
//import com.github.L_Ender.cataclysm.entity.projectile.Death_Laser_Beam_Entity;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.LivingEntity;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(Death_Laser_Beam_Entity.class)
//public abstract class DeathLaserBeamMixin {
//
//    @Shadow public LivingEntity caster;
//    @Shadow public float renderYaw;
//    @Shadow public float renderPitch;
//    @Shadow public ControlledAnimation appear;
//    @Shadow public boolean on;
//    @Shadow public abstract float getYaw();
//    @Shadow public abstract float getPitch();
//
//    @Inject(method = "tick", at = @At("TAIL"))
//    private void lockFixedMaxFormAndBindTip(CallbackInfo ci) {
//        Death_Laser_Beam_Entity beam = (Death_Laser_Beam_Entity) (Object) this;
//
//        if (this.caster instanceof Ignis_Entity ignis) {
//            // 1. 彻底锁定最大形态：粗细强行锁死在最大值 3，且双端直接越过 20 tick 蓄力期
//            if (beam.tickCount < 21) {
//                beam.tickCount = 21;
//            }
//            this.appear.setTimer(3);
//
//            // 2. 持续时间一到（on 变为 false），立刻秒销毁，不做收缩动画
//            if (!this.on) {
//                beam.discard();
//                return;
//            }
//
//            // 3. 锁定激光角度：严禁客户端用 caster.getXRot() 篡改为水平线
//            this.renderYaw = this.getYaw();
//            this.renderPitch = this.getPitch();
//
//            // 4. 剑尖吸附：在顶端保持的 21 tick 期间，如果焰魔转身，剑尖发射源紧跟旋转
//            if (ignis.getAnimation() == Ignis_Entity.POKED_ATTACK) {
//                float angle = (0.01745329251F * ignis.yBodyRot);
//                double forward = LaserConfig.BLADE_FORWARD.get();
//                double up = LaserConfig.BLADE_UP.get();
//
//                double tipX = ignis.getX() + forward * Mth.sin((float) (Math.PI + angle));
//                double tipZ = ignis.getZ() + forward * Mth.cos(angle);
//                double tipY = ignis.getY() + up;
//                beam.setPos(tipX, tipY, tipZ);
//            }
//        }
//    }
//}