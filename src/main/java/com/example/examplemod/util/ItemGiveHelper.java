package com.example.examplemod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ItemGiveHelper {

    /**
     * 根据物品 ID 给玩家发放物品
     * @param player 目标玩家
     * @param itemId 物品注册名（如 "minecraft:diamond" 或 "examplemod:portable_furnace"）
     * @param count 数量
     * @return 是否发放成功（若 ID 不存在返回 false）
     */
    public static boolean giveItemById(ServerPlayer player, String itemId, int count) {
        // 1. 安全解析命名空间与路径（防止非法字符串崩溃）
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return false;
        }

        // 2. 从原版物品注册表获取 Item
        Item item = BuiltInRegistries.ITEM.get(id);

        // 如果找不到该 ID，原版注册表会默认返回空气 (Items.AIR)
        if (item == Items.AIR) {
            return false;
        }

        // 3. 构建 ItemStack 并发放
        ItemStack stack = new ItemStack(item, count);
        
        // NeoForge 推荐写法：自动优先填入背包槽位，背包满了自动掉在脚边
        ItemHandlerHelper.giveItemToPlayer(player, stack);
        return true;
    }
}