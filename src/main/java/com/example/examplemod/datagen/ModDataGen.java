package com.example.examplemod.datagen;

import com.example.examplemod.ExampleMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModDataGen {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var gen = event.getGenerator();
        var out = gen.getPackOutput();
        var helper = event.getExistingFileHelper();

        if (event.includeClient()) {
            gen.addProvider(true, new SpellTraitItemModels(out, helper));
        }
    }
}