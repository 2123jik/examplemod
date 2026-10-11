package com.example.examplemod.deathrecap;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = "examplemod")
public class DeathRecapEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        // 只捕获玩家承受的伤害快照
        if (event.getEntity() instanceof ServerPlayer player) {
            DamageEventSnapshot snapshot = new DamageEventSnapshot(event.getSource(), event.getNewDamage(), player);
            PlayerDamageTracker.recordDamage(player.getUUID(), snapshot);
        }
    }

    // 在 DeathRecapEventHandler.java 中修正：
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 关键修复：死亡事件触发时，当场为【致命一击】拍下快照并入队！
            // 这样即使 Post 尚未派发，致命伤也绝对不会漏掉
            DamageEventSnapshot fatalShot = new DamageEventSnapshot(event.getSource(), 0.0f, player);
            PlayerDamageTracker.recordDamage(player.getUUID(), fatalShot);

            List<DamageEventSnapshot> records = PlayerDamageTracker.getAndClear(player.getUUID());
            if (!records.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new DeathRecapPayload(records));
            }
        }
    }
}