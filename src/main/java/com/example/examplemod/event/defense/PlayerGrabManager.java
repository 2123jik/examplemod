package com.example.examplemod.event.defense;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerGrabManager {
    public static final Map<UUID, GrabData> GRABBED_ENTITIES = new ConcurrentHashMap<>();

    public static class GrabData {
        public final LivingEntity entity;
        public boolean isDefending = false;

        public GrabData(LivingEntity entity) {
            this.entity = entity;
        }
    }

    public static boolean isHoldingEntity(Player player) {
        return GRABBED_ENTITIES.containsKey(player.getUUID());
    }

    public static GrabData getGrabData(Player player) {
        return GRABBED_ENTITIES.get(player.getUUID());
    }

    public static void grab(Player player, LivingEntity entity) {
        entity.setNoGravity(true);
        GRABBED_ENTITIES.put(player.getUUID(), new GrabData(entity));
    }

    public static void release(Player player) {
        GrabData data = GRABBED_ENTITIES.remove(player.getUUID());
        if (data != null && data.entity.isAlive()) {
            data.entity.setNoGravity(false);
            // 放下时确保关闭自转渲染
            if (data.isDefending) {
                PacketDistributor.sendToPlayersTrackingEntity(
                    data.entity, 
                    new ClientboundSyncDefensePayload(data.entity.getId(), false)
                );
            }
        }
    }
}