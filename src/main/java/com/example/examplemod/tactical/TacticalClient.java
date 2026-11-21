package com.example.examplemod.tactical;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class TacticalClient {

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "开启/退出战棋模式", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "战棋玩法"
    );

    // 摄像机俯视状态
    private static boolean tacticalActive = false;
    private static CameraType savedCameraType = CameraType.FIRST_PERSON;
    private static float camPitch = 85.0f; // 85°~89° = 纯 2D 垂直俯视；可右键拖成 45°~70° 斜俯视
    private static float camYaw = 0.0f;
    private static float camDist = 18.0f;  // 滚轮可缩放 6~28 格

    // 用于渲染期反投影鼠标射线
    private static float savedYaw, savedPitch, savedYawO, savedPitchO;
    private static boolean pushedCamRot = false;
    private static Matrix4f lastInvMVP = new Matrix4f();
    private static Vec3 lastCamPos = Vec3.ZERO;
    private static BlockPos hoveredPos = null;
    private static TacticalNet.UnitView hoveredEnemy = null;

    @EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
//        if (TOGGLE_KEY.consumeClick() && Minecraft.getInstance().player != null) {
//            PacketDistributor.sendToServer(new TacticalNet.C2SAction(0, null, BlockPos.ZERO, -1));
//        }
    }

    // 1. 渲染帧前临时把本地视角掰成高空俯视，渲染完立刻还原（不污染真实玩家朝向）
    @SubscribeEvent
    public static void onRenderPre(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!tacticalActive || mc.player == null) return;
        savedYaw = mc.player.getYRot();
        savedPitch = mc.player.getXRot();
        savedYawO = mc.player.yRotO;
        savedPitchO = mc.player.xRotO;
        mc.player.setYRot(camYaw);
        mc.player.yRotO = camYaw;
        mc.player.setXRot(camPitch);
        mc.player.xRotO = camPitch;
        pushedCamRot = true;
    }

    @SubscribeEvent
    public static void onRenderPost(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (pushedCamRot && mc.player != null) {
            mc.player.setYRot(savedYaw);
            mc.player.yRotO = savedYawO;
            mc.player.setXRot(savedPitch);
            mc.player.xRotO = savedPitchO;
            pushedCamRot = false;
        }
    }

    // 2. 拉高第三人称相机距离
    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (tacticalActive) {
            event.setDistance(camDist);
        }
    }

    // 3. 压窄 FOV（长焦消除透视畸变，呈现平直的 2D 战棋视觉）
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (tacticalActive) {
            event.setFOV(28.0);
        }
    }

    // 4. 在真实 3D 世界地面上绘制战棋网格、阵营框，并更新鼠标射线指向
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (!tacticalActive || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TacticalOverlayScreen screen) || mc.level == null) return;

        lastCamPos = event.getCamera().getPosition();
        lastInvMVP = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
        updateHoverTarget(mc, screen);

        TacticalNet.S2CSyncBoard board = screen.board;
        TacticalNet.UnitView active = board.units().stream().filter(TacticalNet.UnitView::active).findFirst().orElse(null);

        PoseStack pose = event.getPoseStack() != null ? event.getPoseStack() : new PoseStack();
        pose.pushPose();
        pose.translate(-lastCamPos.x, -lastCamPos.y, -lastCamPos.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        // 绘制当前我方单位的可移动范围蓝格
        if (active != null && active.team() == 0) {
            int r = Math.min(board.radius(), active.moveRange());
            int refY = active.pos().getY();
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) > active.moveRange()) continue;
                    BlockPos floor = TacticalServer.findFloorAround(mc.level, active.pos().getX() + dx, refY, active.pos().getZ() + dz, 2);
                    if (floor != null) {
                        LevelRenderer.renderLineBox(pose, lines,
                                floor.getX() + 0.04, floor.getY() + 0.02, floor.getZ() + 0.04,
                                floor.getX() + 0.96, floor.getY() + 0.05, floor.getZ() + 0.96,
                                0.25f, 0.55f, 1.0f, 0.75f);
                    }
                }
            }
        }

        // 绘制每个参战单位的脚底阵营框（黄=当前回合，绿=友方，红=敌方）
        for (TacticalNet.UnitView u : board.units()) {
            Entity e = mc.level.getEntity(u.entityId());
            AABB box = e != null
                    ? e.getBoundingBox().inflate(0.08, 0.02, 0.08)
                    : new AABB(u.pos()).inflate(0.1, 0, 0.1);
            float cr = u.active() ? 1.0f : (u.team() == 0 ? 0.2f : 1.0f);
            float cg = u.active() ? 0.85f : (u.team() == 0 ? 0.95f : 0.2f);
            float cb = u.active() ? 0.15f : 0.2f;
            LevelRenderer.renderLineBox(pose, lines, box, cr, cg, cb, 0.95f);
        }

        // 高亮鼠标悬停的目标格子
        if (hoveredPos != null) {
            LevelRenderer.renderLineBox(pose, lines,
                    hoveredPos.getX(), hoveredPos.getY() + 0.03, hoveredPos.getZ(),
                    hoveredPos.getX() + 1.0, hoveredPos.getY() + 0.12, hoveredPos.getZ() + 1.0,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static void updateHoverTarget(Minecraft mc, TacticalOverlayScreen screen) {
        if (mc.level == null || mc.player == null || screen.width <= 0 || screen.height <= 0) return;
        double mx = mc.mouseHandler.xpos() * screen.width / Math.max(1, mc.getWindow().getScreenWidth());
        double my = mc.mouseHandler.ypos() * screen.height / Math.max(1, mc.getWindow().getScreenHeight());

        float ndcX = (float) ((mx / screen.width) * 2.0 - 1.0);
        float ndcY = (float) (1.0 - (my / screen.height) * 2.0);

        Vector4f far = new Vector4f(ndcX, ndcY, 1.0f, 1.0f);
        lastInvMVP.transform(far);
        if (far.w != 0) far.div(far.w);

        Vec3 dir = new Vec3(far.x, far.y, far.z).normalize();
        Vec3 start = lastCamPos;
        Vec3 end = start.add(dir.scale(64.0));

        // 1. 先检测鼠标是否直接点中了敌方实体碰撞箱
        TacticalNet.UnitView bestEnemy = null;
        double bestDist = Double.MAX_VALUE;
        for (TacticalNet.UnitView u : screen.board.units()) {
            if (u.team() != 1) continue;
            Entity e = mc.level.getEntity(u.entityId());
            if (e == null) continue;
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.25).clip(start, end);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(start);
                if (d < bestDist) {
                    bestDist = d;
                    bestEnemy = u;
                }
            }
        }
        hoveredEnemy = bestEnemy;

        // 2. 再检测地面方块
        BlockHitResult bhr = mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (bhr.getType() == HitResult.Type.BLOCK) {
            hoveredPos = bhr.getDirection() == Direction.UP ? bhr.getBlockPos().above() : bhr.getBlockPos();
        } else {
            hoveredPos = null;
        }
    }

    public static void handleSync(TacticalNet.S2CSyncBoard data) {
        Minecraft mc = Minecraft.getInstance();
        if (!data.open()) {
            exitCameraMode(mc);
            if (mc.screen instanceof TacticalOverlayScreen) mc.setScreen(null);
            return;
        }
        if (!tacticalActive) {
            tacticalActive = true;
            savedCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (mc.player != null) camYaw = mc.player.getYRot();
        }
        if (mc.screen instanceof TacticalOverlayScreen screen) {
            screen.board = data;
        } else {
            mc.setScreen(new TacticalOverlayScreen(data));
        }
    }

    private static void exitCameraMode(Minecraft mc) {
        if (tacticalActive) {
            tacticalActive = false;
            mc.options.setCameraType(savedCameraType);
            hoveredPos = null;
            hoveredEnemy = null;
        }
    }

    /** 完全透明的战棋交互层：释放鼠标光标 + 顶部行动条，不遮挡真实世界 */
    public static class TacticalOverlayScreen extends Screen {
        TacticalNet.S2CSyncBoard board;

        protected TacticalOverlayScreen(TacticalNet.S2CSyncBoard board) {
            super(Component.literal("Tactical View"));
            this.board = board;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.literal("结束回合"), b -> {
                if (board.currentTurnUnit() != null) {
                    PacketDistributor.sendToServer(new TacticalNet.C2SAction(3, board.currentTurnUnit(), BlockPos.ZERO, -1));
                }
            }).bounds(width - 95, height - 30, 85, 20).build());
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            // 留空：完全透出真实 3D 俯视世界
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            TacticalNet.UnitView active = board.units().stream().filter(TacticalNet.UnitView::active).findFirst().orElse(null);
            String title = active != null
                    ? "§e当前回合: §f" + active.name() + "  |  §bAP: " + active.ap() + "  |  §aHP: " + (int) active.hp() + "/" + (int) active.maxHp()
                    : "等待敌方行动...";
            g.fill(width / 2 - 140, 6, width / 2 + 140, 24, 0x99000000);
            g.drawCenteredString(font, title, width / 2, 11, 0xFFFFFF);
            g.drawString(font, "左键: 移动/攻击 | 右键拖拽: 调俯仰角 | 滚轮: 缩放 | Esc: 退出", 8, height - 18, 0xCCFFFFFF);

            if (hoveredEnemy != null) {
                g.renderTooltip(font, Component.literal("§c" + hoveredEnemy.name() + " §fHP: " + (int) hoveredEnemy.hp() + "/" + (int) hoveredEnemy.maxHp()), mouseX, mouseY);
            }
            super.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) return true;
            if (button != 0) return false;

            TacticalNet.UnitView active = board.units().stream().filter(TacticalNet.UnitView::active).findFirst().orElse(null);
            if (active == null || active.team() != 0) return false;

            if (hoveredEnemy != null) {
                PacketDistributor.sendToServer(new TacticalNet.C2SAction(2, active.uuid(), hoveredEnemy.pos(), hoveredEnemy.entityId()));
                return true;
            }
            if (hoveredPos != null) {
                Optional<TacticalNet.UnitView> tileEnemy = board.units().stream()
                        .filter(u -> u.team() == 1 && u.pos().getX() == hoveredPos.getX() && u.pos().getZ() == hoveredPos.getZ())
                        .findFirst();
                if (tileEnemy.isPresent()) {
                    PacketDistributor.sendToServer(new TacticalNet.C2SAction(2, active.uuid(), hoveredPos, tileEnemy.get().entityId()));
                } else {
                    PacketDistributor.sendToServer(new TacticalNet.C2SAction(1, active.uuid(), hoveredPos, -1));
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 1) { // 右键拖拽随时切 2D 垂直俯视(89°) <-> 2.5D 斜俯视(45°)
                camYaw += (float) dragX * 0.6f;
                camPitch = Mth.clamp(camPitch + (float) dragY * 0.5f, 35.0f, 89.5f);
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            camDist = Mth.clamp(camDist - (float) scrollY * 1.5f, 6.0f, 28.0f);
            return true;
        }

        @Override
        public void onClose() {
            exitCameraMode(Minecraft.getInstance());
            PacketDistributor.sendToServer(new TacticalNet.C2SAction(0, null, BlockPos.ZERO, -1));
            super.onClose();
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }
}