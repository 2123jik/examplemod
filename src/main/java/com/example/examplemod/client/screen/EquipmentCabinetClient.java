// EquipmentCabinetClient.java
package com.example.examplemod.client.screen;

import com.example.examplemod.ExampleMod;
import icyllis.modernui.mc.neoforge.MenuScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT
)
public final class EquipmentCabinetClient {
    private EquipmentCabinetClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ExampleMod.CABINET_MENU.get(),
                MenuScreenFactory.create(EquipmentCabinetFragment::new)
        );
    }
}
