package com.example.examplemod.mixin.vanilla;
import com.example.examplemod.entity.chest.PlayerMarkedChest;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {

    /**
     * T extends BlockEntity & LidBlockEntity
     *
     * 泛型擦除以后，render 的首个参数类型是 BlockEntity。
     */
    @Inject(
            method =
                    "render(" +
                    "Lnet/minecraft/world/level/block/entity/BlockEntity;" +
                    "F" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lnet/minecraft/client/renderer/MultiBufferSource;" +
                    "II" +
                    ")V",
            at = @At("TAIL")
    )
    private void examplemod$renderMarkedChestOutline(
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo callback
    ) {
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }

        Player localPlayer = Minecraft.getInstance().player;

        if (localPlayer == null) {
            return;
        }

        BlockState state = chest.getBlockState();

        if (!state.hasProperty(ChestBlock.TYPE)) {
            return;
        }

        ChestType chestType = state.getValue(ChestBlock.TYPE);
        UUID playerUuid = localPlayer.getUUID();

        if (chestType == ChestType.SINGLE) {
            if (!examplemod$isMarkedBy(chest, playerUuid)) {
                return;
            }

            examplemod$renderBox(
                    poseStack,
                    bufferSource,
                    new AABB(
                            -0.002D,
                            -0.002D,
                            -0.002D,
                            1.002D,
                            1.002D,
                            1.002D
                    )
            );

            return;
        }

        /*
         * 每个双箱会让 ChestRenderer 分别渲染左右两个 BlockEntity。
         * 固定只让 LEFT 半边绘制外框，避免重复绘制。
         */
        if (chestType != ChestType.LEFT) {
            return;
        }

        Level level = chest.getLevel();

        if (level == null) {
            return;
        }

        /*
         * getConnectedDirection(state) 返回当前半边指向另一半的方向。
         */
        Direction connectedDirection =
                ChestBlock.getConnectedDirection(state);

        BlockPos connectedPos =
                chest.getBlockPos().relative(connectedDirection);

        BlockEntity connectedBlockEntity =
                level.getBlockEntity(connectedPos);

        ChestBlockEntity connectedChest =
                connectedBlockEntity instanceof ChestBlockEntity other
                        ? other
                        : null;

        /*
         * 任意一半存有当前玩家的标记，都显示整个双箱外框。
         *
         * 这样即便只有一半执行过战利品表解包，也不会漏掉显示。
         */
        boolean currentMarked =
                examplemod$isMarkedBy(chest, playerUuid);

        boolean connectedMarked =
                connectedChest != null
                        && examplemod$isMarkedBy(
                                connectedChest,
                                playerUuid
                        );

        if (!currentMarked && !connectedMarked) {
            return;
        }

        int stepX = connectedDirection.getStepX();
        int stepZ = connectedDirection.getStepZ();

        /*
         * AABB 使用当前 LEFT 半箱的局部坐标。
         *
         * 如果另一半在负方向：
         *     X/Z 最小值扩展到 -1
         *
         * 如果另一半在正方向：
         *     X/Z 最大值扩展到 2
         */
        double minX = Math.min(0, stepX) - 0.002D;
        double minZ = Math.min(0, stepZ) - 0.002D;

        double maxX = 1.0D + Math.max(0, stepX) + 0.002D;
        double maxZ = 1.0D + Math.max(0, stepZ) + 0.002D;

        AABB doubleChestBox = new AABB(
                minX,
                -0.002D,
                minZ,
                maxX,
                1.002D,
                maxZ
        );

        examplemod$renderBox(
                poseStack,
                bufferSource,
                doubleChestBox
        );
    }

    private static boolean examplemod$isMarkedBy(
            ChestBlockEntity chest,
            UUID playerUuid
    ) {
        return chest instanceof PlayerMarkedChest markedChest
                && markedChest.examplemod$isMarkedBy(playerUuid);
    }

    private static void examplemod$renderBox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AABB box
    ) {
        VertexConsumer lineConsumer =
                bufferSource.getBuffer(RenderType.lines());

        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                box,
                1.0F,  // 红
                0.2F,  // 绿
                0.05F, // 蓝
                1.0F   // Alpha
        );
    }
}
