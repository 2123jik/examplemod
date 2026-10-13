package com.example.examplemod.event.defense;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientDefenseInputHandler {

    private static boolean wasDefendingLastTick = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean hasEntity = PlayerGrabManager.isHoldingEntity(mc.player);
        boolean isRightClickHeld = mc.options.keyUse.isDown();
        boolean isDefending = hasEntity && isRightClickHeld;

        if (isDefending != wasDefendingLastTick) {
            wasDefendingLastTick = isDefending;

            // 发送给服务端
            PacketDistributor.sendToServer(new ServerboundDefendPayload(isDefending));

            // 联动 PlayerAnimator
            if (isDefending) {
                // 播放举盾动作代码
            } else {
                // 停止举盾动作代码
            }
        }
    }
}