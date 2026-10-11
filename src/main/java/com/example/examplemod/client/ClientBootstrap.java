package com.example.examplemod.client;

import com.example.examplemod.client.render.ExampleShaders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void initialize(IEventBus modEventBus) {
        modEventBus.addListener(
            (RegisterShadersEvent event) -> {
                try {
                    ExampleShaders.register(event);
                } catch (Exception exception) {
                    throw new RuntimeException(
                        "Failed to register examplemod shaders",
                        exception
                    );
                }
            }
        );

        NeoForge.EVENT_BUS.addListener(
            ClientRenderEvents::onClientTick
        );

        NeoForge.EVENT_BUS.addListener(
            ClientRenderEvents::onRenderLevel
        );

        NeoForge.EVENT_BUS.addListener(
            ClientRenderEvents::onComputeFogColor
        );

        NeoForge.EVENT_BUS.addListener(
            ClientRenderEvents::onRenderFog
        );

        NeoForge.EVENT_BUS.addListener(
            ClientRenderEvents::onLoggingOut
        );
    }
}
