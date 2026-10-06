package com.example.examplemod.util;

import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class UnidentifiedItemHelper {
    private UnidentifiedItemHelper() {}

    private static final Component MSG_FULL =
            Component.translatable("message.examplemod.inventory_full").withStyle(ChatFormatting.RED);

    public static boolean deliver(
            Player player,
            InteractionHand hand,
            ItemStack heldStack,
            ItemStack identifiedStack,
            int cooldownTicks,
            SoundEvent sound,
            float volume, float pitch,
            String successTranslationKey
    ) {
        // 关键防护：给生成的新物品打上【已鉴定】标记，防止扔地上或进漏斗二次识别
        identifiedStack.set(ModDataComponents.ALREADY_IDENTIFIED, true);

        // 1. 消耗手中的未鉴定物品
        if (!player.isCreative()) {
            heldStack.shrink(1);
        }

        // 2. 发放物品：如果手中物品刚好扣空，优先回填手中
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, identifiedStack);
        } else {
            // 否则尝试塞入背包，塞不进就掉落在脚下
            if (!player.getInventory().add(identifiedStack)) {
                player.drop(identifiedStack, false);
            }
        }

        // 3. 网络同步与 CD
        if (player instanceof ServerPlayer sp) {
            sp.containerMenu.broadcastChanges();
        }

        player.getCooldowns().addCooldown(
                heldStack.isEmpty() ? identifiedStack.getItem() : heldStack.getItem(),
                cooldownTicks
        );

        // 4. 音效与成功提示
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);

        player.sendSystemMessage(
                Component.translatable(successTranslationKey).withStyle(ChatFormatting.GOLD)
                        .append(" ")
                        .append(identifiedStack.getHoverName())
        );

        return true;
    }

    public static boolean hasRoomForIdentify(Player player, ItemStack heldStack) {
        // 只有 1 个时，消耗后手位就空了，一定有空间放
        if (heldStack.getCount() == 1) return true;
        // 还有多余时，背包必须有空位（或者可以用 getFreeSlot 检查）
        return player.getInventory().getFreeSlot() != -1;
    }

    public static Component fullMessage() {
        return MSG_FULL;
    }
}