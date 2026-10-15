package com.nf121.examplemod.client.environment;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.client.Minecraft;

public enum VisualTier {
    HAVEN,
    FRONTIER,
    ASCENT,
    SUMMIT,
    PINNACLE;

    public static VisualTier getLocalPlayerTier() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return HAVEN;
        }

        WorldTier tier = WorldTier.getTier(minecraft.player);

        if (tier == null) {
            return HAVEN;
        }

        return switch (tier) {
            case HAVEN -> HAVEN;
            case FRONTIER -> FRONTIER;
            case ASCENT -> ASCENT;
            case SUMMIT -> SUMMIT;
            case PINNACLE -> PINNACLE;
        };
    }
}
