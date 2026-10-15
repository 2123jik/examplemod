package com.nf121.examplemod.compat.apothic;

import dev.shadowsoffire.apotheosis.event.CanSocketGemEvent;
import dev.shadowsoffire.apotheosis.event.GetItemSocketsEvent;
import dev.shadowsoffire.apotheosis.event.ItemSocketingEvent;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.SocketedGems;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class CurioSocketEventHandler {

    /**
     * 直接通过 Tag 判断：
     * 只要物品带有任意 namespace 为 "curios" 的标签（如 curios:ring, curios:charm, curios:curio 等），
     * 就认定为饰品。
     */
    public static boolean isCurio(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getTags().anyMatch(tag -> "curios".equals(tag.location().getNamespace()));
    }

    /**
     * 判断饰品是否已经镶嵌了至少 1 颗有效宝石
     */
    public static boolean hasAnyGem(ItemStack stack) {
        SocketedGems socketedGems = SocketHelper.getGems(stack);
        for (GemInstance inst : socketedGems) {
            if (inst != null && inst.isValid() && !inst.gemStack().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGetItemSockets(GetItemSocketsEvent event) {
        ItemStack stack = event.getStack();
        if (isCurio(stack)) {
            if (event.getSockets() > 1) {
                event.setSockets(1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCanSocketGem(CanSocketGemEvent event) {
        ItemStack stack = event.getInputStack();
        if (isCurio(stack) && hasAnyGem(stack)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemSocketing(ItemSocketingEvent event) {
        ItemStack stack = event.getInputStack();
        if (isCurio(stack) && hasAnyGem(stack)) {
            event.setOutput(stack);
        }
    }
}