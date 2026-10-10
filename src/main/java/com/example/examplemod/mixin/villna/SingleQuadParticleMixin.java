package com.example.examplemod.mixin.villna;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    /**
     * 距离镜头小于等于此值时，完全透明。
     */
    @Unique
    private static final float yourmod$HIDE_DISTANCE = 3F;

    /**
     * 距离镜头大于等于此值时，保留原始 Alpha。
     * 必须大于 HIDE_DISTANCE。
     */
    @Unique
    private static final float yourmod$FULL_DISTANCE = 3.5F;

    /**
     * 当前四边形的距离衰减系数。
     */
    @Unique
    private float yourmod$cameraFade = 1.0F;

    /**
     * 注入接收相机相对坐标的重载。
     *
     * 此时 x、y、z 已经包含 partialTicks 插值，
     * 并且已经减去镜头位置。
     * 一个四边形只计算一次距离，供四个顶点共用。
     */
    @Inject(
            method = "renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Quaternionf;FFFF)V",
            at = @At("HEAD"),
            require = 1
    )
    private void yourmod$updateCameraFade(
            VertexConsumer buffer,
            Quaternionf quaternion,
            float x,
            float y,
            float z,
            float partialTicks,
            CallbackInfo ci
    ) {
        float distanceSquared = x * x + y * y + z * z;

        // 远处粒子不需要开平方。
        if (distanceSquared >= yourmod$FULL_DISTANCE * yourmod$FULL_DISTANCE) {
            this.yourmod$cameraFade = 1.0F;
            return;
        }

        // 近处粒子直接完全透明。
        if (distanceSquared <= yourmod$HIDE_DISTANCE * yourmod$HIDE_DISTANCE) {
            this.yourmod$cameraFade = 0.0F;
            return;
        }

        float distance = Mth.sqrt(distanceSquared);

        this.yourmod$cameraFade = Mth.clamp(
                (distance - yourmod$HIDE_DISTANCE)
                        / (yourmod$FULL_DISTANCE - yourmod$HIDE_DISTANCE),
                0.0F,
                1.0F
        );
    }

    /**
     * 修改 setColor(r, g, b, alpha) 的第四个参数。
     * 保留原始颜色和粒子自身的 Alpha 动画。
     */
    @ModifyArg(
            method = "renderVertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Quaternionf;FFFFFFFFI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            index = 3,
            require = 1
    )
    private float yourmod$applyCameraFade(float originalAlpha) {
        return originalAlpha * this.yourmod$cameraFade;
    }
}
