package com.example.examplemod.client;

import com.example.examplemod.network.ArrowCritPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleArrowCrit(ArrowCritPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CritDamageHudRenderer.triggerCrit(payload));
    }

}