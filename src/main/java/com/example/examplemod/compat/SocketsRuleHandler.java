package com.example.examplemod.compat;

import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.event.GetItemSocketsEvent;
import dev.xkmc.l2artifacts.content.core.RankedItem; // 莱特兰遗物基类（若已有依赖）
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "examplemod")
public class SocketsRuleHandler {

    @SubscribeEvent
    public static void onGetItemSockets(GetItemSocketsEvent event) {
        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;

        // 1. 【L2 遗物】：按 Rank 限制最大孔数
        if (stack.getItem() instanceof RankedItem artifact) {
            int rank = artifact.rank; // 或从组件读取 rank
            int max = (rank == 5) ? 3 : (rank == 4) ? 2 : (rank >= 2) ? 1 : 0;
            if (event.getSockets() > max) {
                event.setSockets(max);
            }
            return;
        }

        // 2. 【普通 Curios 饰品】：最多 1 孔
        if (CurioMimicHelper.isCurio(stack) && event.getSockets() > 1) {
            event.setSockets(1);
        }
    }
}