package com.nf121.examplemod.client.environment;

public final class EnvironmentProfiles {

    private EnvironmentProfiles() {
    }

    public static EnvironmentProfile resolve(
        VisualTier tier,
        DayPhase.DayPhaseState phase
    ) {
        EnvironmentProfile current = get(tier, phase.current());
        EnvironmentProfile next = get(tier, phase.next());

        return EnvironmentProfile.lerp(
            current,
            next,
            phase.smoothProgress()
        );
    }

    public static EnvironmentProfile get(VisualTier tier, DayPhase phase) {
        return switch (tier) {
            case HAVEN -> haven(phase);
            case FRONTIER -> frontier(phase);
            case ASCENT -> ascent(phase);
            case SUMMIT -> summit(phase);
            case PINNACLE -> pinnacle(phase);
        };
    }

    private static EnvironmentProfile haven(DayPhase phase) {
        return switch (phase) {
            case DAWN -> profile(
                0.30F, 0.48F, 0.78F,
                1.00F, 0.55F, 0.36F,
                0.72F, 0.76F, 0.78F,
                1.00F, 0.88F, 0.64F,
                0.18F, 0.95F,
                0.27F, 0.30F, 0.32F, 0.11F,
                1.05F, 0.02F, 0.10F, 0.08F
            );
            case MORNING -> profile(
                0.34F, 0.61F, 0.91F,
                0.77F, 0.87F, 0.95F,
                0.72F, 0.82F, 0.89F,
                0.94F, 0.93F, 0.76F,
                0.23F, 1.00F,
                0.18F, 0.25F, 0.27F, 0.10F,
                1.10F, 0.01F, 0.08F, 0.04F
            );
            case NOON -> profile(
                0.28F, 0.58F, 0.94F,
                0.68F, 0.84F, 0.98F,
                0.68F, 0.80F, 0.90F,
                1.00F, 0.98F, 0.82F,
                0.27F, 1.00F,
                0.12F, 0.18F, 0.24F, 0.09F,
                1.14F, 0.00F, 0.07F, 0.02F
            );
            case DUSK -> profile(
                0.22F, 0.35F, 0.67F,
                1.00F, 0.43F, 0.24F,
                0.64F, 0.56F, 0.58F,
                1.00F, 0.76F, 0.46F,
                0.17F, 0.88F,
                0.32F, 0.38F, 0.30F, 0.12F,
                0.93F, 0.03F, 0.13F, 0.10F
            );
            case EVENING -> profile(
                0.08F, 0.13F, 0.31F,
                0.29F, 0.29F, 0.45F,
                0.20F, 0.24F, 0.35F,
                0.62F, 0.75F, 1.00F,
                0.12F, 0.80F,
                0.36F, 0.48F, 0.26F, 0.10F,
                0.70F, 0.08F, 0.12F, 0.30F
            );
            case MIDNIGHT -> profile(
                0.018F, 0.028F, 0.09F,
                0.08F, 0.12F, 0.23F,
                0.08F, 0.10F, 0.17F,
                0.55F, 0.72F, 1.00F,
                0.10F, 0.76F,
                0.44F, 0.56F, 0.24F, 0.10F,
                0.58F, 0.12F, 0.10F, 0.48F
            );
        };
    }

    private static EnvironmentProfile frontier(DayPhase phase) {
        return switch (phase) {
            case DAWN -> profile(
                0.32F, 0.35F, 0.43F,
                0.91F, 0.46F, 0.21F,
                0.58F, 0.45F, 0.31F,
                0.82F, 0.57F, 0.30F,
                0.12F, 0.74F,
                0.34F, 0.72F, 0.50F, 0.13F,
                0.91F, 0.02F, 0.48F, 0.05F
            );
            case MORNING -> profile(
                0.39F, 0.50F, 0.59F,
                0.74F, 0.65F, 0.49F,
                0.57F, 0.55F, 0.47F,
                0.75F, 0.65F, 0.44F,
                0.17F, 0.83F,
                0.24F, 0.60F, 0.46F, 0.12F,
                1.00F, 0.01F, 0.40F, 0.02F
            );
            case NOON -> profile(
                0.40F, 0.58F, 0.70F,
                0.78F, 0.72F, 0.59F,
                0.65F, 0.62F, 0.53F,
                0.79F, 0.69F, 0.48F,
                0.20F, 0.89F,
                0.18F, 0.50F, 0.43F, 0.11F,
                1.04F, 0.00F, 0.35F, 0.01F
            );
            case DUSK -> profile(
                0.25F, 0.24F, 0.32F,
                0.96F, 0.33F, 0.13F,
                0.54F, 0.35F, 0.25F,
                0.92F, 0.46F, 0.20F,
                0.10F, 0.66F,
                0.42F, 0.92F, 0.58F, 0.14F,
                0.83F, 0.04F, 0.62F, 0.06F
            );
            case EVENING -> profile(
                0.08F, 0.08F, 0.13F,
                0.28F, 0.20F, 0.22F,
                0.19F, 0.16F, 0.18F,
                0.58F, 0.38F, 0.22F,
                0.08F, 0.60F,
                0.37F, 0.70F, 0.49F, 0.12F,
                0.62F, 0.08F, 0.55F, 0.15F
            );
            case MIDNIGHT -> profile(
                0.015F, 0.017F, 0.033F,
                0.07F, 0.07F, 0.10F,
                0.07F, 0.07F, 0.09F,
                0.48F, 0.35F, 0.25F,
                0.07F, 0.56F,
                0.30F, 0.55F, 0.44F, 0.11F,
                0.48F, 0.10F, 0.45F, 0.20F
            );
        };
    }

