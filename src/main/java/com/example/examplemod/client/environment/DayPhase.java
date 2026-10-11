package com.example.examplemod.client.environment;

import net.minecraft.util.Mth;

public enum DayPhase {
    DAWN(23000),
    MORNING(1000),
    NOON(5000),
    DUSK(9000),
    EVENING(13000),
    MIDNIGHT(18000);

    private final int startTime;

    DayPhase(int startTime) {
        this.startTime = startTime;
    }

    public int startTime() {
        return this.startTime;
    }

    public DayPhase next() {
        return switch (this) {
            case DAWN -> MORNING;
            case MORNING -> NOON;
            case NOON -> DUSK;
            case DUSK -> EVENING;
            case EVENING -> MIDNIGHT;
            case MIDNIGHT -> DAWN;
        };
    }

    public static DayPhaseState evaluate(double rawTime) {
        double time = rawTime % 24000.0;

        if (time < 0.0) {
            time += 24000.0;
        }

        DayPhase phase;
        double adjustedTime = time;
        double start;
        double end;

        if (time >= 23000.0 || time < 1000.0) {
            phase = DAWN;
            start = 23000.0;
            end = 25000.0;

            if (adjustedTime < 1000.0) {
                adjustedTime += 24000.0;
            }
        } else if (time < 5000.0) {
            phase = MORNING;
            start = 1000.0;
            end = 5000.0;
        } else if (time < 9000.0) {
            phase = NOON;
            start = 5000.0;
            end = 9000.0;
        } else if (time < 13000.0) {
            phase = DUSK;
            start = 9000.0;
            end = 13000.0;
        } else if (time < 18000.0) {
            phase = EVENING;
            start = 13000.0;
            end = 18000.0;
        } else {
            phase = MIDNIGHT;
            start = 18000.0;
            end = 23000.0;
        }

        float linearProgress = (float) ((adjustedTime - start) / (end - start));
        linearProgress = Mth.clamp(linearProgress, 0.0F, 1.0F);

        float smoothProgress = smoothStep(linearProgress);

        return new DayPhaseState(
            phase,
            phase.next(),
            linearProgress,
            smoothProgress
        );
    }

    public static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    public record DayPhaseState(
        DayPhase current,
        DayPhase next,
        float linearProgress,
        float smoothProgress
    ) {
    }
}
