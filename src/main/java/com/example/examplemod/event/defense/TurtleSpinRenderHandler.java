package com.example.examplemod.event.defense;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class TurtleSpinRenderHandler {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();

        if (ClientDefenseManager.isEntitySpinDefending(entity.getId())) {
            PoseStack poseStack = event.getPoseStack();
            float halfHeight = entity.getBbHeight() / 2.0F;

            // 1. 移动到生物中心
            poseStack.translate(0.0F, halfHeight, 0.0F);

            // 2. 向前翻折 90 度，使龟壳直面敌人
            // (如果发现朝向反了，可以修改为 -90.0F)
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

            // 3. 绕壳中心高速自转
            float partialTick = event.getPartialTick();
            float spinSpeed = 35.0F;
            float spinAngle = (entity.tickCount + partialTick) * spinSpeed;
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinAngle));

            // 4. 还原平移中心
            poseStack.translate(0.0F, -halfHeight, 0.0F);
        }
    }
}