    private static EnvironmentProfile ascent(DayPhase phase) {
        return switch (phase) {
            case DAWN -> profile(
                0.22F, 0.31F, 0.67F,
                0.71F, 0.43F, 0.92F,
                0.43F, 0.47F, 0.72F,
                0.49F, 0.92F, 1.00F,
                0.18F, 0.91F,
                0.39F, 0.82F, 0.47F, 0.14F,
                1.02F, 0.11F, 0.27F, 0.46F
            );
            case MORNING -> profile(
                0.20F, 0.52F, 0.88F,
                0.59F, 0.76F, 0.99F,
                0.47F, 0.65F, 0.83F,
                0.42F, 0.94F, 1.00F,
                0.24F, 0.98F,
                0.25F, 0.66F, 0.43F, 0.13F,
                1.10F, 0.06F, 0.20F, 0.35F
            );
            case NOON -> profile(
                0.18F, 0.55F, 0.95F,
                0.52F, 0.84F, 1.00F,
                0.48F, 0.72F, 0.90F,
                0.47F, 0.97F, 1.00F,
                0.29F, 1.00F,
                0.19F, 0.58F, 0.40F, 0.12F,
                1.17F, 0.04F, 0.17F, 0.30F
            );
            case DUSK -> profile(
                0.18F, 0.15F, 0.50F,
                0.94F, 0.35F, 0.79F,
                0.43F, 0.32F, 0.60F,
                0.73F, 0.55F, 1.00F,
                0.15F, 0.84F,
                0.49F, 0.94F, 0.50F, 0.15F,
                0.92F, 0.17F, 0.35F, 0.58F
            );
            case EVENING -> profile(
                0.035F, 0.040F, 0.17F,
                0.18F, 0.12F, 0.35F,
                0.13F, 0.13F, 0.28F,
                0.46F, 0.69F, 1.00F,
                0.11F, 0.76F,
                0.50F, 1.05F, 0.44F, 0.14F,
                0.68F, 0.22F, 0.31F, 0.72F
            );
            case MIDNIGHT -> profile(
                0.006F, 0.008F, 0.05F,
                0.07F, 0.06F, 0.20F,
                0.06F, 0.07F, 0.17F,
                0.37F, 0.71F, 1.00F,
                0.09F, 0.71F,
                0.56F, 1.18F, 0.41F, 0.14F,
                0.55F, 0.31F, 0.26F, 0.87F
            );
        };
    }

