package com.example.examplemod.export.core;

import net.neoforged.fml.ModList;

public final class ModGuard {

    private ModGuard() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }
}