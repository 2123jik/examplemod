package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.gui.PinnacleAdaptiveHud;
import com.example.examplemod.client.tooltip.ModelStageRenderer;
import com.example.examplemod.client.tooltip.TooltipHandler;
import com.example.examplemod.client.tooltip.TooltipRules;
import com.example.examplemod.client.tooltip.VisualComponents;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.skill.gui.SkillTreeFragment;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

@Mod(value = ExampleMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public final class ExampleModClient {

    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
            "key.examplemod.open_skill_tree",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.examplemod"
    );

    public ExampleModClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Mod 总线监听
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerKeys);
        modBus.addListener(this::registerTooltipComponents);
        modBus.addListener(this::registerReloadListeners);
        modBus.addListener(this::onModifyBakingResult);

        // 注册游戏总线组件
        NeoForge.EVENT_BUS.register(TooltipHandler.class);
        NeoForge.EVENT_BUS.register(ModelStageRenderer.class);
    }

    private static final ResourceLocation ADAPTIVE_HUD =
            ResourceLocation.fromNamespaceAndPath(
                    "examplemod",
                    "adaptive_hud"
            );

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ADAPTIVE_HUD,
                PinnacleAdaptiveHud::renderLayer
        );
    }


    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUMMONED_SUPPORT_DRAGON.get(), EnderDragonRenderer::new);
        event.registerEntityRenderer(
                EntityType.EXPERIENCE_ORB,
                HiddenExperienceOrbRenderer::new
        );
        event.registerEntityRenderer(ModEntities.SUMMONED_CAVE_SPIDER.get(), CaveSpiderRenderer::new);
    }
    private static final class HiddenExperienceOrbRenderer
            extends EntityRenderer<ExperienceOrb> {

        private static final ResourceLocation UNUSED_TEXTURE =
                ResourceLocation.withDefaultNamespace(
                        "textures/entity/experience_orb.png"
                );

        private HiddenExperienceOrbRenderer(
                EntityRendererProvider.Context context
        ) {
            super(context);
            shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(
                ExperienceOrb entity,
                Frustum frustum,
                double cameraX,
                double cameraY,
                double cameraZ
        ) {
            return false;
        }

        @Override
        public void render(
                ExperienceOrb entity,
                float entityYaw,
                float partialTick,
                PoseStack poseStack,
                MultiBufferSource buffers,
                int packedLight
        ) {
            // 完全不绘制模型、名字和其他原版内容。
        }

        @Override
        public ResourceLocation getTextureLocation(ExperienceOrb entity) {
            return UNUSED_TEXTURE;
        }
    }
    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILL_TREE);
    }

    private void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        VisualComponents.registerFactories(event);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(TooltipRules.INSTANCE);
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
        if (vanillaModel != null) models.put(myLoc, vanillaModel);
    }

    // 全局快捷键响应
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_SKILL_TREE.consumeClick()) {
            MuiModApi.openScreen(new SkillTreeFragment());
        }
    }
}