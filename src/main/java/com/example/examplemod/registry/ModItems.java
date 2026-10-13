package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MOD_ID);

    // 方块物品
    public static final DeferredHolder<Item, BlockItem> CABINET_ITEM =
            ITEMS.register("equipment_cabinet", () -> new BlockItem(ModBlocks.CABINET.get(), new Item.Properties()));

    // 功能物品
    public static final DeferredHolder<Item, Item> PORTABLE_STONECUTTER =
            ITEMS.register("portable_stonecutter", () -> new PortableStonecutterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_ANVIL =
            ITEMS.register("portable_anvil", () -> new PortableAnvilItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_FURNACE =
            ITEMS.register("portable_furnace", () -> new PortableFurnaceItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_BREWING_STAND =
            ITEMS.register("portable_brewing_stand", () -> new PortableBrewingStandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<TurtleShieldItem> TURTLE_SHIELD =
            ITEMS.register("turtle_shield", () -> new TurtleShieldItem(new Item.Properties().durability(336)));
    public static final DeferredHolder<Item, Item> GLASS_OVERLAY_ITEM =
            ITEMS.register("glass_overlay_item", () -> new Item(new Item.Properties()));

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TURTLE_SHIELD);
        }
    }
}