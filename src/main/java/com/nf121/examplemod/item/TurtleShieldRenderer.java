package com.nf121.examplemod.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TurtleShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TURTLE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/turtle/big_sea_turtle.png");

    private final TurtleModel<Turtle> turtleModel;

    // 海龟壳中心在模型中的偏移坐标（海龟身体中心大约在 Y=1.2格, Z=0格 处）
    private static final float SHELL_CENTER_Y = -1.25F;
    private static final float SHELL_CENTER_Z = 0.05F;

    public TurtleShieldRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.turtleModel = new TurtleModel<>(modelSet.bakeLayer(ModelLayers.TURTLE));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 1. 判断当前玩家是否在举盾格挡
        LocalPlayer player = Minecraft.getInstance().player;
        boolean isBlocking = false;
        if (player != null && player.isUsingItem()) {
            ItemStack activeStack = player.getUseItem();
            isBlocking = (activeStack == stack) || ItemStack.isSameItemSameComponents(activeStack, stack);
        }

        // 2. 盾牌整体缩放（0.85F 大小适中，既有气势又不会完全遮挡视线）
        float scale = 0.85F;
        poseStack.scale(scale, scale, scale);

        // 3. 将海龟壳立起，让背甲面向正前方（原版盾牌朝向）
        // 此时垂直穿透龟甲的法线轴正是世界 Z 轴
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // 4. 【核心步骤】：自转轴精准穿过龟壳正中心
        if (isBlocking) {
            // 每秒 8 圈的超高速旋转（可自行调大或调小倍数）
            float angle = (System.currentTimeMillis() % 1000L) / 1000.0F * 360.0F * 8.0F;

            // ① 先平移到龟壳中心
            poseStack.translate(0.0F, SHELL_CENTER_Z, SHELL_CENTER_Y);
            // ② 绕垂直穿透龟壳的 Z 轴自转
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
            // ③ 平移回原位
            poseStack.translate(0.0F, -SHELL_CENTER_Z, -SHELL_CENTER_Y);
        }

        // 5. 翻转 Y 轴以适配 Minecraft 实体模型渲染规则，并对齐握持把手位置
        poseStack.scale(1.0F, -1.0F, -1.0F);
        // 让握把稍微靠在龟肚子后方一点
        poseStack.translate(0.0F, 0.0F, -0.15F);

        // 6. 提交海龟模型渲染
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TURTLE_TEXTURE));
        this.turtleModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }
}