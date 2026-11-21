package com.example.examplemod.client.render;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientVisualTweaks {

    private ClientVisualTweaks() {}

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ALObjects.Particles.APOTH_CRIT.get(), (type, level, x, y, z, vx, vy, vz) -> null);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !canReplaceFog(minecraft)) {
            return;
        }
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        event.setCanceled(true);
    }

    private static boolean canReplaceFog(Minecraft minecraft) {
        if (minecraft.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return false;
        }
        if (minecraft.player.hasEffect(MobEffects.BLINDNESS)) {
            return false;
        }
        return !minecraft.player.hasEffect(MobEffects.DARKNESS);
    }
}