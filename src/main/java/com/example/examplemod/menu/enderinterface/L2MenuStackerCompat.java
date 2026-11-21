package com.example.examplemod.menu.enderinterface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Stack;

public final class L2MenuStackerCompat {
    private static Boolean loaded;
    private static boolean trackerInitAttempted = false;
    @Nullable private static Method trackerGetMethod;
    @Nullable private static Field stackField;
    @Nullable private static Field widField;
    @Nullable private static Field tempField;
    @Nullable private static Field currentField;
    @Nullable private static Field waitingField;

    private L2MenuStackerCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("l2menustacker");
        }
        return loaded;
    }

    private static boolean ensureTrackerReflection() {
        if (!isLoaded()) return false;
        if (trackerInitAttempted) return trackerGetMethod != null;
        trackerInitAttempted = true;
        try {
            Class<?> cls = Class.forName("dev.xkmc.l2menustacker.screen.base.ScreenTracker");
            trackerGetMethod = cls.getMethod("get", Player.class);

            stackField = cls.getDeclaredField("stack");
            stackField.setAccessible(true);

            widField = cls.getDeclaredField("wid");
            widField.setAccessible(true);

            tempField = cls.getDeclaredField("temp");
            tempField.setAccessible(true);

            currentField = cls.getDeclaredField("current");
            currentField.setAccessible(true);

            waitingField = cls.getDeclaredField("isWaiting");
            waitingField.setAccessible(true);
            return true;
        } catch (Throwable t) {
            EnderInterface.LOGGER.warn("Failed to hook L2MenuStacker ScreenTracker", t);
            trackerGetMethod = null;
            return false;
        }
    }

    public static void clearServerTempOnly(ServerPlayer player) {
        if (!ensureTrackerReflection()) return;
        try {
            Object tracker = trackerGetMethod.invoke(null, player);
            if (tracker != null && tempField != null) {
                tempField.set(tracker, null);
            }
        } catch (Throwable ignored) {}
    }

    public static void clearServerTrackerIfWid(ServerPlayer player, int wid) {
        if (!ensureTrackerReflection()) return;
        try {
            Object tracker = trackerGetMethod.invoke(null, player);
            if (tracker == null) return;
            int currentWid = widField != null ? widField.getInt(tracker) : 0;
            if (wid < 0 || currentWid == wid) {
                if (stackField != null && stackField.get(tracker) instanceof Stack<?> s) {
                    s.clear();
                }
                if (tempField != null) tempField.set(tracker, null);
                if (currentField != null) currentField.set(tracker, null);
                if (widField != null) widField.setInt(tracker, 0);
                if (waitingField != null) waitingField.setBoolean(tracker, false);
            }
        } catch (Throwable ignored) {}
    }

    public static final class Client {
        private static boolean mouseInitAttempted = false;
        @Nullable private static Field mouseGrabField;

        private static boolean clickInitAttempted = false;
        @Nullable private static TagKey<Item> quickAccessTag;
        @Nullable private static Field slotClickMapField;
        @Nullable private static Method isAllowedMethod;
        @Nullable private static Method slotClickToServerMethod;

        private Client() {}

        public static void clearTrackerIfWid(int wid) {
            if (!ensureTrackerReflection()) return;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            try {
                Object tracker = trackerGetMethod.invoke(null, player);
                if (tracker == null) return;
                int currentWid = widField != null ? widField.getInt(tracker) : 0;
                if (wid < 0 || currentWid == wid) {
                    if (stackField != null && stackField.get(tracker) instanceof Stack<?> s) {
                        s.clear();
                    }
                    if (tempField != null) tempField.set(tracker, null);
                    if (currentField != null) currentField.set(tracker, null);
                    if (widField != null) widField.setInt(tracker, 0);
                    if (waitingField != null) waitingField.setBoolean(tracker, false);
                }
            } catch (Throwable ignored) {}
        }

        public static void resetMouseCache() {
            if (!isLoaded()) return;
            if (!mouseInitAttempted) {
                mouseInitAttempted = true;
                try {
                    Class<?> cls = Class.forName("dev.xkmc.l2menustacker.init.MouseCache");
                    mouseGrabField = cls.getDeclaredField("grab");
                    mouseGrabField.setAccessible(true);
                } catch (Throwable t) {
                    mouseGrabField = null;
                }
            }
            if (mouseGrabField != null) {
                try {
                    mouseGrabField.setBoolean(null, false);
                } catch (Throwable ignored) {}
            }
        }

        @SuppressWarnings("unchecked")
        private static boolean ensureClickReflection() {
            if (!isLoaded()) return false;
            if (clickInitAttempted) return slotClickMapField != null;
            clickInitAttempted = true;
            try {
                Class<?> tagCls = Class.forName("dev.xkmc.l2menustacker.init.L2MSTagGen");
                Field tagField = tagCls.getDeclaredField("QUICK_ACCESS");
                tagField.setAccessible(true);
                quickAccessTag = (TagKey<Item>) tagField.get(null);

                Class<?> handlerCls = Class.forName("dev.xkmc.l2menustacker.click.SlotClickHandler");
                slotClickMapField = handlerCls.getDeclaredField("MAP");
                slotClickMapField.setAccessible(true);
                isAllowedMethod = handlerCls.getMethod("isAllowed", ItemStack.class);
                slotClickToServerMethod = handlerCls.getMethod("slotClickToServer", int.class, int.class, int.class);
                return true;
            } catch (Throwable t) {
                EnderInterface.LOGGER.warn("Failed to hook L2MenuStacker SlotClickHandler", t);
                slotClickMapField = null;
                return false;
            }
        }

        @Nullable
        private static Object findMatchingSlotClickHandler(Slot slot, int wid) {
            if (!ensureClickReflection()) return null;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return null;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) return null;
            if (stack.isStackable()) {
                if (stack.getCount() > 1) return null;
                if (quickAccessTag == null || !stack.is(quickAccessTag)) return null;
            }

            boolean isPlayerInv = (slot.container == player.getInventory());
            if (!isPlayerInv && wid <= 0) return null;

            int inv = isPlayerInv ? slot.getSlotIndex() : -1;
            int ind = (inv == -1) ? slot.index : -1;
            if (inv < 0 && ind < 0) return null;

            try {
                Map<?, ?> map = (Map<?, ?>) slotClickMapField.get(null);
                if (map == null) return null;
                for (Object handler : map.values()) {
                    if ((boolean) isAllowedMethod.invoke(handler, stack)) {
                        return handler;
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }

        public static boolean canHandleSlotRightClick(Slot slot, int wid) {
            return findMatchingSlotClickHandler(slot, wid) != null;
        }

        public static boolean tryHandleSlotRightClick(Slot slot, int wid) {
            Object handler = findMatchingSlotClickHandler(slot, wid);
            if (handler == null) return false;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;

            boolean isPlayerInv = (slot.container == player.getInventory());
            int inv = isPlayerInv ? slot.getSlotIndex() : -1;
            int ind = (inv == -1) ? slot.index : -1;
            try {
                slotClickToServerMethod.invoke(handler, ind, inv, wid);
                return true;
            } catch (Throwable t) {
                EnderInterface.LOGGER.warn("Failed to invoke L2MenuStacker slotClickToServer", t);
                return false;
            }
        }
    }
}