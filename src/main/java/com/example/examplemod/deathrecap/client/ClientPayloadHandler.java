package com.example.examplemod.deathrecap.client;

import com.example.examplemod.registry.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleDeathRecap(ModPayloads.DeathRecapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientDeathRecapCache.setRecords(payload.records());
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§c[死亡战报] §e死亡快照已就绪，可在死亡界面点击按钮或按键查看！"),
                        false
                );
            }
        });
    }
}