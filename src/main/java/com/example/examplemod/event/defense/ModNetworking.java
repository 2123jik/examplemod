package com.example.examplemod.event.defense;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "examplemod")
public class ModNetworking {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // 注册 C2S
        registrar.playToServer(
            ServerboundDefendPayload.TYPE,
            ServerboundDefendPayload.STREAM_CODEC,
            ServerboundDefendPayload::handle
        );

        // 注册 S2C
        registrar.playToClient(
            ClientboundSyncDefensePayload.TYPE,
            ClientboundSyncDefensePayload.STREAM_CODEC,
            ClientboundSyncDefensePayload::handle
        );
    }
}