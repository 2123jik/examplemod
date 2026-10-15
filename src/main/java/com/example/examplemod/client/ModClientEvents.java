//package com.example.examplemod.client;
//
//import com.example.examplemod.ExampleMod;
//import com.example.examplemod.util.GameStartupIconGenerator;
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Camera;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.Mob;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//
//import java.util.List;
//
//import static com.example.examplemod.client.layer.MobOverheadRenderer.OVERHEAD_PANEL;
//
//@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
//public class ModClientEvents {
//
//    @SubscribeEvent
//    public static void onClientReload(RegisterClientReloadListenersEvent event) {
////        // 进入游戏或客户端重载资源时，自动生成真实的物理 PNG 文件
////        event.registerReloadListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
////                stage.wait(null).thenRunAsync(GameStartupIconGenerator::generateTexturesOnStartup, gameExecutor)
////        );
//    }
//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        // 选择阶段：在半透明方块（水、染色玻璃）全部绘制完毕后绘制！
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
//            return;
//        }
//
//        Minecraft mc = Minecraft.getInstance();
//        ClientLevel level = mc.level;
//        if (level == null || mc.player == null) {
//            return;
//        }
//
//        Camera camera = event.getCamera();
//        Vec3 camPos = camera.getPosition();
//        float partialTick = event.getPartialTick();
//
//        // 1. 获取实体：根据视距构建空间包围盒检索附近的 Mob
//        double range = 24.0D;
//        AABB box = new AABB(
//                camPos.x - range, camPos.y - range, camPos.z - range,
//                camPos.x + range, camPos.y + range, camPos.z + range
//        );
//
//        List<Mob> nearbyMobs = level.getEntitiesOfClass(
//                Mob.class,
//                box,
//                mob -> mob.isAlive() && !mob.isInvisibleTo(mc.player)
//        );
//
//        if (nearbyMobs.isEmpty()) {
//            return;
//        }
//
//        PoseStack pose = event.getPoseStack();
//        // 获取当前客户端主缓冲源
//        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
//
//        for (Mob entity : nearbyMobs) {
//            // 2. 世界坐标转换为相对摄像机的相对坐标
//            double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camPos.x;
//            double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camPos.y;
//            double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camPos.z;
//
//            pose.pushPose();
//            // 将原点移动到实体脚底
//            pose.translate(entityX, entityY, entityZ);
//
//            // 3. 执行你的原版绘制逻辑（传入 pose, bufferSource 等）
//            MobOverheadRenderer.renderMobOverhead(entity, partialTick, pose, bufferSource);
//
//            pose.popPose();
//        }
//
//        // 4. 关键：在事件结束时手动冲刷自定义缓冲，确保立即绘制
//        bufferSource.endBatch(OVERHEAD_PANEL);
//        bufferSource.endBatch(); // 冲刷文字缓冲区
//    }
//}