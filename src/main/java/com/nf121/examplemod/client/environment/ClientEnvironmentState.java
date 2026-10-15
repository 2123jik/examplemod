package com.nf121.examplemod.client.environment;

import com.nf121.examplemod.client.particle.AmbientParticleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class ClientEnvironmentState {

    private static final int TIER_TRANSITION_TICKS = 40;

    private static VisualTier currentTier = VisualTier.HAVEN;
    private static VisualTier previousTier = VisualTier.HAVEN;

    private static float tierTransition = 1.0F;

    private static EnvironmentProfile previousFrameProfile;
    private static EnvironmentProfile currentFrameProfile;

    private static ClientLevel trackedLevel;

    private ClientEnvironmentState() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }
        EnvironmentColorSampler.INSTANCE.tick(minecraft);
        if (trackedLevel != minecraft.level) {
            trackedLevel = minecraft.level;
            currentTier = VisualTier.getLocalPlayerTier();
            previousTier = currentTier;
            tierTransition = 1.0F;

            AmbientParticleManager.INSTANCE.clear();
        }

        VisualTier resolvedTier = VisualTier.getLocalPlayerTier();

        if (resolvedTier != currentTier) {
            previousTier = currentTier;
            currentTier = resolvedTier;
            tierTransition = 0.0F;
        }

        if (tierTransition < 1.0F) {
            tierTransition += 1.0F / TIER_TRANSITION_TICKS;

            if (tierTransition > 1.0F) {
                tierTransition = 1.0F;
            }
        }

        double time = minecraft.level.getDayTime();

        DayPhase.DayPhaseState phase = DayPhase.evaluate(time);

        EnvironmentProfile oldTierProfile =
            EnvironmentProfiles.resolve(previousTier, phase);

        EnvironmentProfile newTierProfile =
            EnvironmentProfiles.resolve(currentTier, phase);

        float smoothTierTransition = DayPhase.smoothStep(tierTransition);

        EnvironmentProfile result = EnvironmentProfile.lerp(
            oldTierProfile,
            newTierProfile,
            smoothTierTransition
        );

        float rain = minecraft.level.getRainLevel(1.0F);
        float thunder = minecraft.level.getThunderLevel(1.0F);

        result = result.applyWeather(rain, thunder);

        previousFrameProfile =
            currentFrameProfile == null ? result : currentFrameProfile;

        currentFrameProfile = result;

        AmbientParticleManager.INSTANCE.tick(
            minecraft,
            currentTier,
            result
        );
    }

    public static EnvironmentProfile getProfile(float partialTick) {
        if (currentFrameProfile == null) {
            DayPhase.DayPhaseState phase = DayPhase.evaluate(6000.0);
            return EnvironmentProfiles.resolve(VisualTier.HAVEN, phase);
        }

        if (previousFrameProfile == null) {
            return currentFrameProfile;
        }

        return EnvironmentProfile.lerp(
            previousFrameProfile,
            currentFrameProfile,
            partialTick
        );
    }

    public static VisualTier getCurrentTier() {
        return currentTier;
    }

    public static float getTierTransition() {
        return tierTransition;
    }

    public static void reset() {
        trackedLevel = null;
        currentTier = VisualTier.HAVEN;
        previousTier = VisualTier.HAVEN;
        tierTransition = 1.0F;
        previousFrameProfile = null;
        currentFrameProfile = null;
        EnvironmentColorSampler.INSTANCE.reset(); // <- 重置采样器
        AmbientParticleManager.INSTANCE.clear();
    }
}
