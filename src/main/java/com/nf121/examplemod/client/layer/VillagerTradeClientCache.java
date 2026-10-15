package com.nf121.examplemod.client.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.Map;

public final class VillagerTradeClientCache {
    private static final Map<Integer, MerchantOffers> CACHE = new Int2ObjectOpenHashMap<>();

    public static void put(int entityId, MerchantOffers offers) {
        CACHE.put(entityId, offers);
    }

    public static MerchantOffers get(int entityId) {
        return CACHE.get(entityId);
    }

    public static void remove(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }
}