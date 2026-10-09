package com.example.examplemod.mixin.villna;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 500)
public abstract class CameraMixin {

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(
            method = "setup",
            at = @At("RETURN")
    )
    private void examplemod$restoreFirstPersonCamera(
            BlockGetter level,
            Entity cameraEntity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo ci
    ) {
        // detached=true 为第三人称，保留原版距离与墙体碰撞。
        if (detached) {
            return;
        }

        // 恢复正常的实体视线。
        // setRotation 也会重新生成摄像机旋转四元数，清除滚转。
        this.setRotation(
                cameraEntity.getViewYRot(partialTick),
                cameraEntity.getViewXRot(partialTick)
        );

        // 保留原版眼高插值，避免潜行时眼高突然跳动。
        double x = Mth.lerp(
                (double) partialTick,
                cameraEntity.xo,
                cameraEntity.getX()
        );

        double y = Mth.lerp(
                (double) partialTick,
                cameraEntity.yo,
                cameraEntity.getY()
        ) + Mth.lerp(
                partialTick,
                this.eyeHeightOld,
                this.eyeHeight
        );

        double z = Mth.lerp(
                (double) partialTick,
                cameraEntity.zo,
                cameraEntity.getZ()
        );

        this.setPosition(x, y, z);
    }
}
