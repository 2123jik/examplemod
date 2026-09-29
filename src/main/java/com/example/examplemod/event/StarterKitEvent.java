package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.ItemGiveHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class StarterKitEvent {

    private static final String GIVEN_TAG = "has_received_starter_kit";

    private static final List<String> STARTER_ITEMS = List.of(
            "examplemod:portable_furnace",
            "minecraft:crafting_table",
            "minecraft:smithing_table",
            "minecraft:enchanting_table",
            "apotheosis:salvaging_table",
            "apotheosis:gem_cutting_table",
            "apotheosis:simple_reforging_table",
            "irons_spellbooks:diamond_spell_book",
            "irons_spellbooks:inscription_table",
            "irons_spellbooks:scroll_forge",
            "l2backpack:dimensional_storage_white",
            "l2backpack:ender_backpack",
            "l2backpack:pickup_tweaker_tool",
            "l2backpack:gem_bag",
            "l2complements:eternal_anvil",
            "l2weaponry:shulkerate_round_shield",
            "irons_spellbooks:wizard_hat",
            "irons_spellbooks:wizard_chestplate",
            "irons_spellbooks:wizard_leggings",
            "irons_spellbooks:wizard_boots",
            "irons_spellbooks:artificer_cane",
            "l2archery:starter_bow",
            "l2backpack:backpack_white",
            "l2weaponry:shulkerate_hammer",
            "l2weaponry:shulkerate_battle_axe",
            "l2hostility:detector_glasses"
    );

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 1. 修复刷物 Bug：必须将数据保存在 Player.PERSISTED_NBT_TAG 子标签中，才能在玩家死亡重生后继续保留
            CompoundTag playerData = player.getPersistentData();
            CompoundTag persistedData = playerData.getCompound(Player.PERSISTED_NBT_TAG);

            if (!persistedData.getBoolean(GIVEN_TAG)) {
                // 先标记为已领取，避免中途出错重复领取
                persistedData.putBoolean(GIVEN_TAG, true);
                playerData.put(Player.PERSISTED_NBT_TAG, persistedData);

                // 2. 遍历发物并进行安全性过滤
                for (String itemId : STARTER_ITEMS) {
                    ResourceLocation id = ResourceLocation.tryParse(itemId);
                    if (id == null) continue;

                    // 检查物品是否真实注册，避免模组未安装或拼写错误导致异常/空物品
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) {
                        continue; // 模组缺失时跳过，不崩溃也不发空气
                    }

                    ItemGiveHelper.giveItemById(player, itemId, 1);
                }

                // 3. 强制通知客户端刷新背包，防止幽灵物品
                player.containerMenu.broadcastChanges();
            }
        }
    }
}