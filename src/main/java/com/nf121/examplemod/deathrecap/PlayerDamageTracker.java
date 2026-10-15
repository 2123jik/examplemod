package com.nf121.examplemod.deathrecap;

import java.util.*;

public class PlayerDamageTracker {
    // 统一存储 DamageEventSnapshot
    private static final Map<UUID, LinkedList<DamageEventSnapshot>> HISTORY = new HashMap<>();
    private static final int MAX_RECORDS = 8;

    public static void recordDamage(UUID playerUuid, DamageEventSnapshot snapshot) {
        LinkedList<DamageEventSnapshot> list = HISTORY.computeIfAbsent(playerUuid, k -> new LinkedList<>());
        if (list.size() >= MAX_RECORDS) {
            list.removeFirst();
        }
        list.addLast(snapshot);
    }

    public static List<DamageEventSnapshot> getAndClear(UUID playerUuid) {
        LinkedList<DamageEventSnapshot> records = HISTORY.remove(playerUuid);
        return records != null ? records : Collections.emptyList();
    }
}