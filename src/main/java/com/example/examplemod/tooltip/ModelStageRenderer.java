package com.example.examplemod.tooltip;

import com.example.examplemod.Config;
import com.example.examplemod.compat.jei.CuriosCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelStageRenderer {
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 100;
    private static final int GAP = 8;
    private static final int LEFT_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 30;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static net.minecraft.client.multiplayer.ClientLevel activeLevel;
    private static net.minecraft.client.player.LocalPlayer activePlayer;
    private static final Map<EntityType<?>, LivingEntity> ARMOR_ENTITY_CACHE =
            new ConcurrentHashMap<>();

    private static ItemStack activeHoverStack = ItemStack.EMPTY;
    private static LivingEntity currentTargetEntity = null;

    private static final Set<Item> CRASHED_ITEMS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<EntityType<?>, LivingEntity> ENTITY_CACHE = new ConcurrentHashMap<>();

    private ModelStageRenderer() {}

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearCache();
    }

    public static void clearCache() {
        ENTITY_CACHE.clear();
        CRASHED_ITEMS.clear();
        currentTargetEntity = null;
        activeHoverStack = ItemStack.EMPTY;
        activeLevel = null;
        activePlayer = null;
        ARMOR_ENTITY_CACHE.clear();

    }

    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        // 总开关与防打扰
        if (!Config.ENABLED.get() || !Config.SHOW_MODEL_STAGE.get()) return;

        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || stack.isEmpty()) return;

        if (stack.has(DataComponents.HIDE_TOOLTIP)) return;
        if (CRASHED_ITEMS.contains(stack.getItem())) return;

        try {
            updateTarget(mc, stack);
        } catch (RuntimeException | LinkageError exception) {
            CRASHED_ITEMS.add(stack.getItem());
            currentTargetEntity = null;
            LOGGER.warn("无法创建物品模型预览: {}", stack.getItem(), exception);
            return;
        }


        if (currentTargetEntity == null) return;
        int screenH = mc.getWindow().getGuiScaledHeight();

        int cardX = LEFT_MARGIN;

        int cardY = screenH - CARD_HEIGHT - BOTTOM_MARGIN;

        cardY = Math.max(6, cardY);

        GuiGraphics graphics = event.getGraphics();
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, 400);

            graphics.fill(
                    cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, 0x90101015
            );
            graphics.renderOutline(
                    cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0x6655FFFF
            );

            LivingEntity target = currentTargetEntity;
            float height = target.getBbHeight();
            float width = target.getBbWidth();

            if (!Float.isFinite(height) || height <= 0.05F) height = 1.0F;
            if (!Float.isFinite(width) || width <= 0.05F) width = 1.0F;

            float maximum = Math.max(height, width * 1.15F);
            float pixels = Math.min(CARD_WIDTH - 24, CARD_HEIGHT - 38);
            int scale = Mth.clamp(Math.round(pixels / maximum), 10, 68);

            float yaw = (System.nanoTime() % 8_000_000_000L)
                    / 8_000_000_000.0F * 360.0F;
            int centerX = cardX + CARD_WIDTH / 2;
            int centerY = cardY + CARD_HEIGHT / 2 + Math.round(height * scale * 0.42F);

            graphics.enableScissor(
                    cardX + 1, cardY + 1,
                    cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1
            );
            try {
                renderSpinningEntity(graphics, centerX, centerY, scale, yaw, 15.0F, target);
            } finally {
                graphics.disableScissor();
            }
        } catch (RuntimeException | LinkageError exception) {
            CRASHED_ITEMS.add(stack.getItem());
            currentTargetEntity = null;
            LOGGER.warn("无法渲染物品模型预览: {}", stack.getItem(), exception);
        } finally {
            pose.popPose();
        }

    }

    private static void updateTarget(Minecraft mc, ItemStack stack) {
        if (activeLevel != mc.level || activePlayer != mc.player) {
            clearCache();
            activeLevel = mc.level;
            activePlayer = mc.player;
        }
        activeHoverStack = stack.copy();
        currentTargetEntity = null;

        if (stack.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> eggType = egg.getType(stack);
            if (eggType != null) {
                currentTargetEntity = getCachedEntity(mc, eggType);
                return;
            }
        }

        if (stack.getItem() instanceof AnimalArmorItem animalArmor) {
            if (animalArmor.getBodyType() == AnimalArmorItem.BodyType.CANINE) {
                LivingEntity wolf = getCachedEntity(mc, EntityType.WOLF, ARMOR_ENTITY_CACHE);
                if (wolf != null) {
                    for (EquipmentSlot s : EquipmentSlot.values()) wolf.setItemSlot(s, ItemStack.EMPTY);
                    wolf.setItemSlot(EquipmentSlot.BODY, stack.copy());
                    currentTargetEntity = wolf;
                }
                return;
            } else if (animalArmor.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
                LivingEntity horse = getCachedEntity(mc, EntityType.HORSE, ARMOR_ENTITY_CACHE);
                if (horse != null) {
                    for (EquipmentSlot s : EquipmentSlot.values()) horse.setItemSlot(s, ItemStack.EMPTY);
                    horse.setItemSlot(EquipmentSlot.BODY, stack.copy());
                    currentTargetEntity = horse;
                }
                return;
            }
        }

        if (mc.level == null || mc.player == null) return;
        RemotePlayer freshDummy = new RemotePlayer(mc.level, mc.player.getGameProfile());
        freshDummy.setPose(Pose.STANDING);

        if (CuriosCompat.isCurio(stack)) {
            if (CuriosCompat.tryEquipCurio(freshDummy, stack.copy())) {
                currentTargetEntity = freshDummy;
                return;
            }
        }

        EquipmentSlot slot = computeHumanoidSlot(stack);
        if (slot != null) {
            freshDummy.setItemSlot(slot, stack.copy());
            currentTargetEntity = freshDummy;
        }
    }

    private static LivingEntity getCachedEntity(Minecraft mc, EntityType<?> type) {
        return getCachedEntity(mc, type, ENTITY_CACHE);
    }

    private static LivingEntity getCachedEntity(
            Minecraft mc,
            EntityType<?> type,
            Map<EntityType<?>, LivingEntity> cache
    ) {
        if (mc.level == null) return null;

        LivingEntity existing = cache.get(type);
        if (existing != null && existing.level() == mc.level) return existing;

        var created = type.create(mc.level);
        if (!(created instanceof LivingEntity living)) return null;

        if (living instanceof Horse horse) {
            horse.setTamed(true);
        } else if (living instanceof Wolf wolf) {
            wolf.setTame(true, false);
        }

        cache.put(type, living);
        return living;
    }


    private static EquipmentSlot computeHumanoidSlot(ItemStack stack) {
        EquipmentSlot directSlot = stack.getEquipmentSlot();
        if (directSlot != null && isHumanoidSlot(directSlot)) return directSlot;

        if (stack.getItem() instanceof ArmorItem armor && isHumanoidSlot(armor.getType().getSlot())) {
            return armor.getType().getSlot();
        }
        if (stack.getItem() instanceof ShieldItem) return EquipmentSlot.OFFHAND;
        if (stack.is(Items.ELYTRA)) return EquipmentSlot.CHEST;
        if (stack.has(DataComponents.PROFILE) || stack.is(Items.PLAYER_HEAD) || stack.is(Items.CARVED_PUMPKIN)) {
            return EquipmentSlot.HEAD;
        }

        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        if (!modifiers.modifiers().isEmpty()) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                for (var entry : modifiers.modifiers()) {
                    if (entry.slot().test(s) && !entry.slot().test(EquipmentSlot.MAINHAND)) return s;
                }
            }
            for (var entry : modifiers.modifiers()) {
                if (entry.slot().test(EquipmentSlot.OFFHAND) && !entry.slot().test(EquipmentSlot.MAINHAND)) return EquipmentSlot.OFFHAND;
            }
        }

        if (stack.has(DataComponents.TOOL)
                || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem
                || stack.getItem() instanceof FishingRodItem
                || stack.getItem() instanceof ShearsItem
                || stack.getItem() instanceof BrushItem
                || stack.getItem() instanceof SpyglassItem) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    private static boolean isHumanoidSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ||
                slot == EquipmentSlot.CHEST ||
                slot == EquipmentSlot.LEGS ||
                slot == EquipmentSlot.FEET ||
                slot == EquipmentSlot.MAINHAND ||
                slot == EquipmentSlot.OFFHAND;
    }

    private static void renderSpinningEntity(
            GuiGraphics graphics,
            int posX,
            int posY,
            int scale,
            float yaw,
            float pitch,
            LivingEntity entity
    ) {
        var pose = graphics.pose();
        EntityRenderDispatcher dispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();

        // 先提交之前的 GUI 绘制，避免混入实体渲染状态。
        graphics.flush();

        pose.pushPose();
        try {
            pose.translate(posX, posY, 100.0);
            pose.scale(scale, scale, -scale);
            pose.mulPose(new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX(pitch * Mth.DEG_TO_RAD)
                    .rotateY(yaw * Mth.DEG_TO_RAD));

            entity.walkAnimation.setSpeed(0.0F);
            entity.walkAnimation.position(0.0F);
            entity.yBodyRot = entity.yBodyRotO = 0.0F;
            entity.yHeadRot = entity.yHeadRotO = 0.0F;
            entity.yRotO = entity.xRotO = 0.0F;
            entity.setYRot(0.0F);
            entity.setXRot(0.0F);

            dispatcher.setRenderShadow(false);
            try {
                RenderSystem.runAsFancy(() -> dispatcher.render(
                        entity, 0.0, 0.0, 0.0, 0.0F, 1.0F,
                        pose, buffers, 0xF000F0
                ));
            } finally {
                try {
                    buffers.endBatch();
                } finally {
                    dispatcher.setRenderShadow(true);
                }
            }
        } finally {
            pose.popPose();
        }
    }
}