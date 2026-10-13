package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ExampleMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentCabinetMenu>> CABINET_MENU =
            MENUS.register("equipment_cabinet", () -> IMenuTypeExtension.create(EquipmentCabinetMenu::new));
}