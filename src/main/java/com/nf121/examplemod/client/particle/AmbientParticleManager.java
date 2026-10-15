package com.nf121.examplemod.client.particle;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.client.environment.EnvironmentColorSampler;
import com.nf121.examplemod.client.environment.EnvironmentProfile;
import com.nf121.examplemod.client.environment.VisualTier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class AmbientParticleManager {

    public static final AmbientParticleManager INSTANCE =
        new AmbientParticleManager();

    public static final ResourceLocation PARTICLE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(
            ExampleMod.MOD_ID,
            "textures/environment/ambient_particles.png"
        );

    private static final int HARD_LIMIT = 300;

    private final List<AmbientParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private float spawnAccumulator;
    private float budgetScale = 1.0F;

    private AmbientParticleManager() {
    }

    public void tick(
        Minecraft minecraft,
        VisualTier tier,
        EnvironmentProfile profile
    ) {
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            return;
        }

        updateBudget(minecraft);

        Iterator<AmbientParticle> iterator = particles.iterator();

        while (iterator.hasNext()) {
            AmbientParticle particle = iterator.next();

            particle.previousX = particle.x;
            particle.previousY = particle.y;
            particle.previousZ = particle.z;

            particle.age++;

            if (particle.age >= particle.lifetime) {
                iterator.remove();
                continue;
            }

            updateParticle(
                particle,
                tier,
                profile,
                minecraft.level.getGameTime()
            );

            Vec3 cameraPosition =
                minecraft.gameRenderer.getMainCamera().getPosition();

            double dx = particle.x - cameraPosition.x;
            double dy = particle.y - cameraPosition.y;
            double dz = particle.z - cameraPosition.z;

            if (dx * dx + dy * dy + dz * dz > 72.0 * 72.0) {
                iterator.remove();
            }
        }

        int targetCount = getTargetCount(profile);

        if (particles.size() >= targetCount) {
            return;
        }

        float spawnRate =
            1.25F + profile.particleDensity() * 5.5F;

        spawnAccumulator += spawnRate * budgetScale;

        int spawnCount = Math.min(
            (int) spawnAccumulator,
            targetCount - particles.size()
        );

        spawnAccumulator -= spawnCount;

        for (int index = 0; index < spawnCount; index++) {
            spawnParticle(minecraft, tier, profile);
        }
    }

    private void updateParticle(
        AmbientParticle particle,
        VisualTier tier,
        EnvironmentProfile profile,
        long gameTime
    ) {
        float time = gameTime + particle.age;

        float waveX = Mth.sin(
            time * 0.025F + particle.noiseOffset
        );

        float waveZ = Mth.cos(
            time * 0.021F + particle.noiseOffset * 1.37F
        );

        float turbulence = profile.turbulence();

        switch (tier) {
            case HAVEN -> {
                particle.velocityX += waveX * 0.0007F * turbulence;
                particle.velocityY += 0.00045F;
                particle.velocityZ += waveZ * 0.0007F * turbulence;
            }
            case FRONTIER -> {
                particle.velocityX += 0.0035F * turbulence;
                particle.velocityY += waveZ * 0.00035F;
                particle.velocityZ += waveX * 0.0018F * turbulence;
            }
            case ASCENT -> {
                particle.velocityX += waveX * 0.0012F * turbulence;
                particle.velocityY += 0.0024F;
                particle.velocityZ += waveZ * 0.0012F * turbulence;
            }
            case SUMMIT -> {
                particle.velocityX += 0.0048F * turbulence;
                particle.velocityY -= 0.0013F;
                particle.velocityZ += waveZ * 0.0024F * turbulence;
            }
            case PINNACLE -> {
                double centerX = particle.orbitCenterX;
                double centerZ = particle.orbitCenterZ;

                double deltaX = particle.x - centerX;
                double deltaZ = particle.z - centerZ;

                particle.velocityX +=
                    (float) (-deltaZ * 0.00012F)
                        + waveX * 0.0012F * turbulence;

                particle.velocityZ +=
                    (float) (deltaX * 0.00012F)
                        + waveZ * 0.0012F * turbulence;

                particle.velocityY += 0.0015F;
            }
        }

        float maximumSpeed = profile.particleSpeed() * 0.075F;

        particle.velocityX =
            Mth.clamp(particle.velocityX, -maximumSpeed, maximumSpeed);

        particle.velocityY =
            Mth.clamp(particle.velocityY, -maximumSpeed, maximumSpeed);

        particle.velocityZ =
            Mth.clamp(particle.velocityZ, -maximumSpeed, maximumSpeed);

        particle.x += particle.velocityX;
        particle.y += particle.velocityY;
        particle.z += particle.velocityZ;

        particle.velocityX *= 0.975F;
        particle.velocityY *= 0.975F;
        particle.velocityZ *= 0.975F;
        float pulse = Mth.sin(particle.age * particle.colorPhaseSpeed + particle.noiseOffset) * 0.08F;
        particle.red = Mth.clamp(particle.initialR * (1.0F + pulse), 0.0F, 1.0F);
        particle.green = Mth.clamp(particle.initialG * (1.0F + pulse * 0.5F), 0.0F, 1.0F);
        particle.blue = Mth.clamp(particle.initialB * (1.0F - pulse * 0.5F), 0.0F, 1.0F);
    }

    private void spawnParticle(
        Minecraft minecraft,
        VisualTier tier,
        EnvironmentProfile profile
    ) {
        Vec3 cameraPosition =
            minecraft.gameRenderer.getMainCamera().getPosition();

        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 5.0 + random.nextDouble() * 47.0;

        AmbientParticle particle = new AmbientParticle();

        particle.x =
            cameraPosition.x + Math.cos(angle) * distance;

        particle.y =
            cameraPosition.y - 8.0 + random.nextDouble() * 28.0;

        particle.z =
            cameraPosition.z + Math.sin(angle) * distance;

        particle.previousX = particle.x;
        particle.previousY = particle.y;
        particle.previousZ = particle.z;

        particle.orbitCenterX = cameraPosition.x;
        particle.orbitCenterZ = cameraPosition.z;

        float speed = profile.particleSpeed() * 0.018F;

        particle.velocityX =
            (random.nextFloat() - 0.5F) * speed;

        particle.velocityY =
            (random.nextFloat() - 0.5F) * speed;

        particle.velocityZ =
            (random.nextFloat() - 0.5F) * speed;

        particle.age = 0;
        particle.lifetime = 100 + random.nextInt(260);

        particle.size =
            profile.particleSize() * (0.65F + random.nextFloat() * 0.85F);

        particle.red = Mth.clamp(
            profile.particleR() * (0.84F + random.nextFloat() * 0.26F),
            0.0F,
            1.0F
        );

        particle.green = Mth.clamp(
            profile.particleG() * (0.84F + random.nextFloat() * 0.26F),
            0.0F,
            1.0F
        );

        particle.blue = Mth.clamp(
            profile.particleB() * (0.84F + random.nextFloat() * 0.26F),
            0.0F,
            1.0F
        );
        EnvironmentColorSampler sampler = EnvironmentColorSampler.INSTANCE;
        float influence = sampler.getStructureInfluence();

        // 80% 几率吸取结构主色，20% 几率成为发光结构高光色
        boolean isAccent = random.nextFloat() < 0.20F;
        float structR = isAccent ? sampler.getAccentR() : sampler.getStructureR();
        float structG = isAccent ? sampler.getAccentG() : sampler.getStructureG();
        float structB = isAccent ? sampler.getAccentB() : sampler.getStructureB();

        // Profile基准色与结构色的有机混合权重（可调，例如 60% 受结构影响，40% 保留世界阶层印记）
        float blendFactor = influence * 0.65F;

        float baseR = Mth.lerp(blendFactor, profile.particleR(), structR);
        float baseG = Mth.lerp(blendFactor, profile.particleG(), structG);
        float baseB = Mth.lerp(blendFactor, profile.particleB(), structB);

        // 加入细微随机噪点，防止单调
        float noise = 0.90F + random.nextFloat() * 0.20F;
        particle.initialR = particle.red = Mth.clamp(baseR * noise, 0.0F, 1.0F);
        particle.initialG = particle.green = Mth.clamp(baseG * noise, 0.0F, 1.0F);
        particle.initialB = particle.blue = Mth.clamp(baseB * noise, 0.0F, 1.0F);

        particle.colorPhaseSpeed = 0.02F + random.nextFloat() * 0.04F;

        // 如果周围是强发光方块提取的高光色，将粒子标记为 emissive (自发光混合)
        if (isAccent && influence > 0.3F) {
            particle.emissive = true;
        } else {
            particle.emissive = random.nextFloat() < profile.emissiveRatio();
        }

        particle.baseAlpha = 0.25F + random.nextFloat() * 0.55F;
        particle.noiseOffset = random.nextFloat() * 1000.0F;

        particle.emissive =
            random.nextFloat() < profile.emissiveRatio();

        particle.sprite = chooseSprite(tier);

        particles.add(particle);
    }

    private int chooseSprite(VisualTier tier) {
        int base = switch (tier) {
            case HAVEN -> 0;
            case FRONTIER -> 2;
            case ASCENT -> 4;
            case SUMMIT -> 5;
            case PINNACLE -> 6;
        };

        if (random.nextFloat() < 0.30F) {
            base++;
        }

        return Mth.clamp(base, 0, 7);
    }

    private int getTargetCount(EnvironmentProfile profile) {
        int baseBudget = 900;

        int target = (int) (
            baseBudget
                * profile.particleDensity()
                * budgetScale
        );

        return Mth.clamp(target, 64, HARD_LIMIT);
    }

    private void updateBudget(Minecraft minecraft) {
        int fps = Minecraft.getInstance().getFps();

        if (fps > 0 && fps < 42) {
            budgetScale *= 0.985F;
        } else if (fps >= 75) {
            budgetScale *= 1.003F;
        }

        budgetScale = Mth.clamp(
            budgetScale,
            0.28F,
            1.0F
        );
    }

    public List<AmbientParticle> getParticles() {
        return particles;
    }

    public void clear() {
        particles.clear();
        spawnAccumulator = 0.0F;
    }

    public static final class AmbientParticle {
        public float initialR;
        public float initialG;
        public float initialB;

        // 呼吸色彩偏移速度
        public float colorPhaseSpeed;
        public double x;
        public double y;
        public double z;

        public double previousX;
        public double previousY;
        public double previousZ;

        public float velocityX;
        public float velocityY;
        public float velocityZ;

        public int age;
        public int lifetime;

        public float size;

        public float red;
        public float green;
        public float blue;

        public float baseAlpha;
        public float noiseOffset;

        public double orbitCenterX;
        public double orbitCenterZ;

        public int sprite;
        public boolean emissive;

        public float getAlpha(float partialTick) {
            float interpolatedAge = age + partialTick;
            float lifeProgress = interpolatedAge / lifetime;

            float fadeIn = Mth.clamp(lifeProgress / 0.10F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp(
                (1.0F - lifeProgress) / 0.18F,
                0.0F,
                1.0F
            );

            return baseAlpha
                * DayPhaseLikeSmoothStep(fadeIn)
                * DayPhaseLikeSmoothStep(fadeOut);
        }

        private static float DayPhaseLikeSmoothStep(float value) {
            value = Mth.clamp(value, 0.0F, 1.0F);
            return value * value * (3.0F - 2.0F * value);
        }
    }
}