    private static EnvironmentProfile summit(DayPhase phase) {
        return switch (phase) {
            case DAWN -> profile(
                0.25F, 0.37F, 0.57F,
                0.79F, 0.73F, 0.76F,
                0.63F, 0.68F, 0.75F,
                0.81F, 0.92F, 1.00F,
                0.15F, 0.79F,
                0.28F, 0.84F, 0.70F, 0.10F,
                0.98F, 0.04F, 0.68F, 0.13F
            );
            case MORNING -> profile(
                0.32F, 0.57F, 0.77F,
                0.75F, 0.86F, 0.95F,
                0.67F, 0.76F, 0.84F,
                0.87F, 0.95F, 1.00F,
                0.20F, 0.87F,
                0.18F, 0.70F, 0.64F, 0.09F,
                1.05F, 0.02F, 0.59F, 0.09F
            );
            case NOON -> profile(
                0.31F, 0.61F, 0.84F,
                0.79F, 0.91F, 1.00F,
                0.72F, 0.83F, 0.91F,
                0.90F, 0.98F, 1.00F,
                0.23F, 0.92F,
                0.12F, 0.61F, 0.58F, 0.085F,
                1.10F, 0.01F, 0.52F, 0.06F
            );
            case DUSK -> profile(
                0.16F, 0.23F, 0.41F,
                0.75F, 0.56F, 0.63F,
                0.48F, 0.50F, 0.62F,
                0.73F, 0.86F, 1.00F,
                0.12F, 0.70F,
                0.33F, 0.93F, 0.72F, 0.11F,
                0.84F, 0.08F, 0.76F, 0.18F
            );
            case EVENING -> profile(
                0.025F, 0.045F, 0.13F,
                0.13F, 0.17F, 0.31F,
                0.12F, 0.16F, 0.28F,
                0.60F, 0.79F, 1.00F,
                0.09F, 0.64F,
                0.38F, 1.02F, 0.67F, 0.10F,
                0.62F, 0.14F, 0.71F, 0.30F
            );
            case MIDNIGHT -> profile(
                0.005F, 0.010F, 0.04F,
                0.045F, 0.075F, 0.18F,
                0.06F, 0.09F, 0.17F,
                0.55F, 0.78F, 1.00F,
                0.075F, 0.58F,
                0.44F, 1.12F, 0.62F, 0.10F,
                0.48F, 0.20F, 0.66F, 0.45F
            );
        };
    }

    private static EnvironmentProfile pinnacle(DayPhase phase) {
        return switch (phase) {
            case DAWN -> profile(
                0.20F, 0.08F, 0.37F,
                0.95F, 0.54F, 0.17F,
                0.48F, 0.27F, 0.49F,
                1.00F, 0.70F, 0.19F,
                0.13F, 0.77F,
                0.55F, 1.05F, 0.48F, 0.16F,
                1.02F, 0.25F, 0.48F, 0.74F
            );
            case MORNING -> profile(
                0.18F, 0.27F, 0.65F,
                0.73F, 0.51F, 0.91F,
                0.42F, 0.38F, 0.65F,
                0.56F, 1.00F, 0.76F,
                0.19F, 0.86F,
                0.42F, 0.92F, 0.44F, 0.15F,
                1.11F, 0.18F, 0.39F, 0.65F
            );
            case NOON -> profile(
                0.13F, 0.36F, 0.75F,
                0.58F, 0.75F, 1.00F,
                0.41F, 0.53F, 0.74F,
                0.50F, 1.00F, 0.83F,
                0.23F, 0.92F,
                0.33F, 0.82F, 0.41F, 0.14F,
                1.20F, 0.13F, 0.34F, 0.58F
            );
            case DUSK -> profile(
                0.13F, 0.05F, 0.30F,
                1.00F, 0.31F, 0.43F,
                0.44F, 0.21F, 0.48F,
                1.00F, 0.38F, 0.63F,
                0.10F, 0.68F,
                0.66F, 1.28F, 0.53F, 0.17F,
                0.88F, 0.35F, 0.56F, 0.82F
            );
            case EVENING -> profile(
                0.014F, 0.009F, 0.07F,
                0.19F, 0.05F, 0.30F,
                0.11F, 0.06F, 0.22F,
                0.44F, 0.94F, 1.00F,
                0.075F, 0.60F,
                0.73F, 1.42F, 0.49F, 0.16F,
                0.65F, 0.45F, 0.50F, 0.91F
            );
            case MIDNIGHT -> profile(
                0.003F, 0.001F, 0.025F,
                0.07F, 0.015F, 0.17F,
                0.045F, 0.025F, 0.13F,
                0.35F, 0.85F, 1.00F,
                0.055F, 0.53F,
                0.84F, 1.58F, 0.46F, 0.17F,
                0.50F, 0.58F, 0.44F, 0.97F
            );
        };
    }

    private static EnvironmentProfile profile(
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
        return new EnvironmentProfile(
            skyTopR,
            skyTopG,
            skyTopB,
            skyHorizonR,
            skyHorizonG,
            skyHorizonB,
            fogR,
            fogG,
            fogB,
            particleR,
            particleG,
            particleB,
            fogStart,
            fogEnd,
            skyOpacity,
            particleDensity,
            particleSpeed,
            particleSize,
            brightness,
            pulseStrength,
            turbulence,
            emissiveRatio
        );
    }
}
