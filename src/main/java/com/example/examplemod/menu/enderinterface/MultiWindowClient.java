package com.example.examplemod.menu.enderinterface;

import com.example.examplemod.mixin.vanilla.AbstractContainerScreenAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiWindowClient {
    private static final Map<Class<?>, Method[]> BOUND_METHODS_CACHE = new ConcurrentHashMap<>();
    private static final Method[] NO_METHODS = new Method[0];
    private static int pendingReplaceOldId = -1;

    private static final String INV_POS_KEY = "__player_inventory__";
    private static final String INV_COLLAPSED_KEY = "__player_inventory_collapsed__";

    public static final class WindowEntry {
        public static final int CROPPED_BOTTOM_BORDER = 4;

        public AbstractContainerScreen<?> screen;
        public final EnderInterface.RemoteData data;
        public int x;
        public int y;
        public int extL, extR, extT, extB;
        public int cropY = -1;

        public WindowEntry(AbstractContainerScreen<?> screen, EnderInterface.RemoteData data, int x, int y) {
            this.screen = screen;
            this.data = data;
            this.x = x;
            this.y = y;
        }

        public AbstractContainerScreenAccessor acc() {
            return (AbstractContainerScreenAccessor) this.screen;
        }

        public int w() { return acc().enderinterface$getImageWidth(); }
        public int natH() { return acc().enderinterface$getImageHeight(); }
        public int h() { return isCropped() ? (this.cropY + CROPPED_BOTTOM_BORDER) : natH(); }
        public boolean isCropped() { return this.cropY > 0; }

        public int dx() { return this.x - acc().enderinterface$getLeftPos(); }
        public int dy() { return this.y - acc().enderinterface$getTopPos(); }
        public int containerId() { return this.screen.getMenu().containerId; }

        public int outerX() { return this.x - this.extL; }
        public int outerY() { return this.y - this.extT; }
        public int outerW() { return this.w() + this.extL + this.extR; }
        public int outerH() { return this.h() + this.extT + this.extB; }

        public static boolean isPlayerInvSlot(Slot slot) {
            if (Minecraft.getInstance().player == null) return false;
            if (!(slot.container instanceof Inventory)) return false;
            int idx = slot.getContainerSlot();
            return idx >= 0 && idx < 36;
        }

        public void recomputeCrop() {
            int natW = w();
            int natH = natH();
            AbstractContainerMenu menu = this.screen.getMenu();
            if (menu == null) {
                this.cropY = -1;
                return;
            }

            int invSlotCount = 0;
            int minInvY = Integer.MAX_VALUE;
            int maxNonPlayerBottom = 0;

            for (Slot slot : menu.slots) {
                if (!slot.isActive()) continue;
                if (isPlayerInvSlot(slot)) {
                    invSlotCount++;
                    if (slot.y < minInvY) {
                        minInvY = slot.y;
                    }
                } else {
                    int bottom = slot.y + 17;
                    if (bottom > maxNonPlayerBottom) {
                        maxNonPlayerBottom = bottom;
                    }
                }
            }

            // 至少具备玩家物品栏（27+9）才做裁剪
            if (invSlotCount < 27 || minInvY == Integer.MAX_VALUE) {
                this.cropY = -1;
                return;
            }

            // 若有容器槽位或侧边栏延伸到玩家物品栏区域，不裁剪，防止截断功能槽位
            if (maxNonPlayerBottom > minInvY - 3) {
                this.cropY = -1;
                return;
            }

            // 若有容器功能控件（如村民交易列表、展开的升级/合成侧栏）延伸到下方，不裁剪；
            // 忽略主面板内部“容器底部以下”的玩家物品栏整理/转移按钮（如精妙存储 [?][↓]）
            int leftPos = acc().enderinterface$getLeftPos();
            int topPos = acc().enderinterface$getTopPos();
            int limit = Math.max(96, Math.max(natW, natH));
            int[] maxAboveInvWidgetBottom = new int[]{0};
            if (hasWidgetBelow(this.screen.children(), leftPos, topPos, natW, natH, limit,
                    maxNonPlayerBottom, minInvY, maxAboveInvWidgetBottom, 0)) {
                this.cropY = -1;
                return;
            }

            int candidate = Math.max(maxNonPlayerBottom + 1, minInvY - 13);
            if (maxAboveInvWidgetBottom[0] > 0) {
                candidate = Math.max(candidate, Math.min(minInvY - 4, maxAboveInvWidgetBottom[0] + 1));
            }
            if (candidate <= 16 || candidate + CROPPED_BOTTOM_BORDER >= natH - 12) {
                this.cropY = -1;
                return;
            }
            this.cropY = candidate;
        }

        private static boolean hasChildren(ContainerEventHandler ceh) {
            try {
                List<? extends GuiEventListener> children = ceh.children();
                return children != null && !children.isEmpty();
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static boolean hasWidgetBelow(List<? extends GuiEventListener> list, int leftPos, int topPos,
                                              int natW, int natH, int limit, int maxNonPlayerBottom,
                                              int minInvY, int[] maxAboveInvWidgetBottom, int depth) {
            if (list == null || depth > 5) return false;
            for (GuiEventListener l : list) {
                int[] b = readWidgetBounds(l);
                if (b != null && b[2] > 0 && b[3] > 0) {
                    int rx = b[0] - leftPos;
                    int ry = b[1] - topPos;
                    int rw = b[2];
                    int rh = b[3];
                    if (rx + rw >= -limit && rx <= natW + limit && ry >= -limit && ry <= minInvY + limit) {
                        boolean insideMainPanel = rx >= -2 && rx + rw <= natW + 2;
                        boolean isPlayerBandWidget = insideMainPanel && maxNonPlayerBottom > 0 && ry >= maxNonPlayerBottom - 1;
                        boolean isFullPanelWrapper = (l instanceof ContainerEventHandler ceh && !(l instanceof Screen))
                                && hasChildren(ceh) && insideMainPanel && ry <= 2 && ry + rh >= natH - 4;

                        if (!isPlayerBandWidget && !isFullPanelWrapper) {
                            int bottom = ry + rh;
                            if (bottom > minInvY - 1) {
                                return true;
                            }
                            if (bottom > maxAboveInvWidgetBottom[0]) {
                                maxAboveInvWidgetBottom[0] = bottom;
                            }
                        }
                    }
                }
                if (l instanceof ContainerEventHandler ceh && !(l instanceof Screen)) {
                    try {
                        if (hasWidgetBelow(ceh.children(), leftPos, topPos, natW, natH, limit,
                                maxNonPlayerBottom, minInvY, maxAboveInvWidgetBottom, depth + 1)) {
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            return false;
        }

        public void recomputeExtents() {
            int natW = w();
            int natH = natH();
            int effH = h();
            int leftPos = acc().enderinterface$getLeftPos();
            int topPos = acc().enderinterface$getTopPos();
            int limit = Math.max(96, Math.max(natW, natH));
            int[] box = new int[]{0, 0, 0, 0};

            // 1. 扫描所有非隐藏的 Slot（裁剪模式下跳过被隐藏的玩家物品栏槽位）
            AbstractContainerMenu menu = this.screen.getMenu();
            if (menu != null) {
                int pad = 7;
                for (Slot slot : menu.slots) {
                    if (!slot.isActive()) continue;
                    if (isCropped() && isPlayerInvSlot(slot)) continue;
                    int rx = slot.x - pad;
                    int ry = slot.y - pad;
                    int rw = 18 + pad * 2;
                    int rh = 18 + pad * 2;
                    accumulate(box, rx, ry, rw, rh, natW, effH, limit);
                }
            }

            // 2. 扫描所有子控件（覆盖侧边升级栏/设置标签页；裁剪模式下忽略被裁掉的玩家栏区按钮）
            walkListeners(this.screen.children(), leftPos, topPos, natW, natH, effH,
                    limit, isCropped(), this.cropY, box, 0);

            this.extL = box[0];
            this.extR = box[1];
            this.extT = box[2];
            this.extB = box[3];
        }

        private static void walkListeners(List<? extends GuiEventListener> list, int leftPos, int topPos,
                                          int natW, int natH, int effH, int limit,
                                          boolean cropped, int cropY, int[] box, int depth) {
            if (list == null || depth > 5) return;
            for (GuiEventListener l : list) {
                int[] b = readWidgetBounds(l);
                if (b != null && b[2] > 0 && b[3] > 0) {
                    int rx = b[0] - leftPos;
                    int ry = b[1] - topPos;
                    int rw = b[2];
                    int rh = b[3];
                    boolean insideMainPanel = rx >= -2 && rx + rw <= natW + 2;
                    boolean isCroppedBottomWidget = cropped && insideMainPanel && ry >= cropY - 2;
                    boolean isFullPanelWrapper = (l instanceof ContainerEventHandler ceh && !(l instanceof Screen))
                            && hasChildren(ceh) && insideMainPanel && ry <= 2 && ry + rh >= natH - 4;
                    if (!isCroppedBottomWidget && !isFullPanelWrapper) {
                        accumulate(box, rx, ry, rw, rh, natW, effH, limit);
                    }
                }
                if (l instanceof ContainerEventHandler ceh && !(l instanceof Screen)) {
                    try {
                        walkListeners(ceh.children(), leftPos, topPos, natW, natH, effH,
                                limit, cropped, cropY, box, depth + 1);
                    } catch (Throwable ignored) {}
                }
            }
        }

        private static void accumulate(int[] box, int rx, int ry, int rw, int rh, int natW, int effH, int limit) {
            if (rx + rw < -limit || rx > natW + limit || ry + rh < -limit || ry > effH + limit) {
                return;
            }
            if (rx < 0) box[0] = Math.max(box[0], Math.min(limit, -rx));
            if (rx + rw > natW) box[1] = Math.max(box[1], Math.min(limit, rx + rw - natW));
            if (ry < 0) box[2] = Math.max(box[2], Math.min(limit, -ry));
            if (ry + rh > effH) box[3] = Math.max(box[3], Math.min(limit, ry + rh - effH));
        }

        private static int[] readWidgetBounds(GuiEventListener l) {
            if (l instanceof AbstractWidget aw) {
                if (!aw.visible) return null;
                return new int[]{aw.getX(), aw.getY(), aw.getWidth(), aw.getHeight()};
            }
            Method[] ms = BOUND_METHODS_CACHE.computeIfAbsent(l.getClass(), cls -> {
                String[] names = {"getX", "getY", "getWidth", "getHeight"};
                Method[] out = new Method[4];
                for (int i = 0; i < 4; i++) {
                    try {
                        Method m = cls.getMethod(names[i]);
                        if (m.getParameterCount() != 0 || (m.getReturnType() != int.class && m.getReturnType() != Integer.class)) {
                            return NO_METHODS;
                        }
                        m.setAccessible(true);
                        out[i] = m;
                    } catch (NoSuchMethodException e) {
                        return NO_METHODS;
                    }
                }
                return out;
            });
            if (ms == NO_METHODS) return null;
            try {
                return new int[]{
                        ((Number) ms[0].invoke(l)).intValue(),
                        ((Number) ms[1].invoke(l)).intValue(),
                        ((Number) ms[2].invoke(l)).intValue(),
                        ((Number) ms[3].invoke(l)).intValue()
                };
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static final class SavedPos {
        public int x;
        public int y;
        public SavedPos() {}
        public SavedPos(int x, int y) { this.x = x; this.y = y; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path POS_FILE = FMLPaths.CONFIGDIR.get().resolve("enderinterface_windows.json");
    private static final Map<String, SavedPos> SAVED_POSITIONS = new HashMap<>();
    private static boolean positionsLoaded = false;

    private static String posKey(EnderInterface.RemoteData data) {
        if (data.entityUuid().isPresent()) {
            return "entity|" + data.entityUuid().get();
        }
        return data.dim().location() + "|" + data.pos().getX() + "," + data.pos().getY() + "," + data.pos().getZ();
    }

    private static void ensureLoaded() {
        if (positionsLoaded) return;
        positionsLoaded = true;
        try {
            if (Files.exists(POS_FILE)) {
                String json = Files.readString(POS_FILE);
                Type type = new TypeToken<Map<String, SavedPos>>() {}.getType();
                Map<String, SavedPos> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    SAVED_POSITIONS.putAll(loaded);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void flushPositions() {
        try {
            Files.createDirectories(POS_FILE.getParent());
            Files.writeString(POS_FILE, GSON.toJson(SAVED_POSITIONS));
        } catch (Throwable ignored) {}
    }

    public static void savePosition(EnderInterface.RemoteData data, int x, int y) {
        ensureLoaded();
        SAVED_POSITIONS.put(posKey(data), new SavedPos(x, y));
        flushPositions();
    }

    @Nullable
    public static SavedPos getSavedInventoryPos() {
        ensureLoaded();
        return SAVED_POSITIONS.get(INV_POS_KEY);
    }

    public static boolean isSavedInventoryCollapsed() {
        ensureLoaded();
        SavedPos p = SAVED_POSITIONS.get(INV_COLLAPSED_KEY);
        return p != null && p.x == 1;
    }

    public static void saveInventoryState(int x, int y, boolean collapsed) {
        ensureLoaded();
        SAVED_POSITIONS.put(INV_POS_KEY, new SavedPos(x, y));
        SAVED_POSITIONS.put(INV_COLLAPSED_KEY, new SavedPos(collapsed ? 1 : 0, 0));
        flushPositions();
    }

    private static final List<WindowEntry> WINDOWS = new ArrayList<>();
    @Nullable private static EnderInterface.RemoteData pendingRemote;
    @Nullable private static MultiWindowScreen desktop;
    private static boolean renderingChild;

    private MultiWindowClient() {}

    public static void setPendingRemote(EnderInterface.RemoteData data, @Nullable RemoteBlockSnapshot snapshot) {
        pendingRemote = data;
        ClientRemoteBlockCache.setPending(snapshot);
    }

    public static void setPendingReplace(int oldId, EnderInterface.RemoteData data, @Nullable RemoteBlockSnapshot snapshot) {
        pendingReplaceOldId = oldId;
        pendingRemote = data;
        if (snapshot != null) {
            ClientRemoteBlockCache.setPending(snapshot);
        }
    }

    public static void clearPendingRemote() {
        pendingReplaceOldId = -1;
        pendingRemote = null;
        ClientRemoteBlockCache.clearPending();
    }

    public static boolean isRenderingChild() {
        return renderingChild;
    }

    public static void setRenderingChild(boolean value) {
        renderingChild = value;
    }

    public static List<WindowEntry> getWindows() {
        return WINDOWS;
    }

    public static boolean interceptOpen(AbstractContainerScreen<?> incoming) {
        if (pendingRemote == null) {
            return false;
        }
        EnderInterface.RemoteData data = pendingRemote;
        int replaceId = pendingReplaceOldId;
        if (replaceId == -1) {
            L2MenuStackerCompat.Client.clearTrackerIfWid(-1);
        }
        L2MenuStackerCompat.Client.resetMouseCache();
        pendingRemote = null;
        pendingReplaceOldId = -1;

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (replaceId != -1) {
            for (WindowEntry entry : WINDOWS) {
                if (entry.containerId() == replaceId) {
                    incoming.init(mc, sw, sh);
                    ClientRemoteBlockCache.pushContext(replaceId);
                    try { entry.screen.removed(); } catch (Throwable ignored) {} finally { ClientRemoteBlockCache.popContext(); }
                    ClientRemoteBlockCache.bindToWindow(incoming.getMenu().containerId, replaceId);
                    entry.screen = incoming;
                    entry.recomputeCrop();
                    entry.recomputeExtents();
                    int minX = entry.extL + 2;
                    int maxX = Math.max(minX, sw - entry.w() - entry.extR - 2);
                    int minY = entry.extT + 18;
                    int maxY = Math.max(minY, sh - entry.h() - entry.extB - 2);
                    entry.x = Math.clamp(entry.x, minX, maxX);
                    entry.y = Math.clamp(entry.y, minY, maxY);
                    ensureDesktopOpen(mc, entry);
                    return true;
                }
            }
        }

        incoming.init(mc, sw, sh);
        ClientRemoteBlockCache.bindToWindow(incoming.getMenu().containerId, -1);

        WindowEntry entry = new WindowEntry(incoming, data, 0, 0);
        entry.recomputeCrop();
        entry.recomputeExtents();

        int w = entry.w();
        int h = entry.h();
        // 默认布局时给底部公共物品栏预留空间（91px + 16px 标题 + 8px 间距）
        int availH = Math.max(h + 24, sh - 115);

        ensureLoaded();
        SavedPos saved = SAVED_POSITIONS.get(posKey(data));
        int x;
        int y;
        if (saved != null) {
            x = Math.clamp(saved.x, 2, Math.max(2, sw - w - 2));
            y = Math.clamp(saved.y, 18, Math.max(18, sh - h - 2));
        } else if (WINDOWS.isEmpty()) {
            x = (sw - w) / 2;
            y = Math.max(18 + entry.extT, (availH - h) / 2);
        } else if (WINDOWS.size() == 1 && !SAVED_POSITIONS.containsKey(posKey(WINDOWS.get(0).data))) {
            WindowEntry first = WINDOWS.get(0);
            int totalW = first.w() + 12 + w;
            if (totalW <= sw - 16) {
                first.x = (sw - totalW) / 2;
                x = first.x + first.w() + 12;
                y = Math.max(18 + entry.extT, (availH - h) / 2);
            } else {
                x = Math.clamp(first.x + 28, 8, Math.max(8, sw - w - 8));
                y = Math.clamp(first.y + 20, 18, Math.max(18, sh - h - 8));
            }
        } else {
            WindowEntry last = WINDOWS.get(WINDOWS.size() - 1);
            x = Math.clamp(last.x + 28, 8, Math.max(8, sw - w - 8));
            y = Math.clamp(last.y + 20, 18, Math.max(18, sh - h - 8));
        }

        int minX = entry.extL + 2;
        int maxX = Math.max(minX, sw - w - entry.extR - 2);
        int minY = entry.extT + 18;
        int maxY = Math.max(minY, sh - h - entry.extB - 2);
        entry.x = Math.clamp(x, minX, maxX);
        entry.y = Math.clamp(y, minY, maxY);

        WINDOWS.add(entry);
        ensureDesktopOpen(mc, entry);
        return true;
    }

    private static void ensureDesktopOpen(Minecraft mc, @Nullable WindowEntry focus) {
        if (desktop == null) {
            desktop = new MultiWindowScreen();
        }
        if (focus != null && mc.player != null) {
            WINDOWS.remove(focus);
            WINDOWS.add(focus);
            mc.player.containerMenu = focus.screen.getMenu();
        }
        if (mc.screen != desktop) {
            mc.setScreen(desktop);
        }
    }

    public static void restoreAndFocus(int containerId) {
        Minecraft mc = Minecraft.getInstance();
        for (WindowEntry entry : WINDOWS) {
            if (entry.containerId() == containerId) {
                ensureDesktopOpen(mc, entry);
                return;
            }
        }
    }

    @Nullable
    public static AbstractContainerMenu findMenu(int containerId) {
        for (WindowEntry entry : WINDOWS) {
            AbstractContainerMenu m = entry.screen.getMenu();
            if (m.containerId == containerId) {
                return m;
            }
        }
        return null;
    }

    public static void broadcastCarried(ItemStack stack) {
        for (WindowEntry entry : WINDOWS) {
            entry.screen.getMenu().setCarried(stack.copy());
        }
    }

    public static void closeOneFromUi(WindowEntry entry) {
        int id = entry.containerId();
        WINDOWS.remove(entry);
        try {
            entry.screen.removed();
        } catch (Throwable ignored) {}
        PacketDistributor.sendToServer(new MultiWindowPayloads.CloseWindowPayload(id));
        ClientRemoteBlockCache.removeWindow(id);
        L2MenuStackerCompat.Client.clearTrackerIfWid(WINDOWS.isEmpty() ? -1 : id);
        Minecraft mc = Minecraft.getInstance();
        if (WINDOWS.isEmpty()) {
            if (mc.player != null) {
                mc.player.containerMenu = mc.player.inventoryMenu;
            }
            if (mc.screen == desktop) {
                mc.setScreen(null);
            }
            desktop = null;
        } else if (mc.player != null) {
            WindowEntry top = WINDOWS.get(WINDOWS.size() - 1);
            mc.player.containerMenu = top.screen.getMenu();
            PacketDistributor.sendToServer(new MultiWindowPayloads.FocusWindowPayload(top.containerId()));
        }
    }

    public static void minimizeDesktop() {
        L2MenuStackerCompat.Client.clearTrackerIfWid(-1);
        L2MenuStackerCompat.Client.resetMouseCache();
        Minecraft mc = Minecraft.getInstance();
        PacketDistributor.sendToServer(new MultiWindowPayloads.MinimizeDesktopPayload());
        if (mc.player != null) {
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        if (mc.screen == desktop) {
            mc.setScreen(null);
        }
    }

    public static void closeAllFromUi() {
        L2MenuStackerCompat.Client.clearTrackerIfWid(-1);
        L2MenuStackerCompat.Client.resetMouseCache();
        for (WindowEntry entry : WINDOWS) {
            try {
                entry.screen.removed();
            } catch (Throwable ignored) {}
        }
        ClientRemoteBlockCache.clearAll();
        WINDOWS.clear();
        PacketDistributor.sendToServer(new MultiWindowPayloads.CloseAllWindowsPayload());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        if (mc.screen == desktop) {
            mc.setScreen(null);
        }
        desktop = null;
    }

    public static void handleServerClose(int containerId) {
        Iterator<WindowEntry> it = WINDOWS.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            WindowEntry entry = it.next();
            if (entry.containerId() == containerId) {
                it.remove();
                try {
                    entry.screen.removed();
                } catch (Throwable ignored) {}
                removed = true;
                break;
            }
        }
        if (!removed) return;
        ClientRemoteBlockCache.removeWindow(containerId);
        L2MenuStackerCompat.Client.clearTrackerIfWid(WINDOWS.isEmpty() ? -1 : containerId);
        Minecraft mc = Minecraft.getInstance();
        if (WINDOWS.isEmpty()) {
            if (mc.player != null) {
                mc.player.containerMenu = mc.player.inventoryMenu;
            }
            if (mc.screen == desktop) {
                mc.setScreen(null);
            }
            desktop = null;
        } else if (mc.player != null && mc.player.containerMenu.containerId == containerId) {
            WindowEntry top = WINDOWS.get(WINDOWS.size() - 1);
            mc.player.containerMenu = top.screen.getMenu();
            PacketDistributor.sendToServer(new MultiWindowPayloads.FocusWindowPayload(top.containerId()));
        }
    }
    public static boolean isDesktopShowing() {
        return desktop != null && Minecraft.getInstance().screen == desktop;
    }

    public static void onDesktopPreempted() {
        L2MenuStackerCompat.Client.clearTrackerIfWid(-1);
        L2MenuStackerCompat.Client.resetMouseCache();
        PacketDistributor.sendToServer(new MultiWindowPayloads.MinimizeDesktopPayload());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
    }
    public static void clearOnDisconnect() {
        pendingReplaceOldId = -1;
        WINDOWS.clear();
        pendingRemote = null;
        desktop = null;
        ClientRemoteBlockCache.clearAll();
        L2MenuStackerCompat.Client.resetMouseCache();
    }
}