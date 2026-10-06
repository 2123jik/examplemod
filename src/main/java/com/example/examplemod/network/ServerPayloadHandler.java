package com.example.examplemod.network;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class ServerPayloadHandler {

    public static void handle(SwapEquipmentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            EquipmentSlot slot = payload.slot();
            AbstractContainerMenu menu = player.containerMenu;
            ItemStack carried = menu.getCarried();               // 玩家鼠标光标抓着的物品
            ItemStack equipped = player.getItemBySlot(slot);     // 目标槽位当前已有的物品

            boolean changed = false;

            // 情况 1：光标为空，装备槽有装备 -> 取下装备到鼠标光标
            if (carried.isEmpty()) {
                if (!equipped.isEmpty()) {
                    menu.setCarried(equipped.copy());
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                }
            }
            // 情况 2：光标有物品 -> 判断是否能穿入该槽位
            else if (canEquipInSlot(player, slot, carried)) {
                // 如果装备槽是空的，直接放入 1 个物品
                if (equipped.isEmpty()) {
                    ItemStack toEquip = carried.split(1);
                    player.setItemSlot(slot, toEquip);
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                }
                // 如果装备槽有装备，且光标刚好只有 1 件物品 -> 直接对调互换
                else if (carried.getCount() == 1) {
                    player.setItemSlot(slot, carried.copy());
                    menu.setCarried(equipped.copy());
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                }
            }

            if (changed) {
                // 1. 递增状态ID，防止客户端与服务端交易版本不匹配导致点击失效
                menu.incrementStateId();

                // 2. 同步鼠标光标拿取的物品给客户端
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        ClientboundContainerSetSlotPacket.CARRIED_ITEM,
                        menu.getStateId(),
                        -1,
                        menu.getCarried()
                ));

                // 3. 强制同步当前玩家自身的装备槽，让左侧自定义面板立即更新渲染
                player.connection.send(new ClientboundSetEquipmentPacket(
                        player.getId(),
                        List.of(Pair.of(slot, player.getItemBySlot(slot)))
                ));

                // 4. 广播常规槽位变更，并全量刷新一次容器数据，彻底杜绝背包出现幽灵物品
                menu.broadcastChanges();
                menu.sendAllDataToRemote();
            }
        });
    }

    /**
     * 校验物品是否满足槽位穿戴要求
     */
    private static boolean canEquipInSlot(ServerPlayer player, EquipmentSlot slot, ItemStack carried) {
        // 主手与副手允许放置任何物品
        if (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.MAINHAND) {
            return true;
        }
        // 盔甲槽必须符合原版装备槽规则（头/胸/腿/脚）
        return player.getEquipmentSlotForItem(carried) == slot;
    }
}