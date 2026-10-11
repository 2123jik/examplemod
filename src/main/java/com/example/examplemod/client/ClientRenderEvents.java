package com.example.examplemod.client;

import com.example.examplemod.client.environment.ClientEnvironmentState;
import com.example.examplemod.client.environment.EnvironmentProfile;
import com.example.examplemod.client.particle.AmbientParticleRenderer;
import com.example.examplemod.client.render.TierSkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientRenderEvents {

    private ClientRenderEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientEnvironmentState.tick(Minecraft.getInstance());
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        TierSkyRenderer.render(event);
        AmbientParticleRenderer.render(event);
    }

    public static void onComputeFogColor(
        ViewportEvent.ComputeFogColor event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!canReplaceFog(minecraft)) {
            return;
        }

        EnvironmentProfile profile =
            ClientEnvironmentState.getProfile((float) event.getPartialTick());

        event.setRed(profile.fogR());
        event.setGreen(profile.fogG());
        event.setBlue(profile.fogB());
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!canReplaceFog(minecraft)) {
            return;
        }

        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }

        EnvironmentProfile profile =
            ClientEnvironmentState.getProfile((float) event.getPartialTick());

        float renderDistance =
            minecraft.options.getEffectiveRenderDistance() * 16.0F;

        float nearDistance =
            Math.max(2.0F, renderDistance * profile.fogStart());

        float farDistance =
            Math.max(
                nearDistance + 8.0F,
                renderDistance * profile.fogEnd()
            );

        event.setNearPlaneDistance(nearDistance);
        event.setFarPlaneDistance(farDistance);
        event.setCanceled(true);
    }

    public static void onLoggingOut(
        ClientPlayerNetworkEvent.LoggingOut event
    ) {
        ClientEnvironmentState.reset();
    }

    private static boolean canReplaceFog(Minecraft minecraft) {
        if (
            minecraft.gameRenderer
                .getMainCamera()
                .getFluidInCamera() != FogType.NONE
        ) {
            return false;
        }

        if (minecraft.player.hasEffect(MobEffects.BLINDNESS)) {
            return false;
        }

        return !minecraft.player.hasEffect(MobEffects.DARKNESS);
    }
}
