package com.example.examplemod.client.particle;

import com.example.examplemod.client.shader.ModShaders;
import com.example.examplemod.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SpaceSlashParticle extends Particle {
    private final float slashLength;
    private final float slashWidth;
    private final float slashRoll;

    public SpaceSlashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.hasPhysics = false;

        // 延长至 7 tick，让视网膜能捕捉到“撕开 -> 驻留 -> 缝合”的完整过程
        this.lifetime = 7;

        this.slashLength = 3.2F;
        this.slashWidth = 0.65F;

        // 产生随机微偏角，避免每一刀都是死板的固定 45 度
        this.slashRoll = -0.785F + (this.random.nextFloat() - 0.5F) * 0.3F;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float py = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float pz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());

        float progress = ((float) this.age + partialTick) / (float) this.lifetime;

        // 动作曲线优化：极速抽刀（前 10% 瞬间全展开），随后停顿，最后快速愈合
        float lenEnvelope = progress < 0.1F ? (progress / 0.1F) : 1.0F;
        float currentHalfLen = (this.slashLength * lenEnvelope) * 0.5F;
        float currentHalfWid = this.slashWidth * 0.5F;

        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.rotateZ(this.slashRoll);

        Vector3f[] quad = new Vector3f[]{
                new Vector3f(-currentHalfLen, -currentHalfWid, 0.0F),
                new Vector3f(-currentHalfLen,  currentHalfWid, 0.0F),
                new Vector3f( currentHalfLen,  currentHalfWid, 0.0F),
                new Vector3f( currentHalfLen, -currentHalfWid, 0.0F)
        };

        for (Vector3f v : quad) {
            rotation.transform(v);
            v.add(px, py, pz);
        }

        // 写入 Fade（传递给 shader 的 vertexColor.a）
        float fade = Math.max(0.0F, 1.0F - progress);
        int light = 0xF000F0; // 满级全亮发光

        buffer.addVertex(quad[0].x(), quad[0].y(), quad[0].z()).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, fade).setLight(light);
        buffer.addVertex(quad[1].x(), quad[1].y(), quad[1].z()).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, fade).setLight(light);
        buffer.addVertex(quad[2].x(), quad[2].y(), quad[2].z()).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, fade).setLight(light);
        buffer.addVertex(quad[3].x(), quad[3].y(), quad[3].z()).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, fade).setLight(light);
    }
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (++this.age >= this.lifetime) {
            this.remove();
        }
    }
    @Override
    public ParticleRenderType getRenderType() {
        return ModShaders.SPACE_SLASH_RENDER_TYPE;
    }

    public static void spawnSpaceSlash(ClientLevel level, Entity target) {
        level.addParticle(
                ModParticles.DAMAGE_SPARK.get(),
                target.getX(),
                target.getY(0.55),
                target.getZ(),
                0.0, 0.0, 0.0
        );
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet spriteSet) {}

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new SpaceSlashParticle(level, x, y, z);
        }
    }
}