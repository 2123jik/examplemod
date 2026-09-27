package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.ItemGiveHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class StarterKitEvent {

    private static final String GIVEN_TAG = "examplemod:has_received_portable_tools";


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
            if (!player.getPersistentData().getBoolean(GIVEN_TAG)) {

                for (String itemId : STARTER_ITEMS) {
                    ItemGiveHelper.giveItemById(player, itemId, 1);
                }

                player.getPersistentData().putBoolean(GIVEN_TAG, true);
            }
        }
    }

}