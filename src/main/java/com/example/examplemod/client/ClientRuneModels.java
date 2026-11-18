package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientRuneModels {

    private static final List<String> RUNE_PATHS = List.of(
            "runes/rune_01_el", "runes/rune_02_eld", "runes/rune_03_tir", "runes/rune_04_nef",
            "runes/rune_05_eth", "runes/rune_06_ith", "runes/rune_07_tal", "runes/rune_08_ral",
            "runes/rune_09_ort", "runes/rune_10_thul", "runes/rune_11_amn", "runes/rune_12_sol",
            "runes/rune_13_shael", "runes/rune_14_dol", "runes/rune_15_hel", "runes/rune_16_io",
            "runes/rune_17_lum", "runes/rune_18_ko", "runes/rune_19_fal", "runes/rune_20_lem",
            "runes/rune_21_pul", "runes/rune_22_um", "runes/rune_23_mal", "runes/rune_24_ist",
            "runes/rune_25_gul", "runes/rune_26_vex", "runes/rune_27_ohm", "runes/rune_28_lo",
            "runes/rune_29_sur", "runes/rune_30_ber", "runes/rune_31_jah", "runes/rune_32_cham",
            "runes/rune_33_zod",
            "d2_gems/d2_sapphire", "d2_gems/d2_emerald", "d2_gems/d2_topaz",
            "d2_gems/d2_ruby", "d2_gems/d2_amethyst", "d2_gems/d2_diamond", "d2_gems/d2_skull"
    );

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (String path : RUNE_PATHS) {
            event.register(ModelResourceLocation.standalone(Apotheosis.loc("item/gems/" + path)));
        }
    }
}