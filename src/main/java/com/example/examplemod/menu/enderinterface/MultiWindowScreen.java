package com.example.examplemod.menu.enderinterface;

import com.example.examplemod.menu.enderinterface.MultiWindowClient.WindowEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class MultiWindowScreen extends Screen {
    private static final int TITLE_H = 16;
    private static final int CLOSE_SIZE = 12;

    private static final int INV_W = 176;
    private static final int INV_BODY_H = 91;
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // 反射缓存：在渲染子窗口期间临时抑制 EMI / JEI 叠加层，防止被 Scissor 裁切并穿透窗口
    private static boolean emiChecked = false;
    @Nullable private static Field emiEnabledField;
    private static boolean jeiMissing = false;
    @Nullable private static Method jeiGetToggleState;
    @Nullable private static Field jeiOverlayEnabledField;

    @Nullable private WindowEntry draggingWindow;
    private double dragOffsetX;
    private double dragOffsetY;

    // 公共玩家物品栏状态
    private int invX;
    private int invY;
    private boolean invPosInitialized = false;
    private boolean sharedInvCollapsed = false;
    private boolean sharedInvInFront = true;
    private boolean draggingSharedInv = false;
    private double dragInvOffsetX;
    private double dragInvOffsetY;
    @Nullable private ItemStack playerHeadIcon;

    // 公共物品栏的拖拽分堆/双击状态
    private boolean isQuickCrafting = false;
    private int quickCraftingType = 0;
    private int quickCraftingButton = 0;
    private final Set<Slot> quickCraftSlots = new LinkedHashSet<>();
    @Nullable private Slot lastClickSlot;
    private long lastClickTime = 0L;
    private int lastClickButton = -1;
    private boolean doubleClick = false;
    private boolean skipNextRelease = false;

    private double lastMouseX = -10000;
    private double lastMouseY = -10000;

    public MultiWindowScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        ensureSharedInvPos();
    }

    private void ensureSharedInvPos() {
        if (this.width <= 0 || this.height <= 0) return;
        if (!this.invPosInitialized) {
            this.invPosInitialized = true;
            this.sharedInvCollapsed = MultiWindowClient.isSavedInventoryCollapsed();
            MultiWindowClient.SavedPos saved = MultiWindowClient.getSavedInventoryPos();
            if (saved != null) {
                this.invX = saved.x;
                this.invY = saved.y;
            } else {
                this.invX = (this.width - INV_W) / 2;
                this.invY = Math.max(TITLE_H + 2, this.height - INV_BODY_H - 4);
            }
        }
        clampSharedInvPos();
    }

    private void clampSharedInvPos() {
        int bodyH = this.sharedInvCollapsed ? 0 : INV_BODY_H;
        int minX = 2;
        int maxX = Math.max(minX, this.width - INV_W - 2);
        int minY = TITLE_H + 2;
        int maxY = Math.max(minY, this.height - bodyH - 2);
        this.invX = Math.clamp(this.invX, minX, maxX);
        this.invY = Math.clamp(this.invY, minY, maxY);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        clampSharedInvPos();
        for (WindowEntry win : List.copyOf(MultiWindowClient.getWindows())) {
            ClientRemoteBlockCache.pushContext(win.containerId());
            try {
                win.screen.resize(mc, width, height);
                win.recomputeCrop();
                win.recomputeExtents();
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
    }

    @Override
    public void tick() {
        for (WindowEntry win : List.copyOf(MultiWindowClient.getWindows())) {
            ClientRemoteBlockCache.pushContext(win.containerId());
            try {
                win.screen.tick();
                win.recomputeCrop();
                win.recomputeExtents();
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
    }

    private ItemStack getPlayerHeadIcon() {
        if (this.playerHeadIcon == null) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            if (this.minecraft != null && this.minecraft.player != null) {
                head.set(DataComponents.PROFILE, new ResolvableProfile(this.minecraft.player.getGameProfile()));
            }
            this.playerHeadIcon = head;
        }
        return this.playerHeadIcon;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        ensureSharedInvPos();

        this.renderTransparentBackground(gg);
        gg.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (windows.isEmpty()) return;

        WindowEntry top = windows.get(windows.size() - 1);
        WindowEntry hoveredWindow = null;
        boolean hoveredShared = false;

        if (this.sharedInvInFront && isOverSharedInv(mouseX, mouseY)) {
            hoveredShared = true;
        } else {
            hoveredWindow = findHitWindow(mouseX, mouseY);
            if (hoveredWindow == null && isOverSharedInv(mouseX, mouseY)) {
                hoveredShared = true;
            }
        }

        List<Runnable> deferredTooltips = new ArrayList<>();

        if (!this.sharedInvInFront) {
            renderSharedInventory(gg, mouseX, mouseY, hoveredShared, deferredTooltips);
        }

        for (WindowEntry win : List.copyOf(windows)) {
            renderChildWindow(gg, win, win == top, win == hoveredWindow, mouseX, mouseY, partialTick, deferredTooltips);
        }

        if (this.sharedInvInFront) {
            renderSharedInventory(gg, mouseX, mouseY, hoveredShared, deferredTooltips);
        }

        // 先画鼠标抓着的物品（z = 300），再画延迟出来的 Tooltip（z = 400）
        if (this.minecraft != null && this.minecraft.player != null) {
            ItemStack carried = top.screen.getMenu().getCarried();
            if (!carried.isEmpty()) {
                gg.pose().pushPose();
                gg.pose().translate(0.0F, 0.0F, 300.0F);
                gg.renderItem(carried, mouseX - 8, mouseY - 8);
                gg.renderItemDecorations(this.font, carried, mouseX - 8, mouseY - 8);
                gg.pose().popPose();
                gg.flush();
            }
        }

        for (Runnable tooltipTask : deferredTooltips) {
            tooltipTask.run();
        }
        gg.flush();
    }

    private void renderChildWindow(GuiGraphics gg, WindowEntry win, boolean isTop, boolean isHovered,
                                   int mouseX, int mouseY, float partialTick, List<Runnable> deferredTooltips) {
        int x = win.x;
        int y = win.y;
        int w = win.w();
        int barY = win.outerY();

        // 1. 标题栏
        int barColor = isTop ? 0xE0252A36 : 0xD0191C22;
        int borderColor = isTop ? 0xFF6B8AFF : 0xFF444A58;
        gg.fill(x - 1, barY - TITLE_H - 1, x + w + 1, barY, borderColor);
        gg.fill(x, barY - TITLE_H, x + w, barY, barColor);

        ItemStack icon = !win.data.iconStack().isEmpty()
                ? win.data.iconStack()
                : new ItemStack(Items.ENDER_EYE);
        gg.pose().pushPose();
        gg.pose().translate(x + 2, barY - TITLE_H + 1, 0);
        gg.pose().scale(0.85F, 0.85F, 1.0F);
        gg.renderItem(icon, 0, 0);
        gg.pose().popPose();

        String title = win.screen.getTitle().getString();
        gg.drawString(this.font, this.font.plainSubstrByWidth(title, Math.max(20, w - 36)),
                x + 18, barY - TITLE_H + 4, isTop ? 0xFFFFFF : 0xAAAAAA, false);

        int closeX = x + w - CLOSE_SIZE - 2;
        int closeY = barY - TITLE_H + 2;
        boolean hoverClose = isHovered && mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
        gg.fill(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE, hoverClose ? 0xFFE04848 : 0xFF883030);
        gg.drawString(this.font, "x", closeX + 3, closeY + 2, 0xFFFFFF, false);

        gg.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();

        // 2. 子窗口本体（临时抑制 EMI/JEI 叠加层，并通过 Scissor 裁掉底部自带物品栏）
        boolean passMouse = isHovered && mouseY >= barY;
        int dx = win.dx();
        int dy = win.dy();
        int childMx = passMouse ? (mouseX - dx) : -10000;
        int childMy = passMouse ? (mouseY - dy) : -10000;

        ItemStack savedCarried = win.screen.getMenu().getCarried();
        if (!savedCarried.isEmpty()) {
            win.screen.getMenu().setCarried(ItemStack.EMPTY);
        }

        DeferredTooltipGuiGraphics subGg = new DeferredTooltipGuiGraphics(this.minecraft, gg, deferredTooltips, dx, dy);
        ClientRemoteBlockCache.pushContext(win.containerId());
        MultiWindowClient.setRenderingChild(true);
        int overlayMask = pushSuppressOverlays();
        try {
            subGg.drawManaged(() -> {
                boolean cropped = win.isCropped() && (y + win.cropY > win.outerY());
                if (cropped) {
                    subGg.enableScissor(win.outerX(), win.outerY(), win.outerX() + win.outerW(), y + win.cropY);
                }
                subGg.pose().pushPose();
                subGg.pose().translate(dx, dy, 0.0F);
                try {
                    win.screen.renderWithTooltip(subGg, childMx, childMy, partialTick);
                } finally {
                    subGg.pose().popPose();
                    if (cropped) {
                        subGg.disableScissor();
                    }
                }
            });
        } finally {
            popSuppressOverlays(overlayMask);
            RenderSystem.disableScissor();
            MultiWindowClient.setRenderingChild(false);
            ClientRemoteBlockCache.popContext();
            if (!savedCarried.isEmpty()) {
                win.screen.getMenu().setCarried(savedCarried);
            }
        }

        // 3. 若被裁剪，补画 4px 原版质感底边框
        if (win.isCropped()) {
            int fy = y + win.cropY;
            gg.fill(x, fy, x + 1, fy + 3, 0xFF000000);
            gg.fill(x + 1, fy, x + 3, fy + 1, 0xFFFFFFFF);
            gg.fill(x + 3, fy, x + w - 3, fy + 1, 0xFFC6C6C6);
            gg.fill(x + w - 3, fy, x + w - 1, fy + 1, 0xFF555555);
            gg.fill(x + w - 1, fy, x + w, fy + 3, 0xFF000000);
            gg.fill(x + 1, fy + 1, x + w - 1, fy + 3, 0xFF555555);
            gg.fill(x + 1, fy + 3, x + w - 1, fy + 4, 0xFF000000);
        }

        gg.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();
    }

    @Nullable
    private static Field findBooleanField(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            try {
                Field f = cur.getDeclaredField(name);
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    return f;
                }
                return null;
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private static int pushSuppressOverlays() {
        int mask = 0;

        if (!emiChecked) {
            emiChecked = true;
            try {
                Class<?> cls = Class.forName("dev.emi.emi.config.EmiConfig");
                emiEnabledField = findBooleanField(cls, "enabled");
            } catch (Throwable ignored) {}
        }
        if (emiEnabledField != null) {
            try {
                if (emiEnabledField.getBoolean(null)) {
                    emiEnabledField.setBoolean(null, false);
                    mask |= 1;
                }
            } catch (Throwable ignored) {}
        }

        if (jeiOverlayEnabledField == null && !jeiMissing) {
            try {
                Class<?> internalCls = Class.forName("mezz.jei.common.Internal");
                Method m = internalCls.getMethod("getClientToggleState");
                Object state = m.invoke(null);
                if (state != null) {
                    Field f = findBooleanField(state.getClass(), "overlayEnabled");
                    if (f != null) {
                        jeiGetToggleState = m;
                        jeiOverlayEnabledField = f;
                    } else {
                        jeiMissing = true;
                    }
                }
            } catch (Throwable t) {
                jeiMissing = true;
            }
        }
        if (jeiGetToggleState != null && jeiOverlayEnabledField != null) {
            try {
                Object state = jeiGetToggleState.invoke(null);
                if (state != null && jeiOverlayEnabledField.getBoolean(state)) {
                    jeiOverlayEnabledField.setBoolean(state, false);
                    mask |= 2;
                }
            } catch (Throwable ignored) {}
        }

        return mask;
    }

    private static void popSuppressOverlays(int mask) {
        if ((mask & 1) != 0 && emiEnabledField != null) {
            try {
                emiEnabledField.setBoolean(null, true);
            } catch (Throwable ignored) {}
        }
        if ((mask & 2) != 0 && jeiGetToggleState != null && jeiOverlayEnabledField != null) {
            try {
                Object state = jeiGetToggleState.invoke(null);
                if (state != null) {
                    jeiOverlayEnabledField.setBoolean(state, true);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void renderSharedInventory(GuiGraphics gg, int mouseX, int mouseY, boolean isHovered, List<Runnable> deferredTooltips) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        int x = this.invX;
        int y = this.invY;
        int w = INV_W;

        // 1. 标题栏
        int barColor = this.sharedInvInFront ? 0xE023302B : 0xD0191F1C;
        int borderColor = this.sharedInvInFront ? 0xFF5CE0A8 : 0xFF3E6354;
        gg.fill(x - 1, y - TITLE_H - 1, x + w + 1, y, borderColor);
        gg.fill(x, y - TITLE_H, x + w, y, barColor);

        gg.pose().pushPose();
        gg.pose().translate(x + 2, y - TITLE_H + 1, 0);
        gg.pose().scale(0.85F, 0.85F, 1.0F);
        gg.renderItem(getPlayerHeadIcon(), 0, 0);
        gg.pose().popPose();

        Component invTitle = this.minecraft.player.getInventory().getDisplayName();
        gg.drawString(this.font, invTitle, x + 18, y - TITLE_H + 4, 0xFFFFFF, false);

        int toggleX = x + w - CLOSE_SIZE - 2;
        int toggleY = y - TITLE_H + 2;
        boolean hoverToggle = isHovered && mouseX >= toggleX && mouseX <= toggleX + CLOSE_SIZE && mouseY >= toggleY && mouseY <= toggleY + CLOSE_SIZE;
        gg.fill(toggleX, toggleY, toggleX + CLOSE_SIZE, toggleY + CLOSE_SIZE, hoverToggle ? 0xFF4F7A68 : 0xFF355346);
        gg.drawString(this.font, this.sharedInvCollapsed ? "+" : "-", toggleX + 3, toggleY + 2, 0xFFFFFF, false);

        gg.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();

        if (this.sharedInvCollapsed) {
            return;
        }

        // 2. 物品栏贴图主体（直接复用原版 generic_54.png）
        gg.blit(CONTAINER_TEXTURE, x, y, 0, 0, INV_W, 4);
        gg.blit(CONTAINER_TEXTURE, x, y + 4, 0, 135, INV_W, 87);

        WindowEntry targetWin = getSharedTargetWindow();
        AbstractContainerMenu menu = targetWin != null ? targetWin.screen.getMenu() : null;
        Inventory playerInv = this.minecraft.player.getInventory();
        ItemStack carried = menu != null ? menu.getCarried() : ItemStack.EMPTY;

        Slot hoveredSlot = null;
        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int invIndex = 0; invIndex < 36; invIndex++) {
            int sx = slotScreenX(invIndex);
            int sy = slotScreenY(invIndex);

            Slot slot = menu != null ? getSharedSlot(menu, invIndex) : null;
            ItemStack stack = slot != null ? slot.getItem() : playerInv.getItem(invIndex);
            String countOverride = null;

            if (slot != null && this.isQuickCrafting && this.quickCraftSlots.size() > 1 && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
                int addCount = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, carried);
                int maxStack = slot.getMaxStackSize(carried);
                ItemStack preview = carried.copyWithCount(addCount + (stack.isEmpty() ? 0 : stack.getCount()));
                if (preview.getCount() > maxStack) {
                    preview.setCount(maxStack);
                    countOverride = ChatFormatting.YELLOW.toString() + maxStack;
                }
                stack = preview;
                gg.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            }

            if (!stack.isEmpty()) {
                gg.renderItem(stack, sx, sy);
                gg.renderItemDecorations(this.font, stack, sx, sy, countOverride);
            }

            if (isHovered && mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17) {
                hoveredSlot = slot;
                hoveredStack = stack;
                gg.pose().pushPose();
                gg.pose().translate(0.0F, 0.0F, 200.0F);
                gg.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                gg.pose().popPose();
            }
        }

        if (hoveredSlot != null && carried.isEmpty() && !hoveredStack.isEmpty()) {
            ItemStack tipStack = hoveredStack.copy();
            Minecraft mc = this.minecraft;
            int mx = mouseX;
            int my = mouseY;
            deferredTooltips.add(() -> gg.renderTooltip(
                    this.font,
                    Screen.getTooltipFromItem(mc, tipStack),
                    tipStack.getTooltipImage(),
                    tipStack,
                    mx,
                    my
            ));
        }

        gg.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();
    }

    private int slotScreenX(int invIndex) {
        int col = (invIndex < 9) ? invIndex : ((invIndex - 9) % 9);
        return this.invX + 8 + col * 18;
    }

    private int slotScreenY(int invIndex) {
        if (invIndex < 9) {
            return this.invY + 67;
        }
        int row = (invIndex - 9) / 9;
        return this.invY + 9 + row * 18;
    }

    private boolean isOverSharedInv(double mx, double my) {
        if (!this.invPosInitialized) return false;
        int bodyH = this.sharedInvCollapsed ? 0 : INV_BODY_H;
        return mx >= this.invX - 1 && mx <= this.invX + INV_W + 1
                && my >= this.invY - TITLE_H - 1 && my <= this.invY + bodyH;
    }

    private static boolean hitWindow(WindowEntry win, double mx, double my) {
        return mx >= win.outerX() && mx <= win.outerX() + win.outerW()
                && my >= win.outerY() - TITLE_H && my <= win.outerY() + win.outerH();
    }

    @Nullable
    private static WindowEntry findHitWindow(double mx, double my) {
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowEntry win = windows.get(i);
            if (hitWindow(win, mx, my)) {
                return win;
            }
        }
        return null;
    }

    @Nullable
    public WindowEntry getTopWindowAt(double mx, double my) {
        if (this.sharedInvInFront && isOverSharedInv(mx, my)) {
            return null;
        }
        return findHitWindow(mx, my);
    }

    @Nullable
    private WindowEntry getSharedTargetWindow() {
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (windows.isEmpty()) return null;
        WindowEntry top = windows.get(windows.size() - 1);
        if (hasAnyPlayerSlot(top.screen.getMenu())) {
            return top;
        }
        for (int i = windows.size() - 2; i >= 0; i--) {
            WindowEntry w = windows.get(i);
            if (hasAnyPlayerSlot(w.screen.getMenu())) {
                return w;
            }
        }
        return null;
    }

    private static boolean hasAnyPlayerSlot(AbstractContainerMenu menu) {
        for (Slot s : menu.slots) {
            if (s.isActive() && WindowEntry.isPlayerInvSlot(s)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Slot getSharedSlot(AbstractContainerMenu menu, int invIndex) {
        for (Slot s : menu.slots) {
            if (s.isActive() && WindowEntry.isPlayerInvSlot(s) && s.getContainerSlot() == invIndex) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    private Slot findSharedSlotAt(AbstractContainerMenu menu, double mx, double my) {
        if (this.sharedInvCollapsed) return null;
        for (int invIndex = 0; invIndex < 36; invIndex++) {
            int sx = slotScreenX(invIndex);
            int sy = slotScreenY(invIndex);
            if (mx >= sx - 1 && mx < sx + 17 && my >= sy - 1 && my < sy + 17) {
                return getSharedSlot(menu, invIndex);
            }
        }
        return null;
    }

    @Nullable
    public Slot getHoveredSharedSlot(double mx, double my) {
        if (this.sharedInvCollapsed || !isOverSharedInv(mx, my)) return null;
        if (!this.sharedInvInFront && findHitWindow(mx, my) != null) return null;
        WindowEntry target = getSharedTargetWindow();
        if (target == null) return null;
        return findSharedSlotAt(target.screen.getMenu(), mx, my);
    }

    @Nullable
    public AbstractContainerMenu getSharedInventoryMenu() {
        WindowEntry target = getSharedTargetWindow();
        return target != null ? target.screen.getMenu() : null;
    }

    public void bringToFront(WindowEntry win) {
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (windows.isEmpty() || windows.get(windows.size() - 1) == win) return;
        windows.remove(win);
        windows.add(win);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.containerMenu = win.screen.getMenu();
        }
        PacketDistributor.sendToServer(new MultiWindowPayloads.FocusWindowPayload(win.containerId()));
    }
    public void bringWindowToFront(WindowEntry win) {
        bringToFront(win);
    }

    public void focusSharedTargetWindow() {
        WindowEntry target = getSharedTargetWindow();
        if (target != null) {
            bringToFront(target);
        }
    }
    private void sendSlotClick(AbstractContainerMenu menu, int slotId, int mouseButton, ClickType type) {
        if (this.minecraft == null || this.minecraft.gameMode == null || this.minecraft.player == null) return;
        this.minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slotId, mouseButton, type, this.minecraft.player);
        MultiWindowClient.broadcastCarried(menu.getCarried());
    }

    private static boolean canAddQuickCraft(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        return !carried.isEmpty()
                && menu.canDragTo(slot)
                && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
                && (menu.getCarried().getCount() > 0)
                && slot.mayPlace(carried);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean overShared = isOverSharedInv(mouseX, mouseY);
        WindowEntry hitWin = findHitWindow(mouseX, mouseY);

        if (this.sharedInvInFront) {
            if (overShared) {
                return handleSharedInvClick(mouseX, mouseY, button);
            }
            if (hitWin != null) {
                this.sharedInvInFront = false;
                return handleWindowClick(hitWin, mouseX, mouseY, button);
            }
        } else {
            if (hitWin != null) {
                return handleWindowClick(hitWin, mouseX, mouseY, button);
            }
            if (overShared) {
                this.sharedInvInFront = true;
                return handleSharedInvClick(mouseX, mouseY, button);
            }
        }
        return false;
    }

    private boolean handleWindowClick(WindowEntry hit, double mouseX, double mouseY, int button) {
        bringToFront(hit);

        if (mouseY < hit.outerY()) {
            int closeX = hit.x + hit.w() - CLOSE_SIZE - 2;
            int closeY = hit.outerY() - TITLE_H + 2;
            if (button == 0 && mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE) {
                MultiWindowClient.closeOneFromUi(hit);
                return true;
            }
            if (button == 0) {
                this.draggingWindow = hit;
                this.dragOffsetX = mouseX - hit.x;
                this.dragOffsetY = mouseY - hit.y;
                return true;
            }
            return true;
        }

        ClientRemoteBlockCache.pushContext(hit.containerId());
        try {
            return hit.screen.mouseClicked(mouseX - hit.dx(), mouseY - hit.dy(), button);
        } finally {
            ClientRemoteBlockCache.popContext();
        }
    }

    private boolean handleSharedInvClick(double mouseX, double mouseY, int button) {
        this.sharedInvInFront = true;

        // 点击标题栏
        if (mouseY < this.invY) {
            int toggleX = this.invX + INV_W - CLOSE_SIZE - 2;
            int toggleY = this.invY - TITLE_H + 2;
            if (button == 0 && mouseX >= toggleX && mouseX <= toggleX + CLOSE_SIZE && mouseY >= toggleY && mouseY <= toggleY + CLOSE_SIZE) {
                this.sharedInvCollapsed = !this.sharedInvCollapsed;
                clampSharedInvPos();
                MultiWindowClient.saveInventoryState(this.invX, this.invY, this.sharedInvCollapsed);
                return true;
            }
            if (button == 0) {
                this.draggingSharedInv = true;
                this.dragInvOffsetX = mouseX - this.invX;
                this.dragInvOffsetY = mouseY - this.invY;
                return true;
            }
            return true;
        }

        WindowEntry targetWin = getSharedTargetWindow();
        if (targetWin == null || this.minecraft == null || this.minecraft.player == null) return true;
        bringToFront(targetWin);

        AbstractContainerMenu menu = targetWin.screen.getMenu();
        Slot slot = findSharedSlotAt(menu, mouseX, mouseY);
        ItemStack carried = menu.getCarried();

        long now = Util.getMillis();
        this.doubleClick = (slot != null && slot == this.lastClickSlot && now - this.lastClickTime < 250L
                && this.lastClickButton == button && !carried.isEmpty());
        this.skipNextRelease = false;

        if (button == 0 || button == 1) {
            if (slot != null) {
                if (hasShiftDown()) {
                    sendSlotClick(menu, slot.index, button, ClickType.QUICK_MOVE);
                    this.skipNextRelease = true;
                } else if (carried.isEmpty()) {
                    sendSlotClick(menu, slot.index, button, ClickType.PICKUP);
                    this.skipNextRelease = true;
                } else {
                    this.isQuickCrafting = true;
                    this.quickCraftingButton = button;
                    this.quickCraftSlots.clear();
                    this.quickCraftingType = (button == 0) ? 0 : 1;
                    if (canAddQuickCraft(menu, slot, carried)) {
                        this.quickCraftSlots.add(slot);
                    }
                }
            }
        } else if (this.minecraft.options.keyPickItem.matchesMouse(button)) {
            if (slot != null) {
                sendSlotClick(menu, slot.index, button, ClickType.CLONE);
            }
        }

        this.lastClickSlot = slot;
        this.lastClickTime = now;
        this.lastClickButton = button;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingSharedInv && button == 0) {
            this.invX = (int) Math.round(mouseX - this.dragInvOffsetX);
            this.invY = (int) Math.round(mouseY - this.dragInvOffsetY);
            clampSharedInvPos();
            return true;
        }
        if (this.isQuickCrafting) {
            WindowEntry targetWin = getSharedTargetWindow();
            if (targetWin != null) {
                AbstractContainerMenu menu = targetWin.screen.getMenu();
                Slot slot = findSharedSlotAt(menu, mouseX, mouseY);
                ItemStack carried = menu.getCarried();
                if (slot != null && canAddQuickCraft(menu, slot, carried)
                        && (this.quickCraftingType == 2 || carried.getCount() > this.quickCraftSlots.size())) {
                    this.quickCraftSlots.add(slot);
                }
            }
            return true;
        }
        if (this.draggingWindow != null && button == 0) {
            int minX = this.draggingWindow.extL + 2;
            int maxX = this.width - this.draggingWindow.w() - this.draggingWindow.extR - 2;
            int minY = this.draggingWindow.extT + TITLE_H + 2;
            int maxY = this.height - this.draggingWindow.h() - this.draggingWindow.extB - 2;
            this.draggingWindow.x = Math.max(minX, Math.min(Math.max(minX, maxX), (int) Math.round(mouseX - this.dragOffsetX)));
            this.draggingWindow.y = Math.max(minY, Math.min(Math.max(minY, maxY), (int) Math.round(mouseY - this.dragOffsetY)));
            return true;
        }
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (!windows.isEmpty()) {
            WindowEntry top = windows.get(windows.size() - 1);
            ClientRemoteBlockCache.pushContext(top.containerId());
            try {
                return top.screen.mouseDragged(mouseX - top.dx(), mouseY - top.dy(), button, dragX, dragY);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingSharedInv && button == 0) {
            MultiWindowClient.saveInventoryState(this.invX, this.invY, this.sharedInvCollapsed);
            this.draggingSharedInv = false;
            return true;
        }
        if (this.isQuickCrafting && button == this.quickCraftingButton) {
            this.isQuickCrafting = false;
            WindowEntry targetWin = getSharedTargetWindow();
            if (targetWin != null) {
                AbstractContainerMenu menu = targetWin.screen.getMenu();
                if (this.quickCraftSlots.size() > 1) {
                    sendSlotClick(menu, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);
                    for (Slot s : this.quickCraftSlots) {
                        sendSlotClick(menu, s.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
                    }
                    sendSlotClick(menu, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
                } else {
                    Slot slot = findSharedSlotAt(menu, mouseX, mouseY);
                    if (this.doubleClick && slot != null && menu.canTakeItemForPickAll(menu.getCarried(), slot)) {
                        sendSlotClick(menu, slot.index, button, ClickType.PICKUP_ALL);
                    } else if (slot != null && !this.skipNextRelease) {
                        sendSlotClick(menu, slot.index, button, ClickType.PICKUP);
                    }
                }
            }
            this.quickCraftSlots.clear();
            this.doubleClick = false;
            this.skipNextRelease = false;
            return true;
        }
        if (this.skipNextRelease) {
            this.skipNextRelease = false;
            return true;
        }
        if (this.draggingWindow != null && button == 0) {
            MultiWindowClient.savePosition(this.draggingWindow.data, this.draggingWindow.x, this.draggingWindow.y);
            this.draggingWindow = null;
            return true;
        }
        WindowEntry hit = findHitWindow(mouseX, mouseY);
        if (hit != null && mouseY >= hit.outerY()) {
            ClientRemoteBlockCache.pushContext(hit.containerId());
            try {
                return hit.screen.mouseReleased(mouseX - hit.dx(), mouseY - hit.dy(), button);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (!windows.isEmpty()) {
            WindowEntry top = windows.get(windows.size() - 1);
            ClientRemoteBlockCache.pushContext(top.containerId());
            try {
                return top.screen.mouseReleased(mouseX - top.dx(), mouseY - top.dy(), button);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        WindowEntry hit = getTopWindowAt(mouseX, mouseY);
        if (hit != null && mouseY >= hit.outerY()) {
            ClientRemoteBlockCache.pushContext(hit.containerId());
            try {
                return hit.screen.mouseScrolled(mouseX - hit.dx(), mouseY - hit.dy(), scrollX, scrollY);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MultiWindowClient.closeAllFromUi();
            return true;
        }
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (!windows.isEmpty()) {
            WindowEntry top = windows.get(windows.size() - 1);
            ClientRemoteBlockCache.pushContext(top.containerId());
            try {
                var focused = top.screen.getFocused();
                if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }

                // 悬停在公共物品栏槽位上时，支持 Q 丢弃 / F 换副手 / 1-9 快捷栏交换
                Slot sharedHovered = getHoveredSharedSlot(this.lastMouseX, this.lastMouseY);
                if (sharedHovered != null && this.minecraft != null) {
                    WindowEntry targetWin = getSharedTargetWindow();
                    if (targetWin != null) {
                        bringToFront(targetWin);
                        AbstractContainerMenu menu = targetWin.screen.getMenu();
                        if (this.minecraft.options.keyDrop.matches(keyCode, scanCode)) {
                            sendSlotClick(menu, sharedHovered.index, hasControlDown() ? 1 : 0, ClickType.THROW);
                            return true;
                        }
                        if (this.minecraft.options.keySwapOffhand.matches(keyCode, scanCode)) {
                            sendSlotClick(menu, sharedHovered.index, 40, ClickType.SWAP);
                            return true;
                        }
                        for (int i = 0; i < 9; i++) {
                            if (this.minecraft.options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
                                sendSlotClick(menu, sharedHovered.index, i, ClickType.SWAP);
                                return true;
                            }
                        }
                    }
                }

                if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                    MultiWindowClient.minimizeDesktop();
                    return true;
                }
                return top.screen.keyPressed(keyCode, scanCode, modifiers);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (!windows.isEmpty()) {
            WindowEntry top = windows.get(windows.size() - 1);
            ClientRemoteBlockCache.pushContext(top.containerId());
            try {
                return top.screen.keyReleased(keyCode, scanCode, modifiers);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        List<WindowEntry> windows = MultiWindowClient.getWindows();
        if (!windows.isEmpty()) {
            WindowEntry top = windows.get(windows.size() - 1);
            ClientRemoteBlockCache.pushContext(top.containerId());
            try {
                return top.screen.charTyped(codePoint, modifiers);
            } finally {
                ClientRemoteBlockCache.popContext();
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    /**
     * 拦截子窗口内调用的 Tooltip 绘制，延迟到 Scissor 外部和全窗口最顶层再绘制，防止被裁剪或被其他窗口遮挡。
     */
    private static final class DeferredTooltipGuiGraphics extends GuiGraphics {
        private final GuiGraphics realTarget;
        private final List<Runnable> deferred;
        private final int offsetX;
        private final int offsetY;

        DeferredTooltipGuiGraphics(Minecraft mc, GuiGraphics realTarget, List<Runnable> deferred, int offsetX, int offsetY) {
            super(mc, realTarget.bufferSource());
            this.realTarget = realTarget;
            this.deferred = deferred;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public void renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY) {
            ItemStack copy = stack.copy();
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, copy, sx, sy));
        }

        @Override
        public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
            List<Component> linesCopy = textComponents != null ? List.copyOf(textComponents) : List.of();
            ItemStack stackCopy = stack != null ? stack.copy() : ItemStack.EMPTY;
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, linesCopy, tooltipComponent, stackCopy, sx, sy));
        }

        @Override
        public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, int mouseX, int mouseY) {
            List<Component> linesCopy = textComponents != null ? List.copyOf(textComponents) : List.of();
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, linesCopy, tooltipComponent, sx, sy));
        }

        @Override
        public void renderTooltip(Font font, Component text, int mouseX, int mouseY) {
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, text, sx, sy));
        }

        @Override
        public void renderTooltip(Font font, List<? extends FormattedCharSequence> tooltipLines, int mouseX, int mouseY) {
            List<FormattedCharSequence> linesCopy = tooltipLines != null ? List.copyOf(tooltipLines) : List.of();
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, linesCopy, sx, sy));
        }

        @Override
        public void renderTooltip(Font font, List<FormattedCharSequence> tooltipLines, ClientTooltipPositioner tooltipPositioner, int mouseX, int mouseY) {
            List<FormattedCharSequence> linesCopy = tooltipLines != null ? List.copyOf(tooltipLines) : List.of();
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderTooltip(font, linesCopy, tooltipPositioner, sx, sy));
        }

        @Override
        public void renderComponentTooltip(Font font, List<Component> tooltips, int mouseX, int mouseY) {
            List<Component> linesCopy = tooltips != null ? List.copyOf(tooltips) : List.of();
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderComponentTooltip(font, linesCopy, sx, sy));
        }

        @Override
        public void renderComponentTooltip(Font font, List<? extends FormattedText> tooltips, int mouseX, int mouseY, ItemStack stack) {
            List<FormattedText> linesCopy = tooltips != null ? List.copyOf(tooltips) : List.of();
            ItemStack stackCopy = stack != null ? stack.copy() : ItemStack.EMPTY;
            int sx = mouseX + this.offsetX;
            int sy = mouseY + this.offsetY;
            this.deferred.add(() -> this.realTarget.renderComponentTooltip(font, linesCopy, sx, sy, stackCopy));
        }
    }
}