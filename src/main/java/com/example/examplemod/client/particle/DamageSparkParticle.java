package com.example.examplemod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public final class DamageSparkParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float spin;

    public DamageSparkParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz,
        SpriteSet sprites
    ) {
        super(level, x, y, z);

        // 显式指定速度，不使用父类带速度构造器的随机扰动。
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.hasPhysics = false;
        this.lifetime = 6 + this.random.nextInt(3);

        // quadSize 是半尺寸。贴图本身进一步控制火花的细长轮廓。
        this.baseSize = 0.085F + this.random.nextFloat() * 0.035F;
        this.quadSize = this.baseSize;

        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.14F;

        this.rCol = 1.0F;
        this.gCol = 0.95F;
        this.bCol = 0.80F;
        this.alpha = 0.0F;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (++this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd, this.zd);

        // 开始散开，随后迅速收住，避免漂浮一大片。
        this.xd *= 0.76;
        this.yd = this.yd * 0.76 - 0.003;
        this.zd *= 0.76;

        this.roll += this.spin;

        float progress = (float) this.age / this.lifetime;

        // 随生命周期转暖，避免整段时间都是刺眼的纯白。
        this.rCol = 1.0F;
        this.gCol = Mth.lerp(progress, 0.95F, 0.60F);
        this.bCol = Mth.lerp(progress, 0.80F, 0.26F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp(
            (this.age + partialTick) / this.lifetime,
            0.0F,
            1.0F
        );

        float appear = smoothstep(0.0F, 0.10F, progress);
        float disappear = 1.0F - smoothstep(0.28F, 1.0F, progress);

        // TextureSheetParticle 的渲染过程在读取顶点颜色前调用本方法，
        // 因而透明度也能按 partialTick 平滑变化。
        this.alpha = appear * disappear * 0.92F;

        float sizeEnvelope =
            (0.75F + 0.25F * appear) * (1.0F - 0.45F * progress);

        return this.baseSize * sizeEnvelope;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private static float smoothstep(float from, float to, float value) {
        float t = Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
