package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);
    public static final DeferredHolder<Item, Item> PORTABLE_STONECUTTER =
            ITEMS.register("portable_stonecutter", () -> new PortableStonecutterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_ANVIL =
            ITEMS.register("portable_anvil", () -> new PortableAnvilItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_FURNACE =
            ITEMS.register("portable_furnace", () -> new PortableFurnaceItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_BREWING_STAND =
            ITEMS.register("portable_brewing_stand", () -> new PortableBrewingStandItem(new Item.Properties().stacksTo(1)));

}
