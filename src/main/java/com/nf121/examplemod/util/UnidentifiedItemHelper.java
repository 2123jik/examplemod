package com.nf121.examplemod.util;

import com.nf121.examplemod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class UnidentifiedItemHelper {
    private UnidentifiedItemHelper() {}

    private static final Component MSG_FULL =
            Component.translatable("message.examplemod.inventory_full")
                    .withStyle(ChatFormatting.RED);

    public static boolean deliver(
            Player player,
            InteractionHand hand,
            ItemStack heldStack,
            ItemStack identifiedStack,
            int cooldownTicks,
            SoundEvent sound,
            float volume,
            float pitch,
            String successTranslationKey
    ) {
        if (player.level().isClientSide
                || heldStack.isEmpty()
                || identifiedStack.isEmpty()) {
            return false;
        }

        // 此处假定 ALREADY_IDENTIFIED 注册的数据类型为 Unit。
        identifiedStack.set(ModDataComponents.ALREADY_IDENTIFIED.get(), true);

        // 清除结果物品上的未鉴定标记。
        identifiedStack.remove(ModDataComponents.IS_UNIDENTIFIED.get());

        // 背包插入操作会修改 identifiedStack，提前保存后续需要的信息。
        Component identifiedName = identifiedStack.getHoverName().copy();

        boolean replaceHand =
                !player.isCreative() && heldStack.getCount() == 1;

        Item cooldownItem = replaceHand
                ? identifiedStack.getItem()
                : heldStack.getItem();

        // 消耗未鉴定物品。
        if (!player.isCreative()) {
            heldStack.shrink(1);
        }

        // 优先放回腾空的手位，否则放进背包，剩余部分掉落。
        if (replaceHand) {
            player.setItemInHand(hand, identifiedStack);
        } else {
            player.getInventory().add(identifiedStack);

            if (!identifiedStack.isEmpty()) {
                player.drop(identifiedStack, false);
            }
        }

        player.getCooldowns().addCooldown(cooldownItem, cooldownTicks);

        if (player instanceof ServerPlayer sp) {
            sp.containerMenu.broadcastChanges();
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch
        );

        player.sendSystemMessage(
                Component.translatable(successTranslationKey)
                        .withStyle(ChatFormatting.GOLD)
                        .append(" ")
                        .append(identifiedName)
        );

        return true;
    }

    public static boolean hasRoomForIdentify(
            Player player,
            ItemStack heldStack
    ) {
        if (heldStack.isEmpty()) {
            return false;
        }

        // 非创造模式消耗最后一个物品后，可以直接回填手位。
        if (!player.isCreative() && heldStack.getCount() == 1) {
            return true;
        }

        return player.getInventory().getFreeSlot() != -1;
    }

    public static Component fullMessage() {
        return MSG_FULL;
    }
}
