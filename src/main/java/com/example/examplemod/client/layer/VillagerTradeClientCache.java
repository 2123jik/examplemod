package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class VillagerTradeClientCache {

    private static final Map<Integer, MerchantOffers> CACHE = new Int2ObjectOpenHashMap<>();

    private VillagerTradeClientCache() {}

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

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}