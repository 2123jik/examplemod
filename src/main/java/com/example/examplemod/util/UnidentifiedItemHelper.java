package com.example.examplemod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class UnidentifiedItemHelper {
    private UnidentifiedItemHelper() {}

    /** 背包已满时给出的提示（两处共用一份翻译键/文案） */
    private static final Component MSG_FULL =
            Component.literal("背包已满，无法鉴定！请腾出空间后再试。")
                    .withStyle(ChatFormatting.RED);

    /**
     * 把已鉴定物交付给玩家。
     * 前置：调用方必须先判过背包是否够用（见 {@link #hasRoomForIdentify}）。
     *
     * @return false 表示前置条件不满足（背包满且手持不止一个），调用方应 fail。
     */
    public static boolean deliver(
            Player player,
            InteractionHand hand,
            ItemStack heldStack,
            ItemStack identifiedStack,
            int cooldownTicks,
            SoundEvent sound,
            float volume, float pitch,
            String successLabel
    ) {
        // 1. 扣除原物（创造模式不扣）
        if (!player.isCreative()) {
            heldStack.shrink(1);
        }

        // 2. 优先合并到同类堆叠；其次找空格；都跳过"当前交互槽"
        boolean inserted = insertIntoInventory(player, hand, identifiedStack);

        // 3. 兜底：手持槽腾出来了就回手，否则掉地
        if (!inserted) {
            if (heldStack.isEmpty()) {
                player.setItemInHand(hand, identifiedStack);
            } else {
                player.drop(identifiedStack, false);
            }
        }

        // 4. 双端同步，防幽灵物品
        if (player instanceof ServerPlayer sp) {
            sp.containerMenu.broadcastChanges();
        }

        // 5. 底层冷却锁
        player.getCooldowns().addCooldown(heldStack.isEmpty()
                ? identifiedStack.getItem() : heldStack.getItem(), cooldownTicks);

        // 6. 音效 + 消息
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
        player.sendSystemMessage(
                Component.literal(successLabel).withStyle(ChatFormatting.GOLD)
                        .append(identifiedStack.getHoverName()));

        return true;
    }

    /** 背包是否有空间。手持只有 1 个时总能放下（回手）。 */
    public static boolean hasRoomForIdentify(Player player, ItemStack heldStack) {
        if (heldStack.getCount() == 1) return true;
        return player.getInventory().getFreeSlot() != -1;
    }

    public static Component fullMessage() { return MSG_FULL; }

    // ------------------------------------------------------------------

    private static boolean insertIntoInventory(Player player, InteractionHand hand, ItemStack stack) {
        int skip = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().selected : -1;
        var items = player.getInventory().items;

        // 先合并
        for (int i = 0; i < items.size(); i++) {
            if (i == skip) continue;
            ItemStack s = items.get(i);
            if (!s.isEmpty()
                    && ItemStack.isSameItemSameComponents(s, stack)
                    && s.getCount() < s.getMaxStackSize()) {
                s.grow(1);
                return true;
            }
        }
        // 再找空
        for (int i = 0; i < items.size(); i++) {
            if (i == skip) continue;
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                return true;
            }
        }
        return false;
    }
}