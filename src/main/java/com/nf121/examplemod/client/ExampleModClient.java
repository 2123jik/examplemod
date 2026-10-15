package com.nf121.examplemod.client;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.client.environment.ClientEnvironmentState;
import com.nf121.examplemod.client.environment.EnvironmentProfile;
import com.nf121.examplemod.client.gui.PinnacleAdaptiveHud;
import com.nf121.examplemod.client.layer.MobOverheadRenderer;
import com.nf121.examplemod.client.particle.AmbientParticleRenderer;
import com.nf121.examplemod.client.render.ExampleShaders;
import com.nf121.examplemod.client.render.TierSkyRenderer;
import com.nf121.examplemod.skill.gui.SkillTreeFragment;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

@Mod(value = ExampleMod.MOD_ID, dist = Dist.CLIENT)
public final class ExampleModClient {

    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
            "key.examplemod.open_skill_tree",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.examplemod"
    );

    private static final ResourceLocation ADAPTIVE_HUD =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "adaptive_hud");

    public ExampleModClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // ==================== 1. Mod 总线注册 (初始化与资源注册阶段) ====================
        modBus.addListener(this::onRegisterShaders);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerKeys);
        modBus.addListener(this::onModifyBakingResult);
        modBus.addListener(this::onRegisterParticleProviders);
        modBus.addListener(this::registerGuiLayers);

        // ==================== 2. NeoForge 游戏总线注册 (游戏运行与渲染阶段) ====================
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(this::onRenderEntity);
        NeoForge.EVENT_BUS.addListener(this::onComputeFogColor);
        NeoForge.EVENT_BUS.addListener(this::onRenderFog);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    // -----------------------------------------------------------------------------------------
    // Mod 总线事件回调
    // -----------------------------------------------------------------------------------------

    private void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ExampleShaders.register(event);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to register examplemod shaders", exception);
        }
    }



    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.EXPERIENCE_ORB, HiddenExperienceOrbRenderer::new);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILL_TREE);
    }

    private void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ALObjects.Particles.APOTH_CRIT.get(), (type, level, x, y, z, vx, vy, vz) -> null);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ADAPTIVE_HUD, PinnacleAdaptiveHud::renderLayer);
    }

    private void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        redirectItemModel(models, "portable_furnace", "furnace");
        redirectItemModel(models, "portable_stonecutter", "stonecutter");
        redirectItemModel(models, "portable_anvil", "anvil");
        redirectItemModel(models, "portable_brewing_stand", "brewing_stand");
    }

    private static void redirectItemModel(Map<ModelResourceLocation, BakedModel> models, String myItem, String vanillaItem) {
        ModelResourceLocation vanillaLoc = new ModelResourceLocation(ResourceLocation.withDefaultNamespace(vanillaItem), "inventory");
        ModelResourceLocation myLoc = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, myItem), "inventory");
        BakedModel vanillaModel = models.get(vanillaLoc);
        if (vanillaModel != null) {
            models.put(myLoc, vanillaModel);
        }
    }

    // -----------------------------------------------------------------------------------------
    // NeoForge 游戏总线事件回调 (快捷键、环境及世界渲染)
    // -----------------------------------------------------------------------------------------

    private void onClientTick(ClientTickEvent.Post event) {
        // 1. 处理自定义按键
        while (OPEN_SKILL_TREE.consumeClick()) {
            MuiModApi.openScreen(new SkillTreeFragment());
        }

        // 2. 环境状态更新
        ClientEnvironmentState.tick(Minecraft.getInstance());
    }

    private void onRenderLevel(RenderLevelStageEvent event) {
        TierSkyRenderer.render(event);
        AmbientParticleRenderer.render(event);
    }

    private void onRenderEntity(RenderNameTagEvent event) {
        MobOverheadRenderer.renderOverhead(event);
    }

    private void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !canReplaceFog(minecraft)) {
            return;
        }

        EnvironmentProfile profile = ClientEnvironmentState.getProfile((float) event.getPartialTick());
        event.setRed(profile.fogR());
        event.setGreen(profile.fogG());
        event.setBlue(profile.fogB());
    }

    private void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !canReplaceFog(minecraft)) {
            return;
        }

        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }

        EnvironmentProfile profile = ClientEnvironmentState.getProfile((float) event.getPartialTick());
        float renderDistance = minecraft.options.getEffectiveRenderDistance() * 16.0F;
        float nearDistance = Math.max(2.0F, renderDistance * profile.fogStart());
        float farDistance = Math.max(nearDistance + 8.0F, renderDistance * profile.fogEnd());

        event.setNearPlaneDistance(nearDistance);
        event.setFarPlaneDistance(farDistance);
        event.setCanceled(true);
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEnvironmentState.reset();
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

    // -----------------------------------------------------------------------------------------
    // 自定义内部渲染器
    // -----------------------------------------------------------------------------------------

    private static final class HiddenExperienceOrbRenderer extends EntityRenderer<ExperienceOrb> {

        private static final ResourceLocation UNUSED_TEXTURE =
                ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");

        private HiddenExperienceOrbRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(ExperienceOrb entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
            return false;
        }

        @Override
        public void render(ExperienceOrb entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
            // 完全不绘制
        }

        @Override
        public ResourceLocation getTextureLocation(ExperienceOrb entity) {
            return UNUSED_TEXTURE;
        }
    }
}