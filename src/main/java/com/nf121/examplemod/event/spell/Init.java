package com.nf121.examplemod.event.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.tunes_n_tomes.registry.TSchoolRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.warphan.iss_magicfromtheeast.registries.MFTESchoolRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Init {

    // 仅收录：外部模组弹射物 / 独立伤害碰撞体 / 原版投射物
    static final Map<ResourceLocation, Supplier<SchoolType>> ENTITY_TO_SCHOOL = new HashMap<>();
    // 仅收录：外部模组自定义伤害类型 / 原版环境伤害类型
    static final Map<ResourceKey<DamageType>, Supplier<SchoolType>> DAMAGE_TO_SCHOOL = new HashMap<>();

    static {
        // =========================================================================
        // 1. 火焰学派 (FIRE)
        // =========================================================================
        registerEntity("minecraft:fireball", SchoolRegistry.FIRE);
        registerEntity("minecraft:small_fireball", SchoolRegistry.FIRE);
        registerEntity("minecraft:firework_rocket", SchoolRegistry.FIRE);
        registerEntity("cataclysm:ignis_fireball", SchoolRegistry.FIRE);
        registerEntity("cataclysm:ignis_abyss_fireball", SchoolRegistry.FIRE);
        registerEntity("cataclysm:lava_bomb", SchoolRegistry.FIRE);
        registerEntity("cataclysm:flare_bomb", SchoolRegistry.FIRE);
        registerEntity("cataclysm:blazing_bone", SchoolRegistry.FIRE);
        registerEntity("cataclysm:flame_jet", SchoolRegistry.FIRE);
        registerEntity("cataclysm:flame_strike", SchoolRegistry.FIRE);
        registerEntity("cataclysm:ashen_breath", SchoolRegistry.FIRE);
        registerEntity("block_factorys_bosses:blazing_fire_ball", SchoolRegistry.FIRE);
        registerEntity("block_factorys_bosses:flaming_skeleton_guard_fireball", SchoolRegistry.FIRE);
        registerEntity("block_factorys_bosses:fire_area", SchoolRegistry.FIRE);
        registerEntity("twilightforest:hydra_mortar", SchoolRegistry.FIRE);
        registerEntity("twilightforest:lich_bomb", SchoolRegistry.FIRE);
        registerEntity("dungeonnowloading:flame_projectile", SchoolRegistry.FIRE);
        registerEntity("kaleidoscope_tavern:thrown_molotov", SchoolRegistry.FIRE);
        registerEntity("l2complements:strong_fire_charge", SchoolRegistry.FIRE);
        registerEntity("mowziesmobs:super_nova", SchoolRegistry.FIRE);
        registerEntity("mynethersdelight:strider_rock", SchoolRegistry.FIRE);

        registerDamage("minecraft:in_fire", SchoolRegistry.FIRE);
        registerDamage("minecraft:on_fire", SchoolRegistry.FIRE);
        registerDamage("minecraft:lava", SchoolRegistry.FIRE);
        registerDamage("minecraft:hot_floor", SchoolRegistry.FIRE);
        registerDamage("minecraft:campfire", SchoolRegistry.FIRE);
        registerDamage("minecraft:fireball", SchoolRegistry.FIRE);
        registerDamage("minecraft:unattributed_fireball", SchoolRegistry.FIRE);
        registerDamage("minecraft:fireworks", SchoolRegistry.FIRE);
        registerDamage("apothic_attributes:fire_damage", SchoolRegistry.FIRE);
        registerDamage("apothic_attributes:detonation", SchoolRegistry.FIRE);
        registerDamage("cataclysm:abyssal_burn", SchoolRegistry.FIRE);
        registerDamage("cataclysm:flame_strike", SchoolRegistry.FIRE);
        registerDamage("farmersdelight:stove_burn", SchoolRegistry.FIRE);
        registerDamage("golemdungeons:flame_attack", SchoolRegistry.FIRE);
        registerDamage("twilightforest:fiery", SchoolRegistry.FIRE);
        registerDamage("twilightforest:fire_jet", SchoolRegistry.FIRE);
        registerDamage("twilightforest:hydra_fire", SchoolRegistry.FIRE);
        registerDamage("twilightforest:hydra_mortar", SchoolRegistry.FIRE);
        registerDamage("twilightforest:lich_bomb", SchoolRegistry.FIRE);
        registerDamage("twilightforest:ominous_fire", SchoolRegistry.FIRE);
        registerDamage("twilightforest:scorched", SchoolRegistry.FIRE);

        // =========================================================================
        // 2. 冰霜学派 (ICE)
        // =========================================================================
        registerEntity("minecraft:snowball", SchoolRegistry.ICE);
        registerEntity("block_factorys_bosses:ice_spike_pr", SchoolRegistry.ICE);
        registerEntity("block_factorys_bosses:ice_spike", SchoolRegistry.ICE);
        registerEntity("block_factorys_bosses:ice_spike_cluster", SchoolRegistry.ICE);
        registerEntity("block_factorys_bosses:glacial_shove", SchoolRegistry.ICE);
        registerEntity("bosses_of_mass_destruction:comet", SchoolRegistry.ICE);
        registerEntity("mowziesmobs:ice_ball", SchoolRegistry.ICE);
        registerEntity("mowziesmobs:ice_breath", SchoolRegistry.ICE);
        registerEntity("twilightforest:ice_crystal", SchoolRegistry.ICE);
        registerEntity("twilightforest:ice_arrow", SchoolRegistry.ICE);
        registerEntity("twilightforest:ice_snowball", SchoolRegistry.ICE);
        registerEntity("twilightforest:thrown_ice", SchoolRegistry.ICE);
        registerEntity("twilightforest:falling_ice", SchoolRegistry.ICE);
        registerEntity("endermanoverhaul:icy_pearl", SchoolRegistry.ICE);
        registerEntity("simplyswords:frostfall_entity", SchoolRegistry.ICE);

        registerDamage("minecraft:freeze", SchoolRegistry.ICE);
        registerDamage("apothic_attributes:cold_damage", SchoolRegistry.ICE);
        registerDamage("l2complements:freeze", SchoolRegistry.ICE);
        registerDamage("twilightforest:chilling_breath", SchoolRegistry.ICE);
        registerDamage("twilightforest:falling_ice", SchoolRegistry.ICE);
        registerDamage("twilightforest:frozen", SchoolRegistry.ICE);
        registerDamage("twilightforest:snowball_fight", SchoolRegistry.ICE);

        // =========================================================================
        // 3. 雷电学派 (LIGHTNING)
        // =========================================================================
        registerEntity("cataclysm:lightning_spear", SchoolRegistry.LIGHTNING);
        registerEntity("cataclysm:bolt_strike", SchoolRegistry.LIGHTNING);
        registerEntity("cataclysm:laser_beam", SchoolRegistry.LIGHTNING);
        registerEntity("cataclysm:player_ceraunus", SchoolRegistry.LIGHTNING);
        registerEntity("cataclysm:scylla_ceraunus", SchoolRegistry.LIGHTNING);

        registerDamage("minecraft:lightning_bolt", SchoolRegistry.LIGHTNING);
        registerDamage("cataclysm:emp", SchoolRegistry.LIGHTNING);
        registerDamage("cataclysm:laser", SchoolRegistry.LIGHTNING);
        registerDamage("cataclysm:lightning", SchoolRegistry.LIGHTNING);
        registerDamage("cataclysm:player_ceraunus", SchoolRegistry.LIGHTNING);
        registerDamage("cataclysm:storm_bringer", SchoolRegistry.LIGHTNING);

        // =========================================================================
        // 4. 自然学派 (NATURE)
        // =========================================================================
        registerEntity("minecraft:llama_spit", SchoolRegistry.NATURE);
        registerEntity("farmersdelight:rotten_tomato", SchoolRegistry.NATURE);
        registerEntity("cataclysm:water_spear", SchoolRegistry.NATURE);
        registerEntity("cataclysm:poison_dart", SchoolRegistry.NATURE);
        registerEntity("cataclysm:lionfish_spike", SchoolRegistry.NATURE);
        registerEntity("cataclysm:urchin_spike", SchoolRegistry.NATURE);
        registerEntity("cataclysm:octo_ink", SchoolRegistry.NATURE);
        registerEntity("cataclysm:wave", SchoolRegistry.NATURE);
        registerEntity("cataclysm:tidal_hook", SchoolRegistry.NATURE);
        registerEntity("cataclysm:coral_spear", SchoolRegistry.NATURE);
        registerEntity("cataclysm:coral_bardiche", SchoolRegistry.NATURE);
        registerEntity("block_factorys_bosses:poison_spit_pr", SchoolRegistry.NATURE);
        registerEntity("block_factorys_bosses:poison_area", SchoolRegistry.NATURE);
        registerEntity("block_factorys_bosses:kraken_trident", SchoolRegistry.NATURE);
        registerEntity("bosses_of_mass_destruction:petal_blade", SchoolRegistry.NATURE);
        registerEntity("bosses_of_mass_destruction:spore_ball", SchoolRegistry.NATURE);
        registerEntity("mowziesmobs:poison_ball", SchoolRegistry.NATURE);
        registerEntity("mowziesmobs:dart", SchoolRegistry.NATURE);
        registerEntity("twilightforest:nature_bolt", SchoolRegistry.NATURE);
        registerEntity("twilightforest:moonworm_shot", SchoolRegistry.NATURE);
        registerEntity("twilightforest:slime_blob", SchoolRegistry.NATURE);
        registerEntity("endermanoverhaul:bubble_pearl", SchoolRegistry.NATURE);
        registerEntity("simplyswords:livyatan_entity", SchoolRegistry.NATURE);

        registerDamage("minecraft:cactus", SchoolRegistry.NATURE);
        registerDamage("minecraft:sweet_berry_bush", SchoolRegistry.NATURE);
        registerDamage("minecraft:drown", SchoolRegistry.NATURE);
        registerDamage("minecraft:dry_out", SchoolRegistry.NATURE);
        registerDamage("minecraft:sting", SchoolRegistry.NATURE);
        registerDamage("minecraft:spit", SchoolRegistry.NATURE);
        registerDamage("neoforge:poison", SchoolRegistry.NATURE);
        registerDamage("endersdelight:watered", SchoolRegistry.NATURE);
        registerDamage("twilightdelight:thorn_rose_tea", SchoolRegistry.NATURE);
        registerDamage("twilightforest:acid_rain", SchoolRegistry.NATURE);
        registerDamage("twilightforest:moonworm", SchoolRegistry.NATURE);
        registerDamage("twilightforest:oreberry", SchoolRegistry.NATURE);
        registerDamage("twilightforest:stale_sandwich", SchoolRegistry.NATURE);
        registerDamage("twilightforest:thorns", SchoolRegistry.NATURE);

        // =========================================================================
        // 5. 灵魂学派 (SPIRIT - MFTE)
        // =========================================================================
        registerEntity("bosses_of_mass_destruction:soul_star", MFTESchoolRegistries.SPIRIT);
        registerEntity("bosses_of_mass_destruction:blue_fireball", MFTESchoolRegistries.SPIRIT);
        registerEntity("block_factorys_bosses:soul_shockwave", MFTESchoolRegistries.SPIRIT);
        registerEntity("l2complements:soul_fire_charge", MFTESchoolRegistries.SPIRIT);
        registerEntity("endermanoverhaul:soul_pearl", MFTESchoolRegistries.SPIRIT);
        registerEntity("cataclysm:wither_missile", MFTESchoolRegistries.SPIRIT);
        registerEntity("cataclysm:wither_homing_missile", MFTESchoolRegistries.SPIRIT);
        registerEntity("cataclysm:wither_howitzer", MFTESchoolRegistries.SPIRIT);
        registerEntity("simplyswords:soulstalker_cleave", MFTESchoolRegistries.SPIRIT);
        registerEntity("simplyswords:wraithmaw_cutlass", MFTESchoolRegistries.SPIRIT);

        registerDamage("minecraft:wither", MFTESchoolRegistries.SPIRIT);
        registerDamage("minecraft:wither_skull", MFTESchoolRegistries.SPIRIT);
        registerDamage("l2complements:soul_flame", MFTESchoolRegistries.SPIRIT);
        registerDamage("cataclysm:maledictio_anima", MFTESchoolRegistries.SPIRIT);
        registerDamage("twilightforest:expired", MFTESchoolRegistries.SPIRIT);
        registerDamage("twilightforest:ghast_tear", MFTESchoolRegistries.SPIRIT);
        registerDamage("twilightforest:haunt", MFTESchoolRegistries.SPIRIT);

        // =========================================================================
        // 6. 鲜血学派 (BLOOD)
        // =========================================================================
        registerEntity("endermanoverhaul:crimson_pearl", SchoolRegistry.BLOOD);

        registerDamage("minecraft:starve", SchoolRegistry.BLOOD);
        registerDamage("apothic_attributes:bleeding", SchoolRegistry.BLOOD);
        registerDamage("apothic_attributes:current_hp_damage", SchoolRegistry.BLOOD);
        registerDamage("cataclysm:shredder", SchoolRegistry.BLOOD);
        registerDamage("incendium:sacrifice", SchoolRegistry.BLOOD);
        registerDamage("l2complements:bleed", SchoolRegistry.BLOOD);
        registerDamage("l2complements:life_sync", SchoolRegistry.BLOOD);
        registerDamage("l2hostility:killer_aura", SchoolRegistry.BLOOD);
        registerDamage("twilightforest:lifedrain", SchoolRegistry.BLOOD);

        // =========================================================================
        // 7. 末影学派 (ENDER)
        // =========================================================================
        registerEntity("minecraft:ender_pearl", SchoolRegistry.ENDER);
        registerEntity("minecraft:shulker_bullet", SchoolRegistry.ENDER);
        registerEntity("minecraft:dragon_fireball", SchoolRegistry.ENDER);
        registerEntity("bosses_of_mass_destruction:charged_ender_pearl", SchoolRegistry.ENDER);
        registerEntity("cataclysm:ender_guardian_bullet", SchoolRegistry.ENDER);
        registerEntity("cataclysm:void_shard", SchoolRegistry.ENDER);
        registerEntity("cataclysm:void_howitzer", SchoolRegistry.ENDER);
        registerEntity("cataclysm:void_rune", SchoolRegistry.ENDER);
        registerEntity("cataclysm:void_scatter_arrow", SchoolRegistry.ENDER);
        registerEntity("cataclysm:void_vortex", SchoolRegistry.ENDER);
        registerEntity("dungeonnowloading:vertex_orb_projectile", SchoolRegistry.ENDER);
        registerEntity("dungeonnowloading:vertex_arrow_projectile", SchoolRegistry.ENDER);
        registerEntity("dungeonnowloading:vertex_domain_projectile", SchoolRegistry.ENDER);
        registerEntity("dungeonnowloading:vertex_pillar_projectile", SchoolRegistry.ENDER);
        registerEntity("endermanoverhaul:ancient_pearl", SchoolRegistry.ENDER);
        registerEntity("endermanoverhaul:warped_pearl", SchoolRegistry.ENDER);
        registerEntity("endermanoverhaul:ender_bullet", SchoolRegistry.ENDER);
        registerEntity("twilightforest:lich_bolt", SchoolRegistry.ENDER);
        registerEntity("block_factorys_bosses:rift_projectile", SchoolRegistry.ENDER);
        registerEntity("block_factorys_bosses:big_rift_projectile", SchoolRegistry.ENDER);
        registerEntity("simplyswords:caelestis_dreadglare", SchoolRegistry.ENDER);
        registerEntity("simplyswords:gloampiercer_spear", SchoolRegistry.ENDER);

        registerDamage("minecraft:dragon_breath", SchoolRegistry.ENDER);
        registerDamage("minecraft:bad_respawn_point", SchoolRegistry.ENDER);
        registerDamage("minecraft:outside_border", SchoolRegistry.ENDER);
        registerDamage("l2complements:void_eye", SchoolRegistry.ENDER);
        registerDamage("twilightforest:lich_bolt", SchoolRegistry.ENDER);
        registerDamage("twilightforest:twilight_scepter", SchoolRegistry.ENDER);

        // =========================================================================
        // 8. 诡秘学派 (ELDRITCH)
        // =========================================================================
        registerEntity("cataclysm:abyss_blast", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:abyss_orb", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:abyss_mine", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:portal_abyss_blast", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:death_laser_beam", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:mini_abyss_blast", SchoolRegistry.ELDRITCH);
        registerEntity("cataclysm:accretion", SchoolRegistry.ELDRITCH);
        registerEntity("dungeonnowloading:chaos_spawner_projectile", SchoolRegistry.ELDRITCH);
        registerEntity("twilightforest:cube_of_annihilation", SchoolRegistry.ELDRITCH);
        registerEntity("endermanoverhaul:corrupted_pearl", SchoolRegistry.ELDRITCH);
        registerEntity("l2complements:black_fire_charge", SchoolRegistry.ELDRITCH);
        registerEntity("l2hostility:hostility_charge", SchoolRegistry.ELDRITCH);

        registerDamage("minecraft:sonic_boom", SchoolRegistry.ELDRITCH);
        registerDamage("minecraft:out_of_world", SchoolRegistry.ELDRITCH);
        registerDamage("apotheosis:psychic", SchoolRegistry.ELDRITCH);
        registerDamage("apothic_enchanting:corrupted", SchoolRegistry.ELDRITCH);
        registerDamage("cataclysm:deathlaser", SchoolRegistry.ELDRITCH);
        registerDamage("cataclysm:maledictio", SchoolRegistry.ELDRITCH);
        registerDamage("golemdungeons:echo_attack", SchoolRegistry.ELDRITCH);
        registerDamage("modulargolems:echo_attack", SchoolRegistry.ELDRITCH);
        registerDamage("twilightforest:leaf_brain", SchoolRegistry.ELDRITCH);
        registerDamage("twilightforest:lost_words", SchoolRegistry.ELDRITCH);
        registerDamage("twilightforest:reactor", SchoolRegistry.ELDRITCH);

        // =========================================================================
        // 9. 沙漠/大地学派 (DUNE - MFTE)
        // =========================================================================
        registerEntity("cataclysm:sandstorm_projectile", MFTESchoolRegistries.DUNE);
        registerEntity("cataclysm:cursed_sandstorm", MFTESchoolRegistries.DUNE);
        registerEntity("cataclysm:earthquake", MFTESchoolRegistries.DUNE);
        registerEntity("block_factorys_bosses:sand_column", MFTESchoolRegistries.DUNE);
        registerEntity("mowziesmobs:boulder_projectile", MFTESchoolRegistries.DUNE);
        registerEntity("mowziesmobs:rock_sling", MFTESchoolRegistries.DUNE);
        registerEntity("mowziesmobs:earth_spike", MFTESchoolRegistries.DUNE);

        registerDamage("minecraft:falling_block", MFTESchoolRegistries.DUNE);
        registerDamage("minecraft:stalagmite", MFTESchoolRegistries.DUNE);
        registerDamage("minecraft:falling_stalactite", MFTESchoolRegistries.DUNE);
        registerDamage("minecraft:in_wall", MFTESchoolRegistries.DUNE);
        registerDamage("dungeonnowloading:spike", MFTESchoolRegistries.DUNE);

        // =========================================================================
        // 10. 唤魔学派 (EVOCATION)
        // 规范：仅收录“唤魔尖牙、幻影具现武技、掠夺巫术、仆从使役”
        // =========================================================================
        registerEntity("minecraft:evoker_fangs", SchoolRegistry.EVOCATION);
        registerEntity("endermanoverhaul:summoner_pearl", SchoolRegistry.EVOCATION);
        registerEntity("cataclysm:phantom_arrow", SchoolRegistry.EVOCATION);
        registerEntity("cataclysm:phantom_halberd", SchoolRegistry.EVOCATION);
        registerEntity("twilightforest:tome_bolt", SchoolRegistry.EVOCATION);
        registerEntity("twilightforest:wand_bolt", SchoolRegistry.EVOCATION);

        registerDamage("cataclysm:maledictio_sagitta", SchoolRegistry.EVOCATION);
        registerDamage("cataclysm:maledictio_magicae", SchoolRegistry.EVOCATION);
        registerDamage("twilightforest:failed_challenge", SchoolRegistry.EVOCATION);

        // =========================================================================
        // 11. 神圣学派 (HOLY)
        // =========================================================================
        registerEntity("minecraft:spectral_arrow", SchoolRegistry.HOLY);
        registerEntity("mowziesmobs:solar_beam", SchoolRegistry.HOLY);
        registerEntity("mowziesmobs:sunstrike", SchoolRegistry.HOLY);
        registerEntity("modulargolems:beacon_laser", SchoolRegistry.HOLY);
        registerEntity("simplyswords:dawnquiver_arrow", SchoolRegistry.HOLY);
        registerDamage("modulargolems:bacon_attack", SchoolRegistry.HOLY);

        // =========================================================================
        // 12. 旋律学派 (MELODY - Tunes 'n Tomes)
        // =========================================================================
        registerEntity("tunes_n_tomes:default_note", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:black_note", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:red_ball", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:red_arrow", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:blue_ball", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:blue_bird", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:aria_beam", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:hope_burst", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:harp_explosion", TSchoolRegistry.MELODY);
        registerEntity("tunes_n_tomes:fortissimo_release", TSchoolRegistry.MELODY);

        // =========================================================================
        // 13. 对称学派 (SYMMETRY - MFTE)
        // 规范：收录“伤害反弹、镜面平衡”机制
        // =========================================================================
        registerDamage("l2hostility:reflect", MFTESchoolRegistries.SYMMETRY);
    }

    private static void registerEntity(String id, Supplier<SchoolType> school) {
        ENTITY_TO_SCHOOL.put(ResourceLocation.parse(id), school);
    }

    private static void registerDamage(String id, Supplier<SchoolType> school) {
        DAMAGE_TO_SCHOOL.put(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(id)), school);
    }
}
