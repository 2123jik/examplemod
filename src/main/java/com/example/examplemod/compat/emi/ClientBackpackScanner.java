//package com.example.examplemod.compat.emi;
//
//import dev.emi.emi.api.stack.EmiStack;
//import dev.xkmc.l2backpack.compat.CuriosCompat;
//import dev.xkmc.l2backpack.content.bag.AbstractBag;
//import dev.xkmc.l2backpack.content.common.BaseBagItem;
//import dev.xkmc.l2backpack.content.remote.dimensional.DimensionalItem;
//import dev.xkmc.l2backpack.content.remote.player.EnderBackpackItem;
//import dev.xkmc.l2backpack.init.registrate.LBItems;
//import net.minecraft.client.Minecraft;
//import net.minecraft.core.NonNullList;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.component.ItemContainerContents;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ClientBackpackScanner {
//
//    /**
//     * 客户端扫描玩家背饰背包及其内部所有嵌套背包的物品列表
//     */
//    public static List<EmiStack> scanCuriosBackpackContents() {
//        List<EmiStack> list = new ArrayList<>();
//        Player player = Minecraft.getInstance().player;
//        if (player == null) return list;
//
//        CuriosCompat.getSlot(player, ClientBackpackScanner::isContainer).ifPresent(pair -> {
//            ItemStack root = pair.getFirst();
//            scanRecursive(root, list, player, 0);
//        });
//
//        return list;
//    }
//
//    private static void scanRecursive(ItemStack stack, List<EmiStack> list, Player player, int depth) {
//        if (depth > 5 || stack.isEmpty()) return;
//
//        // 1. 普通背包/功能包：直接读取 1.20.5+ 的 DataComponent (ItemContainerContents)
//        ItemContainerContents contents = stack.get(LBItems.BACKPACK_CONTENT.get());
//        if (contents == null) {
//            contents = stack.get(LBItems.BAG_CONTENT.get());
//        }
//        if (contents != null) {
//            for (ItemStack sub : contents.nonEmptyStream().toList()) {
//                if (isContainer(sub)) {
//                    scanRecursive(sub, list, player, depth + 1);
//                } else {
//                    list.add(EmiStack.of(sub));
//                }
//            }
//            return;
//        }
//
//        // 2. 末影背包：客户端读取同步过来的末影箱库存
//        if (stack.getItem() instanceof EnderBackpackItem) {
//            for (ItemStack sub : player.getEnderChestInventory().getItems()) {
//                if (isContainer(sub)) {
//                    scanRecursive(sub, list, player, depth + 1);
//                } else if (!sub.isEmpty()) {
//                    list.add(EmiStack.of(sub));
//                }
//            }
//        }
//
//        // 3. 空间背包：如果客户端缓存有数据也可以遍历
//    }
//
//    private static boolean isContainer(ItemStack stack) {
//        return stack.getItem() instanceof BaseBagItem
//            || stack.getItem() instanceof AbstractBag
//            || stack.getItem() instanceof EnderBackpackItem
//            || stack.getItem() instanceof DimensionalItem;
//    }
//}