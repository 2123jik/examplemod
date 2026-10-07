package com.example.examplemod.client.tooltip;

import com.example.examplemod.Config;
import com.example.examplemod.compat.CuriosPreviewCompat;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelStageRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 100;
    private static final int LEFT_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 30;

    private static final long ROTATION_PERIOD_NS = 8_000_000_000L;
    private static final long RETRY_DELAY_NS = 5_000_000_000L;
    private static final int MAX_FAILURES = 64;

    private static final EquipmentSlot[] EQUIPMENT_SLOTS =
            EquipmentSlot.values();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /*
     * 所有状态均由客户端线程访问。
     * 不使用并发容器，因为实体和渲染状态本身也不是线程安全的。
     */
    private static ClientLevel activeLevel;
    private static LocalPlayer activePlayer;

    private static RemotePlayer dummyPlayer;

    // 只缓存一个非玩家预览实体。
    private static EntityType<?> cachedEntityType;
    private static LivingEntity cachedEntity;

    private static LivingEntity currentTargetEntity;
    private static ItemStack activeHoverStack = ItemStack.EMPTY;
    private static boolean targetResolved;

    /*
     * 失败记录只保存注册物品和时间，不保存物品栈、实体或世界。
     * 同一种物品失败后延迟重试，最多保留 MAX_FAILURES 条。
     */
    private static final Map<Item, Long> FAILURE_TIMES =
            new LinkedHashMap<>();

    private ModelStageRenderer() {}

    @SubscribeEvent
    public static void onClientLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        clearCache();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel unloadedLevel)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // 不在其他线程直接修改客户端预览状态。
        mc.execute(() -> {
            if (activeLevel == unloadedLevel) {
                clearCache();
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        synchronizeContext(Minecraft.getInstance());
    }

    /**
     * 应在客户端线程调用。
     *
     * 这些预览实体没有加入世界，释放本类引用即可。
     * 不调用实体移除逻辑，以免触发额外的世界/模组生命周期行为。
     */
    public static void clearCache() {
        currentTargetEntity = null;
        cachedEntity = null;
        cachedEntityType = null;
        dummyPlayer = null;

        activeHoverStack = ItemStack.EMPTY;
        targetResolved = false;

        activeLevel = null;
        activePlayer = null;

        FAILURE_TIMES.clear();
    }

    private static boolean isEnabled() {
        return Config.ENABLED.get() && Config.SHOW_MODEL_STAGE.get();
    }

    private static boolean synchronizeContext(Minecraft mc) {
        if (!isEnabled() || mc.level == null || mc.player == null) {
            clearCache();
            return false;
        }

        if (activeLevel != mc.level || activePlayer != mc.player) {
            clearCache();
            activeLevel = mc.level;
            activePlayer = mc.player;
        }

        return true;
    }

    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        // 生命周期检查必须先于物品和失败记录的提前返回。
        if (!synchronizeContext(mc)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.has(DataComponents.HIDE_TOOLTIP)) {
            return;
        }

        if (isCoolingDown(stack.getItem())) {
            return;
        }

        try {
            updateTarget(mc, stack);
        } catch (RuntimeException | LinkageError exception) {
            recordFailure(stack, "创建", exception);
            return;
        }

        LivingEntity target = currentTargetEntity;
        if (target == null) {
            return;
        }

        GuiGraphics graphics = event.getGraphics();
        var pose = graphics.pose();

        pose.pushPose();
        try {
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int cardX = LEFT_MARGIN;
            int cardY = Math.max(
                    6,
                    screenHeight - CARD_HEIGHT - BOTTOM_MARGIN
            );

            pose.translate(0.0F, 0.0F, 400.0F);

            graphics.fill(
                    cardX,
                    cardY,
                    cardX + CARD_WIDTH,
                    cardY + CARD_HEIGHT,
                    0x90101015
            );

            graphics.renderOutline(
                    cardX,
                    cardY,
                    CARD_WIDTH,
                    CARD_HEIGHT,
                    0x6655FFFF
            );

            float height = validDimension(target.getBbHeight());
            float width = validDimension(target.getBbWidth());

            float maximum = Math.max(height, width * 1.15F);
            float pixels = Math.min(CARD_WIDTH - 24, CARD_HEIGHT - 38);

            // 允许大型实体缩到 1 以下，避免原来的最小缩放 10。
            float scale = Math.min(pixels / maximum, 68.0F);

            float yaw = (float) (
                    Math.floorMod(System.nanoTime(), ROTATION_PERIOD_NS)
                            / (double) ROTATION_PERIOD_NS * 360.0
            );

            int centerX = cardX + CARD_WIDTH / 2;
            int centerY = cardY + CARD_HEIGHT / 2
                    + Math.round(height * scale * 0.42F);

            graphics.enableScissor(
                    cardX + 1,
                    cardY + 1,
                    cardX + CARD_WIDTH - 1,
                    cardY + CARD_HEIGHT - 1
            );

            try {
                renderSpinningEntity(
                        graphics,
                        centerX,
                        centerY,
                        scale,
                        yaw,
                        15.0F,
                        target
                );
            } finally {
                graphics.disableScissor();
            }
        } catch (RuntimeException | LinkageError exception) {
            recordFailure(stack, "渲染", exception);
        } finally {
            pose.popPose();
        }
    }

    private static float validDimension(float value) {
        return Float.isFinite(value) && value > 0.05F ? value : 1.0F;
    }

    private static boolean isCoolingDown(Item item) {
        Long failureTime = FAILURE_TIMES.get(item);
        if (failureTime == null) {
            return false;
        }

        if (System.nanoTime() - failureTime < RETRY_DELAY_NS) {
            return true;
        }

        FAILURE_TIMES.remove(item);
        return false;
    }

    private static void recordFailure(
            ItemStack stack,
            String phase,
            Throwable exception
    ) {
        Item item = stack.getItem();

        FAILURE_TIMES.remove(item);
        FAILURE_TIMES.put(item, System.nanoTime());

        while (FAILURE_TIMES.size() > MAX_FAILURES) {
            var iterator = FAILURE_TIMES.keySet().iterator();
            iterator.next();
            iterator.remove();
        }

        /*
         * 不复用可能只初始化了一半、或被异常渲染器修改过的实体。
         * 下一次允许重试时重新创建。
         */
        currentTargetEntity = null;
        cachedEntity = null;
        cachedEntityType = null;
        dummyPlayer = null;

        activeHoverStack = ItemStack.EMPTY;
        targetResolved = false;

        LOGGER.warn(
                "无法{}物品模型预览，稍后重试: {}",
                phase,
                item,
                exception
        );
    }

    private static void updateTarget(Minecraft mc, ItemStack stack) {
        if (targetResolved && ItemStack.matches(activeHoverStack, stack)) {
            return;
        }

        targetResolved = false;
        currentTargetEntity = null;
        activeHoverStack = ItemStack.EMPTY;

        /*
         * 切换任何物品时都清空假人：
         * 包括饰品 -> 普通装备，以及饰品 -> 刷怪蛋。
         */
        if (dummyPlayer != null) {
            resetDummy(dummyPlayer);
        }

        if (cachedEntity != null) {
            clearEquipment(cachedEntity);
        }

        LivingEntity resolved = resolveTarget(mc, stack);

        // 配置全部成功后才提交快照，失败由外层统一处理。
        activeHoverStack = stack.copy();
        currentTargetEntity = resolved;
        targetResolved = true;
    }

    private static LivingEntity resolveTarget(
            Minecraft mc,
            ItemStack stack
    ) {
        if (stack.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> type = egg.getType(stack);
            return type == null ? null : getPreviewEntity(mc, type);
        }

        if (stack.getItem() instanceof AnimalArmorItem animalArmor) {
            EntityType<?> type;

            if (animalArmor.getBodyType()
                    == AnimalArmorItem.BodyType.CANINE) {
                type = EntityType.WOLF;
            } else if (animalArmor.getBodyType()
                    == AnimalArmorItem.BodyType.EQUESTRIAN) {
                type = EntityType.HORSE;
            } else {
                return null;
            }

            LivingEntity animal = getPreviewEntity(mc, type);
            if (animal != null) {
                animal.setItemSlot(EquipmentSlot.BODY, stack.copy());
            }
            return animal;
        }

        boolean curio = CuriosPreviewCompat.isCurio(stack);
        EquipmentSlot slot = computeHumanoidSlot(stack);

        // 不支持的物品无需创建假人。
        if (!curio && slot == null) {
            return null;
        }

        RemotePlayer dummy = getOrCreateDummy(mc);

        if (curio && CuriosPreviewCompat.applySinglePreviewCurio(
                dummy, stack
        )) {
            return dummy;
        }

        if (slot != null) {
            dummy.setItemSlot(slot, stack.copy());
            return dummy;
        }

        return null;
    }

    private static RemotePlayer getOrCreateDummy(Minecraft mc) {
        if (dummyPlayer == null) {
            RemotePlayer created = new RemotePlayer(
                    mc.level,
                    mc.player.getGameProfile()
            );

            created.setPose(Pose.STANDING);
            resetDummy(created);
            dummyPlayer = created;
        }

        return dummyPlayer;
    }

    private static void resetDummy(RemotePlayer dummy) {
        clearEquipment(dummy);
        CuriosPreviewCompat.clearPreviewCurios(dummy);
        dummy.setPose(Pose.STANDING);
    }

    private static void clearEquipment(LivingEntity entity) {
        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            if (!entity.getItemBySlot(slot).isEmpty()) {
                entity.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static LivingEntity getPreviewEntity(
            Minecraft mc,
            EntityType<?> type
    ) {
        if (cachedEntityType == type && cachedEntity != null) {
            return cachedEntity;
        }

        cachedEntity = null;
        cachedEntityType = null;

        var created = type.create(mc.level);
        if (!(created instanceof LivingEntity living)) {
            return null;
        }

        if (living instanceof Horse horse) {
            horse.setTamed(true);
        } else if (living instanceof Wolf wolf) {
            wolf.setTame(true, false);
        }

        cachedEntityType = type;
        cachedEntity = living;
        return living;
    }

    private static EquipmentSlot computeHumanoidSlot(ItemStack stack) {
        EquipmentSlot directSlot = stack.getEquipmentSlot();
        if (directSlot != null && isHumanoidSlot(directSlot)) {
            return directSlot;
        }

        if (stack.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getType().getSlot();
            if (isHumanoidSlot(slot)) {
                return slot;
            }
        }

        if (stack.getItem() instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }

        if (stack.is(Items.ELYTRA)) {
            return EquipmentSlot.CHEST;
        }

        if (stack.has(DataComponents.PROFILE)
                || stack.is(Items.PLAYER_HEAD)
                || stack.is(Items.CARVED_PUMPKIN)) {
            return EquipmentSlot.HEAD;
        }

        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            for (var entry : modifiers.modifiers()) {
                if (entry.slot().test(slot)
                        && !entry.slot().test(EquipmentSlot.MAINHAND)) {
                    return slot;
                }
            }
        }

        for (var entry : modifiers.modifiers()) {
            if (entry.slot().test(EquipmentSlot.OFFHAND)
                    && !entry.slot().test(EquipmentSlot.MAINHAND)) {
                return EquipmentSlot.OFFHAND;
            }
        }

        if (stack.has(DataComponents.TOOL)
                || stack.getItem() instanceof SwordItem
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
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET
                || slot == EquipmentSlot.MAINHAND
                || slot == EquipmentSlot.OFFHAND;
    }

    private static void renderSpinningEntity(
            GuiGraphics graphics,
            int posX,
            int posY,
            float scale,
            float yaw,
            float pitch,
            LivingEntity entity
    ) {
        EntityRenderDispatcher dispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();

        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        var pose = graphics.pose();

        graphics.flush();


        Quaternionf previousCamera =
                new Quaternionf(dispatcher.cameraOrientation());

        Quaternionf pitchRotation =
                new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);

        Quaternionf modelRotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .mul(pitchRotation)
                .rotateY(yaw * Mth.DEG_TO_RAD);

        /*
         * 这些是本类独占的预览实体，不修改真实玩家或世界实体。
         */
        entity.walkAnimation.setSpeed(0.0F);
        entity.walkAnimation.position(0.0F);
        entity.yBodyRot = entity.yBodyRotO = 0.0F;
        entity.yHeadRot = entity.yHeadRotO = 0.0F;
        entity.yRotO = entity.xRotO = 0.0F;
        entity.setYRot(0.0F);
        entity.setXRot(0.0F);

        pose.pushPose();
        try {
            pose.translate(posX, posY, 100.0F);
            pose.scale(scale, scale, -scale);
            pose.mulPose(modelRotation);

            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(
                    new Quaternionf(pitchRotation).conjugate()
            );
            dispatcher.setRenderShadow(false);

            try {
                RenderSystem.runAsFancy(() -> dispatcher.render(
                        entity,
                        0.0,
                        0.0,
                        0.0,
                        0.0F,
                        1.0F,
                        pose,
                        buffers,
                        0xF000F0
                ));
            } finally {
                // 在实体光照和当前裁剪仍生效时提交。
                buffers.endBatch();
            }
        } finally {
            dispatcher.overrideCameraOrientation(previousCamera);
            pose.popPose();

            // 回到常规 GUI 物品光照状态。
            Lighting.setupFor3DItems();
        }
    }
}
