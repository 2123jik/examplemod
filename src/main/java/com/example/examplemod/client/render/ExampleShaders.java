package com.example.examplemod.client.render;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

public final class ExampleShaders {

    public static ShaderInstance tierSky;
    public static ShaderInstance ambientParticle;

    private ExampleShaders() {
    }

    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "tier_sky"
                ),
                DefaultVertexFormat.POSITION_COLOR
            ),
            shader -> tierSky = shader
        );

        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "ambient_particle"
                ),
                DefaultVertexFormat.POSITION_TEX_COLOR
            ),
            shader -> ambientParticle = shader
        );
    }
}
