package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DamageParticlePacketMixin {
    @Unique
    private static final int HITFX_PARTICLES_PER_HIT = 5;

    @Unique
    private static final int HITFX_PARTICLES_PER_TICK = 30;

    @Shadow
    private ClientLevel level;

    @Unique
    private ClientLevel hitfx$budgetLevel;

    @Unique
    private long hitfx$budgetTick = Long.MIN_VALUE;

    @Unique
    private int hitfx$usedParticles;

    @Inject(
        method = "handleParticleEvent",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/network/protocol/game/"
                + "ClientboundLevelParticlesPacket;getCount()I",
            ordinal = 0
        ),
        cancellable = true,
        require = 1
    )
    private void hitfx$replaceDamageBurst(
        ClientboundLevelParticlesPacket packet,
        CallbackInfo ci
    ) {
        if (packet.getParticle().getType() != ParticleTypes.DAMAGE_INDICATOR) {
            return;
        }

        // 取消原版根据 packet.count 展开的粒子循环。
        ci.cancel();

        ClientLevel currentLevel = this.level;
        if (currentLevel == null) {
            return;
        }

        long tick = currentLevel.getGameTime();

        if (hitfx$budgetLevel != currentLevel || hitfx$budgetTick != tick) {
            hitfx$budgetLevel = currentLevel;
            hitfx$budgetTick = tick;
            hitfx$usedParticles = 0;
        }

        // 整簇准入，避免预算耗尽时只剩零星一枚。
        if (hitfx$usedParticles + HITFX_PARTICLES_PER_HIT
            > HITFX_PARTICLES_PER_TICK) {
            return;
        }

        hitfx$usedParticles += HITFX_PARTICLES_PER_HIT;

        RandomSource random = currentLevel.random;

        // 借用包中的范围信息，但严格限制扩散半径。
        double spreadX = Math.min(Math.abs(packet.getXDist()), 0.18);
        double spreadY = Math.min(Math.abs(packet.getYDist()), 0.22);
        double spreadZ = Math.min(Math.abs(packet.getZDist()), 0.18);

        float phase = random.nextFloat() * Mth.TWO_PI;

        for (int i = 0; i < HITFX_PARTICLES_PER_HIT; i++) {
            float angle =
                phase + i * Mth.TWO_PI / HITFX_PARTICLES_PER_HIT;

            double speed = 0.075 + random.nextDouble() * 0.055;

            double x = packet.getX()
                + (random.nextDouble() * 2.0 - 1.0) * spreadX;
            double y = packet.getY()
                + (random.nextDouble() * 2.0 - 1.0) * spreadY;
            double z = packet.getZ()
                + (random.nextDouble() * 2.0 - 1.0) * spreadZ;

            double vx = Mth.cos(angle) * speed;
            double vy = 0.035 + random.nextDouble() * 0.075;
            double vz = Mth.sin(angle) * speed;

            // 使用常规粒子入口，尊重客户端距离和粒子设置。
            currentLevel.addParticle(
                ParticleTypes.DAMAGE_INDICATOR,
                x, y, z,
                vx, vy, vz
            );
        }
    }
}
