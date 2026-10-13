package com.example.examplemod.event.defense;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDefenseManager {
    private static final Set<Integer> SPINNING_ENTITIES = ConcurrentHashMap.newKeySet();

    public static void setDefending(int entityId, boolean defending) {
        if (defending) {
            SPINNING_ENTITIES.add(entityId);
        } else {
            SPINNING_ENTITIES.remove(entityId);
        }
    }

    public static boolean isEntitySpinDefending(int entityId) {
        return SPINNING_ENTITIES.contains(entityId);
    }
}