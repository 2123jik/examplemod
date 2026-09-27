package com.example.examplemod.compat;

import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.event.GetItemSocketsEvent;
import dev.xkmc.l2artifacts.content.core.RankedItem; 
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "examplemod")
public class SocketsRuleHandler {

    @SubscribeEvent
    public static void onGetItemSockets(GetItemSocketsEvent event) {
        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;


        if (stack.getItem() instanceof RankedItem artifact) {
            int rank = artifact.rank; 
            int max = (rank == 5) ? 3 : (rank == 4) ? 2 : (rank >= 2) ? 1 : 0;
            if (event.getSockets() > max) {
                event.setSockets(max);
            }
            return;
        }


        if (CurioMimicHelper.isCurio(stack) && event.getSockets() > 1) {
            event.setSockets(1);
        }
    }
}