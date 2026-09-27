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

    
    private static final Component MSG_FULL =
            Component.literal("背包已满，无法鉴定！请腾出空间后再试。")
                    .withStyle(ChatFormatting.RED);

    
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

        if (!player.isCreative()) {
            heldStack.shrink(1);
        }


        boolean inserted = insertIntoInventory(player, hand, identifiedStack);


        if (!inserted) {
            if (heldStack.isEmpty()) {
                player.setItemInHand(hand, identifiedStack);
            } else {
                player.drop(identifiedStack, false);
            }
        }


        if (player instanceof ServerPlayer sp) {
            sp.containerMenu.broadcastChanges();
        }


        player.getCooldowns().addCooldown(heldStack.isEmpty()
                ? identifiedStack.getItem() : heldStack.getItem(), cooldownTicks);


        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
        player.sendSystemMessage(
                Component.literal(successLabel).withStyle(ChatFormatting.GOLD)
                        .append(identifiedStack.getHoverName()));

        return true;
    }

    
    public static boolean hasRoomForIdentify(Player player, ItemStack heldStack) {
        if (heldStack.getCount() == 1) return true;
        return player.getInventory().getFreeSlot() != -1;
    }

    public static Component fullMessage() { return MSG_FULL; }



    private static boolean insertIntoInventory(Player player, InteractionHand hand, ItemStack stack) {
        int skip = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().selected : -1;
        var items = player.getInventory().items;


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