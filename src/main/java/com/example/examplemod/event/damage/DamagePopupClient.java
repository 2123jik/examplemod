package com.example.examplemod.event.damage;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class DamagePopupClient {

    private static final List<Popup> POPUPS = new ArrayList<>();
    private static final Map<Integer, ComboTracker> COMBO_MAP = new HashMap<>();
    private static final DecimalFormat FORMAT = new DecimalFormat("#.#");
    private static final int MAX_POPUPS = 120;

    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        event.enqueueWork(() -> ModList.get().getModContainerById(ExampleMod.MODID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new)
        ));
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        syncFilterPreferences();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == DamagePopupConfig.CLIENT_SPEC) {
            scheduleSyncFilterPreferences();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == DamagePopupConfig.CLIENT_SPEC) {
            scheduleSyncFilterPreferences();
        }
    }

    private static void scheduleSyncFilterPreferences() {
        Minecraft.getInstance().execute(DamagePopupClient::syncFilterPreferences);
    }

    private static void syncFilterPreferences() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return;
        }
        PacketDistributor.sendToServer(new DamagePopupServer.DamagePopupFilterSyncPayload(
                DamagePopupConfig.getClientPopupMask(),
                DamagePopupConfig.getClientChatMask()
        ));
    }

    public static void handlePayload(DamagePopupServer.DamagePopupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return;
            }

            int selfId = mc.player.getId();
            boolean showPopup = DamagePopupConfig.matchesMask(
                    DamagePopupConfig.getClientPopupMask() & DamagePopupConfig.getServerPopupMask(),
                    selfId,
                    payload.targetId(),
                    payload.attackerId(),
                    payload.ownerId(),
                    payload.targetIsPlayer(),
                    payload.attackerIsPlayer(),
                    payload.ownerIsPlayer()
            );
            boolean showChat = DamagePopupConfig.matchesMask(
                    DamagePopupConfig.getClientChatMask() & DamagePopupConfig.getServerChatMask(),
                    selfId,
                    payload.targetId(),
                    payload.attackerId(),
                    payload.ownerId(),
                    payload.targetIsPlayer(),
                    payload.attackerIsPlayer(),
                    payload.ownerIsPlayer()
            );

            if (showPopup) {
                spawnPopup(mc, payload);
            }
            if (showChat) {
                sendChatDamageReport(mc, payload);
            }
        });
    }

    private static void spawnPopup(Minecraft mc, DamagePopupServer.DamagePopupPayload payload) {
        long now = mc.level.getGameTime();
        ComboTracker tracker = COMBO_MAP.computeIfAbsent(payload.targetId(), id -> new ComboTracker());
        int slot = tracker.nextSlot(now);

        // 获取当前摄像机基准向量，确保弹字横向散射始终对齐玩家视野左右
        Camera camera = mc.gameRenderer.getMainCamera();
        Vector3f camRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.rotation());
        Vector3f camBack = new Vector3f(0.0F, 0.0F, 1.0F).rotate(camera.rotation());

        // 交替扇形槽位：0->右1, 1->左1, 2->右2, 3->左2, 4->右3, 5->左3
        int fanIdx = slot % 6;
        float side = (fanIdx % 2 == 0) ? 1.0F : -1.0F;
        int ring = fanIdx / 2;

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        float jitter = (rand.nextFloat() - 0.5F) * 0.012F;
        float lateralSpeed = side * (0.044F + ring * 0.022F) + jitter;

        // 将生成点略微拉向摄像机外侧，确保 NORMAL 模式下不会被怪物身体完全遮挡
        double forwardPull = payload.targetWidth() * 0.42 + 0.12 + (slot % 4) * 0.03;
        double spawnX = payload.x() + camBack.x() * forwardPull + camRight.x() * side * 0.08;
        double spawnY = payload.y() + (rand.nextDouble() - 0.5) * 0.12;
        double spawnZ = payload.z() + camBack.z() * forwardPull + camRight.z() * side * 0.08;

        // 水平速度 = 摄像机左右扇形分量 + 服务端受击方向冲量
        double knockImpulse = 0.018 + 0.024 * payload.impactWeight();
        double vx = camRight.x() * lateralSpeed + payload.hitDirX() * knockImpulse;
        double vz = camRight.z() * lateralSpeed + payload.hitDirZ() * knockImpulse;

        // 垂直初速度：重击抛得更高
        double vy = 0.145 + 0.055 * payload.impactWeight() + rand.nextDouble() * 0.018;

        // 节奏梯度：轻击 18 tick 快速清场，重击 30 tick 突出存在感
        int maxAge = 18 + Math.round(payload.impactWeight() * 12.0F);

        Component displayText = formatDamageText(payload.damage(), payload.impactWeight());

        if (POPUPS.size() >= MAX_POPUPS) {
            POPUPS.remove(0);
        }

        POPUPS.add(new Popup(
                spawnX, spawnY, spawnZ,
                vx, vy, vz,
                displayText,
                payload.color() & 0x00FFFFFF,
                payload.impactWeight(),
                maxAge
        ));
    }

    private static void sendChatDamageReport(Minecraft mc, DamagePopupServer.DamagePopupPayload payload) {
        if (mc.level == null) {
            return;
        }

        Entity target = mc.level.getEntity(payload.targetId());
        Component targetName = target != null
                ? target.getDisplayName()
                : Component.literal("未知实体#" + payload.targetId());

        Component attackerName;
        if (payload.attackerId() >= 0) {
            Entity attacker = mc.level.getEntity(payload.attackerId());
            attackerName = attacker != null
                    ? attacker.getDisplayName()
                    : Component.literal("未知实体#" + payload.attackerId());
        } else {
            attackerName = Component.literal("环境");
        }

        String damageStr = FORMAT.format(payload.damage());
        int damageColor = payload.color() & 0x00FFFFFF;
        String damageTypeId = payload.damageTypeId();

        Component typeComponent = Component.literal(damageTypeId)
                .withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, damageTypeId))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("点击复制: " + damageTypeId)
                        ))
                );

        Component report = Component.empty()
                .append(Component.literal("[伤害] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(attackerName.copy().withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" 对 ").withStyle(ChatFormatting.GRAY))
                .append(targetName.copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" 造成 ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(damageStr).withStyle(style -> style.withColor(damageColor).withBold(true)))
                .append(Component.literal(" 点 ").withStyle(ChatFormatting.GRAY))
                .append(typeComponent);

        mc.gui.getChat().addMessage(report);
    }

    private static Component formatDamageText(float damage, float impactWeight) {
        String numStr = damage >= 100.0F
                ? Integer.toString(Math.round(damage))
                : FORMAT.format(damage);
        boolean isHeavy = impactWeight >= 0.50F;
        boolean isCrit = impactWeight >= 0.75F;

        MutableComponent component = Component.literal(isCrit ? numStr + "!" : numStr);
        if (isHeavy) {
            component.withStyle(ChatFormatting.BOLD);
        }
        return component;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!POPUPS.isEmpty()) {
                POPUPS.clear();
            }
            if (!COMBO_MAP.isEmpty()) {
                COMBO_MAP.clear();
            }
            return;
        }
        if (mc.isPaused()) {
            return;
        }

        // 1. 局部防重叠斥力：轻推距离过近的弹字，避免多段 DoT 叠在一起
        int count = POPUPS.size();
        for (int i = 0; i < count; i++) {
            Popup a = POPUPS.get(i);
            for (int j = i + 1; j < count; j++) {
                Popup b = POPUPS.get(j);
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double dz = b.z - a.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                double minDist = 0.42;
                if (distSq < minDist * minDist) {
                    double dist = Math.sqrt(distSq);
                    double overlap = (minDist - dist) * 0.055;
                    if (dist > 1.0E-4) {
                        double nx = dx / dist;
                        double ny = (dy / dist) * 1.35;
                        double nz = dz / dist;
                        a.vx -= nx * overlap;
                        a.vy -= ny * overlap;
                        a.vz -= nz * overlap;
                        b.vx += nx * overlap;
                        b.vy += ny * overlap;
                        b.vz += nz * overlap;
                    } else {
                        a.vy -= overlap;
                        b.vy -= overlap;
                    }
                }
            }
        }

        // 2. 更新物理积分与生命周期
        Iterator<Popup> iterator = POPUPS.iterator();
        while (iterator.hasNext()) {
            Popup popup = iterator.next();
            popup.tick();
            if (popup.isExpired()) {
                iterator.remove();
            }
        }

        // 定期清理长时间未受击的连击追踪器
        if ((mc.level.getGameTime() & 63L) == 0L && !COMBO_MAP.isEmpty()) {
            long now = mc.level.getGameTime();
            COMBO_MAP.entrySet().removeIf(e -> now - e.getValue().lastTick > 100L);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 必须为 AFTER_PARTICLES：此时 PoseStack 绝对有效存在，不会导致空指针崩溃
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || POPUPS.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        // 摄像机由远及近排序，保证半透明文字前后层级正确
        for (Popup popup : POPUPS) {
            popup.renderX = Mth.lerp(partialTick, popup.prevX, popup.x);
            popup.renderY = Mth.lerp(partialTick, popup.prevY, popup.y);
            popup.renderZ = Mth.lerp(partialTick, popup.prevZ, popup.z);
            popup.camDistSq = camPos.distanceToSqr(popup.renderX, popup.renderY, popup.renderZ);
        }
        POPUPS.sort(Comparator.comparingDouble((Popup p) -> p.camDistSq).reversed());

        Font font = mc.font;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (Popup popup : POPUPS) {
            float ageF = popup.age + partialTick;
            float progress = Mth.clamp(ageF / popup.maxAge, 0.0F, 1.0F);

            // 1. 纯净统一的 Alpha 淡出（最后 25% 平滑消失）
            int alpha = 255;
            if (progress > 0.75F) {
                alpha = (int) ((1.0F - (progress - 0.75F) / 0.25F) * 255.0F);
            }
            alpha = Mth.clamp(alpha, 10, 255);
            int argb = (alpha << 24) | (popup.rgb & 0x00FFFFFF);

            // 2. 基础尺寸与远近适度补偿
            double dist = Math.sqrt(popup.camDistSq);
            float distComp = Mth.clamp((float) Math.pow(Math.max(dist, 1.0), 0.5) * 0.45F, 0.85F, 2.5F);
            float popScale = (ageF < 3.0F) ? 1.22F : 1.0F; // 刚跳出时的打击微弹
            float finalScale = 0.024F * distComp * popScale;

            // 3. 构建朝向相机的公告板矩阵
            poseStack.pushPose();
            poseStack.translate(
                    popup.renderX - camPos.x,
                    popup.renderY - camPos.y,
                    popup.renderZ - camPos.z
            );
            poseStack.mulPose(camera.rotation());
            poseStack.scale(finalScale, -finalScale, finalScale);

            // 几何居中
            float textX = -font.width(popup.displayText) / 2.0F;
            float textY = -font.lineHeight / 2.0F;

            // 4. 关键点：使用 NORMAL 模式 + dropShadow = true
            // NORMAL 会写入实际世界深度值（几米），而云层在几百米开外。
            // 随后原版绘制云层时，因深度测试发现云在文字后面，因此白云绝不会再遮挡伤害字！
            font.drawInBatch(
                    popup.visualText,
                    textX,
                    textY,
                    argb,
                    true, // 启用原版原生阴影，字体清晰锐利
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL, // 解决云层遮挡的关键
                    0,
                    LightTexture.FULL_BRIGHT
            );

            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    private static class ComboTracker {
        long lastTick = -1000L;
        int count = 0;

        int nextSlot(long now) {
            if (now - lastTick > 25L) {
                count = 0;
            } else {
                count++;
            }
            lastTick = now;
            return count;
        }
    }

    private static class Popup {
        final Component displayText;
        final FormattedCharSequence visualText;
        final int rgb;
        final float impactWeight;
        final int maxAge;

        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        int age = 0;

        double renderX, renderY, renderZ;
        double camDistSq;

        Popup(
                double x, double y, double z,
                double vx, double vy, double vz,
                Component displayText, int rgb,
                float impactWeight, int maxAge
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.displayText = displayText;
            this.visualText = displayText.getVisualOrderText();
            this.rgb = rgb;
            this.impactWeight = impactWeight;
            this.maxAge = maxAge;
        }

        void tick() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;

            this.x += this.vx;
            this.y += this.vy;
            this.z += this.vz;

            // 抛物线重力 + 顶点滞空顿挫（Apex Hang-Time）
            boolean nearApex = Math.abs(this.vy) < 0.032;
            double gravity = nearApex ? 0.0052 : 0.0138;
            double horizontalDrag = nearApex ? 0.85 : 0.93;

            this.vy = Math.max(-0.055, this.vy - gravity);
            this.vx *= horizontalDrag;
            this.vz *= horizontalDrag;

            this.age++;
        }

        boolean isExpired() {
            return this.age >= this.maxAge;
        }
    }
}