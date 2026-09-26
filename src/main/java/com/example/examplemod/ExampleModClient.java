package com.example.examplemod;

import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModMenus;
import com.example.examplemod.tooltip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT) // 此注解管理本类中的游戏运行期事件(Key, Tooltip等)
public final class ExampleModClient {

    public ExampleModClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // 统一在 MOD 总线上注册生命周期事件
        modBus.addListener(this::registerKeys);
        modBus.addListener(this::registerTooltipComponents);
        modBus.addListener(this::registerReloadListeners);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerScreens); // 核心添加：绑定工具包界面！

        TooltipHandler.register();
        NeoForge.EVENT_BUS.register(ModelStageRenderer.class);
        NeoForge.EVENT_BUS.register(TooltipInteraction.class);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUMMONED_CAVE_SPIDER.get(), CaveSpiderRenderer::new);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {

    }

    private void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        VisualComponents.registerFactories(event);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(TooltipRules.INSTANCE);
    }

    // ================= 下方为游戏运行期事件（GAME Bus） =================

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
    }

    @SubscribeEvent
    public static void handleKey(InputEvent.Key event) {
    }

    @SubscribeEvent
    public static void renderSlotDebug(ContainerScreenEvent.Render.Foreground event) {
        if (!Config.ENABLED.get() || !Config.SHOW_SLOT_DEBUG.get()) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) return;

        boolean empty = !slot.hasItem();
        Component status = Component.translatable(
                empty ? "examplemod.slot_debug.empty" : "examplemod.slot_debug.item",
                slot.index
        );
        int x = screen.getGuiLeft() + slot.x;
        int y = screen.getGuiTop() + slot.y - 10;
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, status, x, y,
                empty ? 0xFFFFAA00 : 0xFF55FF55, true);
    }

    @SubscribeEvent
    public static void tooltipWidth(RenderTooltipEvent.GatherComponents event) {
        if (Config.ENABLED.get()) {
            event.setMaxWidth(Config.MAX_TOOLTIP_WIDTH.get());
        }
    }

    @SubscribeEvent
    public static void tooltipColors(RenderTooltipEvent.Color event) {
        if (!Config.ENABLED.get()) return;

        event.setBackground(Config.getBackgroundColor());
        int border = Config.getBorderColor();

        if (Config.RARITY_THEME.get()) {
            Integer rarityColor = event.getItemStack().getRarity().color().getColor();
            if (rarityColor != null && rarityColor != 0) {
                border = 0xFF000000 | rarityColor;
            }
        }
        event.setBorderStart(border);
        event.setBorderEnd(border);
    }
}