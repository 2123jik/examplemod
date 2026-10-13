package com.example.examplemod.event;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@EventBusSubscriber // 替换为你的 MOD ID
public class EndDimensionListener {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // 判断目标维度是否是末地，且必须是服务端玩家
        if (event.getTo().equals(Level.END) && event.getEntity() instanceof ServerPlayer player) {
            
            // 1. 给予漂浮效果 (持续 3 分钟 = 3 * 60 * 20 = 3600 ticks)
            // 参数: 效果, 时长(ticks), 等级(0代表I级), 是否环境效果, 是否显示粒子, 是否显示图标
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 3600, 0, false, true, true));

            // 2. 给予一桶牛奶
            ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
            giveItemOrStore(player, milkBucket);
        }
    }

    /**
     * 智能物品存放逻辑：本体背包 -> 模组储存物品 -> 末影箱 -> 掉落脚下
     */
    private static void giveItemOrStore(ServerPlayer player, ItemStack itemToGive) {
        // Step 1: 尝试直接塞入原版背包 (快捷栏/主背包)
        if (player.getInventory().add(itemToGive)) {
            return; // 成功完全塞入
        }

        // Step 2: 如果背包满了，检查身上是否携带了模组储存容器 (背包/袋子等)
        // 遍历玩家背包中所有物品，检查是否带有 NeoForge 的 ItemHandler 能力
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            // NeoForge 1.21.1 查询物品是否具备 ItemHandler
            IItemHandler itemStorage = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (itemStorage != null) {
                // 尝试塞入模组背包，返回未能塞入的剩余物品
                itemToGive = ItemHandlerHelper.insertItem(itemStorage, itemToGive, false);
                if (itemToGive.isEmpty()) {
                    return; // 模组背包完全接纳
                }
            }
        }

        // Step 3: 如果模组背包依然塞不下或没有模组背包，尝试放入末影箱
        IItemHandler enderChestHandler = new InvWrapper(player.getEnderChestInventory());
        itemToGive = ItemHandlerHelper.insertItem(enderChestHandler, itemToGive, false);
        if (itemToGive.isEmpty()) {
            return;
        }

        // Step 4: 终极兜底方案，如果所有空间都满了，掉落在玩家脚下
        if (!itemToGive.isEmpty()) {
            player.drop(itemToGive, false);
        }
    }
}