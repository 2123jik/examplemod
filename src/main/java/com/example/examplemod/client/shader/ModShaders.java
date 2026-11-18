package com.example.examplemod.client.shader;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ModShaders {
    private static ShaderInstance spaceSlashShader;

    public static final ResourceLocation SLASH_PACK_TEX =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/particle/space_slash_pack.png");
    public static final ResourceLocation SLASH_NOISE_TEX =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/particle/space_slash_noise.png");

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "space_slash"),
                        DefaultVertexFormat.PARTICLE
                ),
                shader -> spaceSlashShader = shader
        );
    }

    public static final ParticleRenderType SPACE_SLASH_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            // 关键：采用标准透明混合，让深黑虚空核心能够正常遮蔽背后的方块/生物
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );

            // 绑定 0 号与 1 号纹理
            RenderSystem.setShaderTexture(0, SLASH_PACK_TEX);
            RenderSystem.setShaderTexture(1, SLASH_NOISE_TEX);

            RenderSystem.setShader(() -> spaceSlashShader);
            return tesselator.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "examplemod:space_slash";
        }
    };
}