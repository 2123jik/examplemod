package com.example.examplemod.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;

@EventBusSubscriber(modid = AffixCodexBlockTrigger.MOD_ID)
public final class AffixCodexBlockTrigger {

    public static final String MOD_ID = "examplemod";

    /**
     * 绑定的原版方块集合（想增加或更换方块直接改这里）
     */
    private static final Set<Block> TARGET_BLOCKS = Set.of(
            Blocks.NETHERITE_BLOCK
    );

    /**
     * 可选：按注册 ID 绑定其他模组的方块（无需 import 对应模组类）
     * 例如: ResourceLocation.parse("apotheosis:reforging_table")
     */
    private static final Set<ResourceLocation> TARGET_BLOCK_IDS = Set.of(
            // ResourceLocation.parse("apotheosis:simple_reforging_table")
    );

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 只处理主手，避免副手重复触发两次
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        // 潜行时放行原版放置/交互逻辑，方便贴着方块搭建筑
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!isTargetBlock(block)) {
            return;
        }

        // 拦截默认右键行为（防止手中物品被使用或放置），并触发挥手动画
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        // 仅在物理客户端 + 逻辑客户端线程打开图鉴界面
        if (FMLEnvironment.dist == Dist.CLIENT && event.getLevel().isClientSide()) {
            ClientOnly.openCodex();
        }
    }

    private static boolean isTargetBlock(Block block) {
        if (TARGET_BLOCKS.contains(block)) {
            return true;
        }
        if (!TARGET_BLOCK_IDS.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return TARGET_BLOCK_IDS.contains(id);
        }
        return false;
    }

    private static final class ClientOnly {
        private static void openCodex() {
            // 传入 null 作为父界面：按 ESC 或点“返回”时会直接回到游戏画面
            Minecraft.getInstance().setScreen(new AffixCodexScreen(null));
        }
    }
}