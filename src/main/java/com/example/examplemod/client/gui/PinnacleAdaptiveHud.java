package com.example.examplemod.client.gui;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.*;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class PinnacleAdaptiveHud {

    private static final ResourceLocation ADAPTIVE_HUD_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "adaptive_hud");

    private static final int BAR_W = 81;
    private static final int MAIN_H = 5;
    private static final int SUB_H = 2;
    private static final int XP_W = 60;
    private static final int XP_H = 2;

    private static final int EFFECT_ICON = 18;
    private static final int EFFECT_CELL_W = 26;
    private static final int EFFECT_CELL_H = 32;
    private static final int EFFECT_MARGIN = 6;
    private static final int FIRE_DISPLAY_TICKS = 200;

    private static final int COL_BG = 0xCC101010;
    private static final int COL_BORDER = 0xFF000000;
    private static final int COL_HEALTH_OK = 0xFF2ECC71;
    private static final int COL_HEALTH_LOW = 0xFFE74C3C;
    private static final int COL_WITHER = 0xFF607080;
    private static final int COL_POISON = 0xFF8E44AD;
    private static final int COL_FROZEN = 0xFF3498DB;
    private static final int COL_ABSORB = 0xFFFFD700;
    private static final int COL_ARMOR = 0xFFBDC3C7;
    private static final int COL_FOOD = 0xFFE67E22;
    private static final int COL_HUNGER = 0xFF27AE60;
    private static final int COL_MOUNT = 0xFFE91E63;
    private static final int COL_AIR = 0xFF00BCD4;
    private static final int COL_EXP = 0xFF55FF55;
    private static final int COL_JUMP = 0xFFFFAA00;
    private static final int COL_JUMP_COOLDOWN = 0xFFDD5555;
    private static final int COL_FIRE = 0xFFFF6A00;

    private static final int COL_EFFECT_GOOD = 0xFF64CB85;
    private static final int COL_EFFECT_BAD = 0xFFE07070;
    private static final int COL_EFFECT_MASK = 0xA0000000;

    private static final Map<Holder<MobEffect>, EffectClock> EFFECT_CLOCKS = new HashMap<>();

    private static Player trackedPlayer;
    private static Object trackedLevel;
    private static int lastTrackedTick = Integer.MIN_VALUE;
    private static int fireStartTick = -1;

    private PinnacleAdaptiveHud() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ADAPTIVE_HUD_ID, PinnacleAdaptiveHud::renderLayer);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearTracking();
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!canRender(mc)) return;

        ResourceLocation layer = event.getName();
        if (layer.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || layer.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || layer.equals(VanillaGuiLayers.FOOD_LEVEL)
                || layer.equals(VanillaGuiLayers.AIR_LEVEL)
                || layer.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || layer.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || layer.equals(VanillaGuiLayers.JUMP_METER)
                || layer.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || layer.equals(VanillaGuiLayers.EFFECTS)) {
            event.setCanceled(true);
        }
    }

    public static void renderLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearTracking();
            return;
        }

        updateTracking(mc.player);
        if (!canRender(mc)) return;

        render(guiGraphics, mc.player);
    }

    public static void render(GuiGraphics g, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (!canRender(mc) || player != mc.player) return;

        updateTracking(player);

        int midX = g.guiWidth() / 2;
        int leftX = midX - 91;
        int rightX = midX + 91;
        int baseY = g.guiHeight() - 35;
        int upperY = baseY - SUB_H - 2;

        Entity vehicle = player.getVehicle();

        if (mc.gameMode.canHurtPlayer()) {
            renderSurvivalBars(g, player, vehicle, leftX, rightX, baseY, upperY);
        }

        renderExperienceOrJump(g, player, midX, baseY);
        renderFire(g, player, leftX, baseY);
        renderEffects(g, player);
    }

    private static boolean canRender(Minecraft mc) {
        Player player = mc.player;
        return player != null
                && mc.level != null
                && mc.gameMode != null
                && !mc.options.hideGui
                && !player.isSpectator()
                && !player.isDeadOrDying()
                && !player.isSleeping();
    }

    private static void clearTracking() {
        trackedPlayer = null;
        trackedLevel = null;
        lastTrackedTick = Integer.MIN_VALUE;
        fireStartTick = -1;
        EFFECT_CLOCKS.clear();
    }

    private static void updateTracking(Player player) {
        if (trackedPlayer != player || trackedLevel != player.level()) {
            clearTracking();
            trackedPlayer = player;
            trackedLevel = player.level();
        }

        int tick = player.tickCount;
        if (tick == lastTrackedTick) return;

        if (tick < lastTrackedTick) {
            fireStartTick = -1;
            EFFECT_CLOCKS.clear();
        }
        lastTrackedTick = tick;

        if (player.isOnFire()) {
            if (fireStartTick < 0) fireStartTick = tick;
        } else {
            fireStartTick = -1;
        }

        Set<Holder<MobEffect>> active = new HashSet<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (!instance.showIcon()) continue;
            Holder<MobEffect> effect = instance.getEffect();
            active.add(effect);
            EffectClock clock = EFFECT_CLOCKS.get(effect);
            if (clock == null) {
                EFFECT_CLOCKS.put(effect, new EffectClock(instance));
            } else {
                clock.update(instance);
            }
        }
        EFFECT_CLOCKS.keySet().retainAll(active);
    }

    private static void renderSurvivalBars(GuiGraphics g, Player player, Entity vehicle,
                                           int leftX, int rightX, int baseY, int upperY) {
        float maxHealth = player.getMaxHealth();
        float health = player.getHealth();
        float healthRatio = ratio(health, maxHealth);

        int healthColor;
        if (player.hasEffect(MobEffects.WITHER)) healthColor = COL_WITHER;
        else if (player.hasEffect(MobEffects.POISON)) healthColor = COL_POISON;
        else if (player.isFullyFrozen()) healthColor = COL_FROZEN;
        else healthColor = healthRatio <= 0.25F ? COL_HEALTH_LOW : COL_HEALTH_OK;

        drawBar(g, leftX, baseY, BAR_W, MAIN_H, width(healthRatio, BAR_W, true), healthColor, false);

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int absorptionWidth = width(ratio(absorption, maxHealth), BAR_W, true);
            g.fill(leftX, baseY, leftX + absorptionWidth, baseY + 1, COL_ABSORB);
        }

        int armor = player.getArmorValue();
        if (armor > 0) {
            drawBar(g, leftX, upperY, BAR_W, SUB_H, width(ratio(armor, 20), BAR_W, true), COL_ARMOR, false);
        }

        LivingEntity mount = vehicle instanceof LivingEntity living
                && living.showVehicleHealth()
                && living.getMaxHealth() > 0.0F ? living : null;

        if (mount != null) {
            drawBar(g, rightX - BAR_W, baseY, BAR_W, MAIN_H,
                    width(ratio(mount.getHealth(), mount.getMaxHealth()), BAR_W, true), COL_MOUNT, true);
        } else {
            int food = player.getFoodData().getFoodLevel();
            int foodColor = player.hasEffect(MobEffects.HUNGER) ? COL_HUNGER : COL_FOOD;
            drawBar(g, rightX - BAR_W, baseY, BAR_W, MAIN_H, width(ratio(food, 20), BAR_W, true), foodColor, true);
        }

        int maxAir = player.getMaxAirSupply();
        int air = player.getAirSupply();
        if (maxAir > 0 && (air < maxAir || player.isEyeInFluid(FluidTags.WATER))) {
            drawBar(g, rightX - BAR_W, upperY, BAR_W, SUB_H, width(ratio(air, maxAir), BAR_W, true), COL_AIR, true);
        }
    }

    private static void renderExperienceOrJump(GuiGraphics g, Player player, int midX, int baseY) {
        Minecraft mc = Minecraft.getInstance();
        PlayerRideableJumping jumpable = mc.player.jumpableVehicle();

        int barX = midX - XP_W / 2;
        int barY = baseY + MAIN_H + 3;

        if (jumpable != null) {
            boolean coolingDown = jumpable.getJumpCooldown() > 0;
            int fill = coolingDown ? XP_W : width(mc.player.getJumpRidingScale(), XP_W, false);
            drawBar(g, barX, barY, XP_W, XP_H, fill, coolingDown ? COL_JUMP_COOLDOWN : COL_JUMP, false);
            return;
        }

        if (!mc.gameMode.hasExperience()) return;

        drawBar(g, barX, barY, XP_W, XP_H, width(player.experienceProgress, XP_W, false), COL_EXP, false);

        if (player.experienceLevel > 0) {
            String level = Integer.toString(player.experienceLevel);
            g.drawString(mc.font, level, midX - mc.font.width(level) / 2, baseY - 10, COL_EXP, true);
        }
    }

    private static void renderFire(GuiGraphics g, Player player, int leftX, int baseY) {
        if (!player.isOnFire() || fireStartTick < 0) return;

        int elapsedTicks = Math.max(0, player.tickCount - fireStartTick);
        int fireY = baseY - 12;

        drawBar(g, leftX, fireY, BAR_W, SUB_H,
                width(ratio(elapsedTicks, FIRE_DISPLAY_TICKS), BAR_W, false), COL_FIRE, false);

        String label = "燃烧已持续 " + (elapsedTicks / 20) + "s";
        g.drawString(Minecraft.getInstance().font, label, leftX, fireY - 10, COL_FIRE, true);
    }

    private static void renderEffects(GuiGraphics g, Player player) {
        Minecraft mc = Minecraft.getInstance();
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.showIcon()) effects.add(instance);
        }
        if (effects.isEmpty()) return;

        effects.sort(Comparator
                .comparingInt((MobEffectInstance instance) -> instance.getEffect().value().isBeneficial() ? 0 : 1)
                .thenComparing(instance -> instance.getEffect().value().getDescriptionId()));

        int availableWidth = Math.max(EFFECT_CELL_W, g.guiWidth() / 2 - EFFECT_MARGIN);
        int columns = Math.max(1, availableWidth / EFFECT_CELL_W);
        int startY = mc.isDemo() ? 22 : EFFECT_MARGIN;

        for (int i = 0; i < effects.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            int x = EFFECT_MARGIN + EFFECT_CELL_W + column * EFFECT_CELL_W;
            int y = startY + row * EFFECT_CELL_H;

            if (y + EFFECT_CELL_H > g.guiHeight() - 65) break;
            renderEffect(g, mc, effects.get(i), x, y);
        }
    }

    private static void renderEffect(GuiGraphics g, Minecraft mc, MobEffectInstance instance, int x, int y) {
        int borderColor = instance.getEffect().value().isBeneficial() ? COL_EFFECT_GOOD : COL_EFFECT_BAD;

        g.fill(x, y, x + 22, y + 22, borderColor);
        g.fill(x + 1, y + 1, x + 21, y + 21, COL_BG);
        g.flush();

        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(instance.getEffect());
        g.blit(x + 2, y + 2, 0, EFFECT_ICON, EFFECT_ICON, sprite);
        g.flush();

        boolean infinite = instance.isInfiniteDuration();
        if (!infinite) {
            EffectClock clock = EFFECT_CLOCKS.get(instance.getEffect());
            int totalTicks = clock != null ? clock.totalTicks : Math.max(1, instance.getDuration());
            float remaining = ratio(instance.getDuration(), totalTicks);
            int maskHeight = Mth.clamp(Mth.floor(EFFECT_ICON * (1.0F - remaining)), 0, EFFECT_ICON);
            if (maskHeight > 0) {
                g.fill(x + 2, y + 2, x + 2 + EFFECT_ICON, y + 2 + maskHeight, COL_EFFECT_MASK);
            }
        }

        if (instance.getAmplifier() > 0) {
            String amplifier = Long.toString((long) instance.getAmplifier() + 1L);
            g.drawString(mc.font, amplifier, x + 21 - mc.font.width(amplifier), y + 12, 0xFFFFFFFF, true);
        }

        String duration = formatDuration(instance);
        int textColor = !infinite && instance.getDuration() <= 200 ? 0xFFFF7777 : 0xFFFFFFFF;
        g.drawString(mc.font, duration, x + 11 - mc.font.width(duration) / 2, y + 23, textColor, true);
        g.flush();
    }

    private static String formatDuration(MobEffectInstance instance) {
        if (instance.isInfiniteDuration()) return "∞";
        long seconds = (Math.max(0L, instance.getDuration()) + 19L) / 20L;
        if (seconds < 60L) return seconds + "s";
        if (seconds < 3600L) return ((seconds + 59L) / 60L) + "m";
        if (seconds < 86400L) return ((seconds + 3599L) / 3600L) + "h";
        return Math.min(999L, (seconds + 86399L) / 86400L) + "d";
    }

    private static float ratio(float value, float max) {
        if (!Float.isFinite(value) || !Float.isFinite(max) || max <= 0.0F) return 0.0F;
        return Mth.clamp(value / max, 0.0F, 1.0F);
    }

    private static int width(float progress, int totalWidth, boolean minOne) {
        if (totalWidth <= 0 || !Float.isFinite(progress)) return 0;
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        int result = Mth.floor(totalWidth * clamped);
        if (minOne && clamped > 0.0F) result = Math.max(1, result);
        return Mth.clamp(result, 0, totalWidth);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int w, int h, int fillWidth, int color, boolean fromRight) {
        if (w <= 0 || h <= 0) return;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);
        g.fill(x, y, x + w, y + h, COL_BG);

        int fill = Mth.clamp(fillWidth, 0, w);
        if (fill <= 0) return;

        int fillX = fromRight ? x + w - fill : x;
        g.fill(fillX, y, fillX + fill, y + h, color);
    }

    private static final class EffectClock {
        int totalTicks;
        int previousTicks;
        int amplifier;

        EffectClock(MobEffectInstance instance) {
            reset(instance);
        }

        void reset(MobEffectInstance instance) {
            totalTicks = Math.max(1, instance.getDuration());
            previousTicks = instance.getDuration();
            amplifier = instance.getAmplifier();
        }

        void update(MobEffectInstance instance) {
            int remaining = instance.getDuration();
            if (remaining > previousTicks || amplifier != instance.getAmplifier()) {
                reset(instance);
            } else {
                previousTicks = remaining;
            }
        }
    }
}