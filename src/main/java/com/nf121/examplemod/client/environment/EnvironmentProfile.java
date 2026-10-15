package com.nf121.examplemod.client.environment;

import net.minecraft.util.Mth;

public record EnvironmentProfile(
    float skyTopR,
    float skyTopG,
    float skyTopB,

    float skyHorizonR,
    float skyHorizonG,
    float skyHorizonB,

    float fogR,
    float fogG,
    float fogB,

    float particleR,
    float particleG,
    float particleB,

    float fogStart,
    float fogEnd,

    float skyOpacity,
    float particleDensity,
    float particleSpeed,
    float particleSize,

    float brightness,
    float pulseStrength,
    float turbulence,
    float emissiveRatio
) {

    public static EnvironmentProfile lerp(
        EnvironmentProfile from,
        EnvironmentProfile to,
        float progress
    ) {
        float value = Mth.clamp(progress, 0.0F, 1.0F);

        return new EnvironmentProfile(
            Mth.lerp(value, from.skyTopR, to.skyTopR),
            Mth.lerp(value, from.skyTopG, to.skyTopG),
            Mth.lerp(value, from.skyTopB, to.skyTopB),

            Mth.lerp(value, from.skyHorizonR, to.skyHorizonR),
            Mth.lerp(value, from.skyHorizonG, to.skyHorizonG),
            Mth.lerp(value, from.skyHorizonB, to.skyHorizonB),

            Mth.lerp(value, from.fogR, to.fogR),
            Mth.lerp(value, from.fogG, to.fogG),
            Mth.lerp(value, from.fogB, to.fogB),

            Mth.lerp(value, from.particleR, to.particleR),
            Mth.lerp(value, from.particleG, to.particleG),
            Mth.lerp(value, from.particleB, to.particleB),

            Mth.lerp(value, from.fogStart, to.fogStart),
            Mth.lerp(value, from.fogEnd, to.fogEnd),

            Mth.lerp(value, from.skyOpacity, to.skyOpacity),
            Mth.lerp(value, from.particleDensity, to.particleDensity),
            Mth.lerp(value, from.particleSpeed, to.particleSpeed),
            Mth.lerp(value, from.particleSize, to.particleSize),

            Mth.lerp(value, from.brightness, to.brightness),
            Mth.lerp(value, from.pulseStrength, to.pulseStrength),
            Mth.lerp(value, from.turbulence, to.turbulence),
            Mth.lerp(value, from.emissiveRatio, to.emissiveRatio)
        );
    }

    public EnvironmentProfile applyWeather(float rain, float thunder) {
        float weather = Mth.clamp(rain * 0.65F + thunder * 0.35F, 0.0F, 1.0F);

        float weatherFogR = this.fogR * 0.65F;
        float weatherFogG = this.fogG * 0.69F;
        float weatherFogB = this.fogB * 0.73F;

        return new EnvironmentProfile(
            Mth.lerp(weather, this.skyTopR, this.skyTopR * 0.52F),
            Mth.lerp(weather, this.skyTopG, this.skyTopG * 0.56F),
            Mth.lerp(weather, this.skyTopB, this.skyTopB * 0.62F),

            Mth.lerp(weather, this.skyHorizonR, weatherFogR),
            Mth.lerp(weather, this.skyHorizonG, weatherFogG),
            Mth.lerp(weather, this.skyHorizonB, weatherFogB),

            Mth.lerp(weather, this.fogR, weatherFogR),
            Mth.lerp(weather, this.fogG, weatherFogG),
            Mth.lerp(weather, this.fogB, weatherFogB),

            this.particleR,
            this.particleG,
            this.particleB,

            Mth.lerp(weather, this.fogStart, this.fogStart * 0.55F),
            Mth.lerp(weather, this.fogEnd, this.fogEnd * 0.68F),

            Mth.lerp(weather, this.skyOpacity, this.skyOpacity * 0.65F),
            Mth.lerp(weather, this.particleDensity, this.particleDensity * 1.2F),
            Mth.lerp(weather, this.particleSpeed, this.particleSpeed * 1.25F),
            this.particleSize,

            Mth.lerp(weather, this.brightness, this.brightness * 0.78F),
            this.pulseStrength,
            Mth.lerp(weather, this.turbulence, this.turbulence * 1.5F),
            this.emissiveRatio
        );
    }
}
