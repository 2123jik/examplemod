package com.example.examplemod.compat.l2hostility;

import dev.xkmc.l2hostility.init.registrate.LHTraits;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

@EventBusSubscriber(modid = "examplemod")
public class SpellTraitRegistrar {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Set<String> WHITELIST = Set.of(

            "null",
            "iss_magicfromtheeast:dragon_glide",
            "iss_magicfromtheeast:jade_bullet",
            "iss_magicfromtheeast:jade_judgement",
            "iss_magicfromtheeast:nephrite_slash",
            "iss_magicfromtheeast:sword_dance",
            "iss_magicfromtheeast:underworld_aid",

            "null",
            "irons_spellbooks:cone_of_cold",
            "irons_spellbooks:frostwave",
            "irons_spellbooks:ice_block",
            "irons_spellbooks:ice_spikes",
            "irons_spellbooks:icicle",
            "irons_spellbooks:ray_of_frost",
            "irons_spellbooks:snowball",

            "null",
            "irons_spellbooks:chain_creeper",
            "irons_spellbooks:fang_strike",
            "irons_spellbooks:fang_swirl",
            "irons_spellbooks:fang_ward",
            "irons_spellbooks:firecracker",
            "irons_spellbooks:gust",
            "irons_spellbooks:lob_creeper",
            "irons_spellbooks:slow",

            "null",
            "irons_spellbooks:black_hole",
            "irons_spellbooks:counterspell",
            "irons_spellbooks:dragon_breath",
            "irons_spellbooks:echoing_strikes",
            "irons_spellbooks:gravity_fissure",
            "irons_spellbooks:magic_arrow",
            "irons_spellbooks:magic_missile",
            "irons_spellbooks:shadow_slash",
            "irons_spellbooks:starfall",

            "null",
            "irons_spellbooks:fire_arrow",
            "irons_spellbooks:fire_breath",
            "irons_spellbooks:fireball",
            "irons_spellbooks:firebolt",
            "irons_spellbooks:flaming_strike",
            "irons_spellbooks:heat_surge",
            "irons_spellbooks:magma_bomb",
            "irons_spellbooks:raise_hell",
            "irons_spellbooks:scorch",
            "irons_spellbooks:burning_dash",

            "null",
            "irons_spellbooks:blood_needles",
            "irons_spellbooks:blood_slash",
            "irons_spellbooks:devour",
            "irons_spellbooks:ray_of_siphoning",
            "irons_spellbooks:wither_skull",
            "irons_spellbooks:heartstop",

            "null",
            "irons_spellbooks:guiding_bolt",
            "irons_spellbooks:healing_circle",
            "irons_spellbooks:sunbeam",
            "irons_spellbooks:wisp",

            "null",
            "irons_spellbooks:blight",
            "irons_spellbooks:earthquake",
            "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:poison_breath",
            "irons_spellbooks:poison_splash",
            "irons_spellbooks:root",
            "irons_spellbooks:stomp",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:spider_aspect",
            "null",
            "irons_spellbooks:sculk_tentacles",
            "irons_spellbooks:sonic_boom",
            "irons_spellbooks:telekinesis",

            "null",
            "irons_spellbooks:chain_lightning",
            "irons_spellbooks:electrocute",
            "irons_spellbooks:lightning_bolt",
            "irons_spellbooks:lightning_lance",
            "irons_spellbooks:shockwave",
            "irons_spellbooks:charge",
            "irons_spellbooks:volt_strike",

            "null",
            "iss_magicfromtheeast:bone_hands",
            "iss_magicfromtheeast:calamity_cut",
            "iss_magicfromtheeast:soul_burst",
            "iss_magicfromtheeast:soul_catalyst",
            "iss_magicfromtheeast:spirit_challenging",
            "iss_magicfromtheeast:splitting_shuriken"
    );
    public static ResourceLocation traitIdOf(String idStr) {
        ResourceLocation spellId = ResourceLocation.tryParse(idStr);
        if (spellId == null) return null;
        return ResourceLocation.fromNamespaceAndPath(
                "examplemod",
                spellId.getNamespace() + "_" + spellId.getPath() + "_spelltrait"
        );
    }
    @SubscribeEvent
    public static void onRegisterTraits(RegisterEvent event) {
        if (!event.getRegistryKey().equals(LHTraits.TRAITS.key())) {
            return;
        }

        LOGGER.info("[ExampleMod] 开始扫描铁魔法法术并注入 L2Hostility 词条（白名单模式）...");

        Registry<AbstractSpell> spellRegistry = SpellRegistry.REGISTRY;
        if (spellRegistry == null) {
            LOGGER.error("[ExampleMod] 铁魔法法术注册表为空或尚未初始化！");
            return;
        }

        int registered = 0;
        int missing = 0;

        for (String idStr : WHITELIST) {
            ResourceLocation spellId = ResourceLocation.tryParse(idStr);
            if (spellId == null) {
                LOGGER.warn("[ExampleMod] 白名单中存在非法 ID: {}", idStr);
                continue;
            }

            AbstractSpell spell = spellRegistry.get(spellId);
            if (spell == null) {
                LOGGER.warn("[ExampleMod] 白名单中的法术不存在（可能是版本差异或拼写错误）: {}", idStr);
                missing++;
                continue;
            }

            if (!spell.isEnabled()) {
                LOGGER.warn("[ExampleMod] 白名单中的法术已被铁魔法配置禁用，跳过: {}", idStr);
                continue;
            }
            event.register(
                    LHTraits.TRAITS.key(),
                    traitIdOf(idStr),
                    () -> new AutoSpellTrait(() -> spell)
            );
            registered++;
        }

        LOGGER.info("[ExampleMod] 白名单注册完成：成功 {} 个，缺失 {} 个。", registered, missing);
    }


}