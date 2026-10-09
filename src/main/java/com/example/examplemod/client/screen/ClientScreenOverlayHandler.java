package com.example.examplemod.client.screen;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.SwapEquipmentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ClientScreenOverlayHandler {

    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 4;
    private static final int DOLL_WIDTH = 46;
    private static final int DOLL_HEIGHT = 78;

    // 面板尺寸计算：左侧槽位(18) + 中间娃娃(46) + 右侧槽位(18) + 间距与边距 = 94
    private static final int PANEL_WIDTH = PADDING * 2 + SLOT_SIZE * 2 + DOLL_WIDTH + 4;
    private static final int PANEL_HEIGHT = DOLL_HEIGHT + PADDING * 2;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final Set<Class<? extends Screen>> WHITELIST_SCREENS = Set.of(
            ContainerScreen.class,       // 单箱、大箱、末影箱
            ShulkerBoxScreen.class       // 潜影盒
            // 可根据需要增加：Barrel（木桶原版也是 ContainerScreen）
    );

    private static boolean shouldDisplay(Screen screen, Player player) {
        if (player == null || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        // 仅在白名单容器中启用
        if (!WHITELIST_SCREENS.contains(screen.getClass())) {
            return false;
        }

        return containerScreen.getGuiLeft() >= PANEL_WIDTH + 6;
    }


    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (!shouldDisplay(screen, player)) return;
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        int panelX = containerScreen.getGuiLeft() - PANEL_WIDTH - 4;
        int panelY = containerScreen.getGuiTop();

        // 绘制整体面板深色半透明背景与外边框
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC0101010);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF3C3C3C);

        // 绘制 3D 玩家布娃娃
        int dollX1 = panelX + PADDING + SLOT_SIZE + 2;
        int dollY1 = panelY + PADDING;
        int dollX2 = dollX1 + DOLL_WIDTH;
        int dollY2 = dollY1 + DOLL_HEIGHT;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                dollX1, dollY1,
                dollX2, dollY2,
                32,
                0.0625F,
                (float) mouseX, (float) mouseY,
                player
        );

        ItemStack hoveredStack = ItemStack.EMPTY;

        // 绘制左侧 4 个盔甲槽位
        int armorX = panelX + PADDING;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int slotY = panelY + PADDING + i * (SLOT_SIZE + 2);
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (renderSlot(graphics, mc.font, stack, armorX, slotY, mouseX, mouseY)) {
                hoveredStack = stack;
            }
        }

        // 绘制右侧主手与副手槽位
        int rightX = dollX2 + 2;
        int mainHandY = panelY + PADDING;
        int offHandY = panelY + PADDING + SLOT_SIZE + 2;

        if (renderSlot(graphics, mc.font, player.getMainHandItem(), rightX, mainHandY, mouseX, mouseY)) {
            hoveredStack = player.getMainHandItem();
        }
        if (renderSlot(graphics, mc.font, player.getOffhandItem(), rightX, offHandY, mouseX, mouseY)) {
            hoveredStack = player.getOffhandItem();
        }

        // 悬停提示 Tooltip（光标未拿取物品时展示）
        ItemStack carried = containerScreen.getMenu().getCarried();
        if (!hoveredStack.isEmpty() && carried.isEmpty()) {
            graphics.renderTooltip(mc.font, hoveredStack, mouseX, mouseY);
        }

        // 光标持有物品且位于自定义面板上方时在 Post 重新绘制光标物品，防止被面板背景遮盖
        if (!carried.isEmpty() && isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(mc.font, carried, mouseX - 8, mouseY - 8);
        }
    }

    @SubscribeEvent
    public static void onMouseClickPre(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        Minecraft mc = Minecraft.getInstance();

        if (!shouldDisplay(screen, mc.player)) return;
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        int panelX = containerScreen.getGuiLeft() - PANEL_WIDTH - 4;
        int panelY = containerScreen.getGuiTop();

        // 1. 只要鼠标位于面板范围内，立刻拦截原版处理（防止触发界面外丢弃物品）
        if (isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            event.setCanceled(true);

            // 2. 仅对鼠标左键 (0) 响应槽位装备交换
            if (event.getButton() == 0) {
                // 检测左侧 4 个盔甲槽
                int armorX = panelX + PADDING;
                for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                    int slotY = panelY + PADDING + i * (SLOT_SIZE + 2);
                    if (isInside(mouseX, mouseY, armorX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                        sendClick(ARMOR_SLOTS[i]);
                        return;
                    }
                }

                // 检测右侧双手槽
                int rightX = panelX + PADDING + SLOT_SIZE + 2 + DOLL_WIDTH + 2;
                int mainHandY = panelY + PADDING;
                int offHandY = panelY + PADDING + SLOT_SIZE + 2;

                if (isInside(mouseX, mouseY, rightX, mainHandY, SLOT_SIZE, SLOT_SIZE)) {
                    sendClick(EquipmentSlot.MAINHAND);
                    return;
                }

                if (isInside(mouseX, mouseY, rightX, offHandY, SLOT_SIZE, SLOT_SIZE)) {
                    sendClick(EquipmentSlot.OFFHAND);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseReleasePre(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen screen = event.getScreen();
        Minecraft mc = Minecraft.getInstance();

        if (!shouldDisplay(screen, mc.player)) return;
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;

        int panelX = containerScreen.getGuiLeft() - PANEL_WIDTH - 4;
        int panelY = containerScreen.getGuiTop();

        if (isInside(event.getMouseX(), event.getMouseY(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            event.setCanceled(true);
        }
    }

    private static void sendClick(EquipmentSlot slot) {
        PacketDistributor.sendToServer(new SwapEquipmentPayload(slot));
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static boolean renderSlot(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x60000000);
        graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFF2B2B2B);

        boolean hovered = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x40FFFFFF);
        }

        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }

        return hovered && !stack.isEmpty();
    }
}