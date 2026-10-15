package com.example.examplemod.network.villager;

import com.example.examplemod.registry.ModPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class VillagerSyncEvents {

    // 1. 进入视野时同步
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractVillager villager && event.getEntity() instanceof ServerPlayer player) {
            syncVillagerTradesToPlayer(villager, player);
        }
    }

    // 2. 离开视野时通知客户端清理（也可以在客户端监听 ClientTick/实体卸载）
    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof AbstractVillager villager && event.getEntity() instanceof ServerPlayer player) {
            // 发送一个 offers 为空的包，表示清除该村民的缓存
            PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundVillagerTradesPacket(villager.getId(), new MerchantOffers()));
        }
    }

    // 辅助工具方法：向指定玩家同步
    public static void syncVillagerTradesToPlayer(AbstractVillager villager, ServerPlayer player) {
        MerchantOffers offers = villager.getOffers();
        if (offers != null && !offers.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundVillagerTradesPacket(villager.getId(), offers));
        }
    }

    // 辅助工具方法：广播给所有正在追踪该村民的玩家（当交易升级或就职时调用）
    public static void syncVillagerTradesToTrackingPlayers(AbstractVillager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers != null) {
            PacketDistributor.sendToPlayersTrackingEntity(villager, new ModPayloads.ClientboundVillagerTradesPacket(villager.getId(), offers));
        }
    }
}