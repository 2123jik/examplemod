package com.nf121.examplemod.event.util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.alshanex.tunes_n_tomes.registry.TAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.warphan.iss_magicfromtheeast.registries.MFTEAttributeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
//@EventBusSubscriber
public final class AttributeBookData {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("examplemod")
            .resolve("entity_attributes.json");
    private static final ResourceLocation R_FIRE   = id("fire_magic_resist");
    private static final ResourceLocation R_ICE    = id("ice_magic_resist");
    private static final ResourceLocation R_LIGHT  = id("lightning_magic_resist");
    private static final ResourceLocation R_HOLY   = id("holy_magic_resist");
    private static final ResourceLocation R_ENDER  = id("ender_magic_resist");
    private static final ResourceLocation R_BLOOD  = id("blood_magic_resist");
    private static final ResourceLocation R_EVO    = id("evocation_magic_resist");
    private static final ResourceLocation R_NATURE = id("nature_magic_resist");
    private static final ResourceLocation R_ELD    = id("eldritch_magic_resist");
    private static final ResourceLocation R_SPIRIT = id("spirit_magic_resist");
    private static final ResourceLocation R_SYM    = id("symmetry_magic_resist");
    private static final ResourceLocation R_DUNE   = id("dune_magic_resist");
    private static final ResourceLocation R_MELODY = id("melody_magic_resist");
    private static final ResourceLocation R_UNIV = id("spell_resist");


    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("examplemod", path);
    }

    public static void fire(LivingEntity e, double m)     { mod(e, AttributeRegistry.FIRE_MAGIC_RESIST,          R_FIRE,   m); }
    public static void ice(LivingEntity e, double m)      { mod(e, AttributeRegistry.ICE_MAGIC_RESIST,           R_ICE,    m); }
    public static void lightning(LivingEntity e, double m){ mod(e, AttributeRegistry.LIGHTNING_MAGIC_RESIST,     R_LIGHT,  m); }
    public static void holy(LivingEntity e, double m)     { mod(e, AttributeRegistry.HOLY_MAGIC_RESIST,          R_HOLY,   m); }
    public static void ender(LivingEntity e, double m)    { mod(e, AttributeRegistry.ENDER_MAGIC_RESIST,         R_ENDER,  m); }
    public static void blood(LivingEntity e, double m)    { mod(e, AttributeRegistry.BLOOD_MAGIC_RESIST,         R_BLOOD,  m); }
    public static void evocation(LivingEntity e, double m){ mod(e, AttributeRegistry.EVOCATION_MAGIC_RESIST,     R_EVO,    m); }
    public static void nature(LivingEntity e, double m)   { mod(e, AttributeRegistry.NATURE_MAGIC_RESIST,        R_NATURE, m); }
    public static void eldritch(LivingEntity e, double m) { mod(e, AttributeRegistry.ELDRITCH_MAGIC_RESIST,      R_ELD,    m); }
    public static void spirit(LivingEntity e, double m)   { mod(e, MFTEAttributeRegistries.SPIRIT_MAGIC_RESIST,   R_SPIRIT, m); }
    public static void symmetry(LivingEntity e, double m) { mod(e, MFTEAttributeRegistries.SYMMETRY_MAGIC_RESIST, R_SYM,    m); }
    public static void dune(LivingEntity e, double m)     { mod(e, MFTEAttributeRegistries.DUNE_MAGIC_RESIST,     R_DUNE,   m); }
    public static void melody(LivingEntity e, double m)   { mod(e, TAttributeRegistry.MELODY_MAGIC_RESIST,        R_MELODY, m); }
    public static void univ(LivingEntity e, double m)   { mod(e, AttributeRegistry.SPELL_RESIST,        R_UNIV, m); }

    private static void mod(LivingEntity e, Holder<Attribute> holder,
                            ResourceLocation rid, double percent) {
        AttributeInstance inst = e.getAttribute(holder);
        if (inst == null) return;
        inst.removeModifier(rid);
        inst.addPermanentModifier(new AttributeModifier(
                rid, -percent / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }


//    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isSpawnCancelled()) return;
        var e = event.getEntity();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();

        switch (id) {
            // ==================== 原版 ====================
            case "minecraft:skeleton"                    -> { holy(e, -40); blood(e, +50); }
            case "minecraft:stray"                       -> { holy(e, -40); blood(e, +50); ice(e, -40); }
            case "minecraft:bogged"                      -> { holy(e, -40); blood(e, +50); nature(e, -30); }
            case "minecraft:wither_skeleton"             -> { holy(e, -50); blood(e, +80); fire(e, -50); }
            case "minecraft:skeleton_horse"              -> { holy(e, -40); blood(e, +50); }
            case "minecraft:zombie"                      -> { holy(e, -30); blood(e, +30); }
            case "minecraft:zombie_villager"             -> { holy(e, -30); blood(e, +30); }
            case "minecraft:husk"                        -> { holy(e, -30); blood(e, +30); nature(e, -20); }
            case "minecraft:drowned"                     -> { holy(e, +20); blood(e, -20); nature(e, -20); }
            case "minecraft:zombie_horse"                -> { holy(e, -30); blood(e, +30); }
            case "minecraft:zombified_piglin"            -> { fire(e, -50); ice(e, +50); }
            case "minecraft:piglin"                      -> { fire(e, -30); }
            case "minecraft:piglin_brute"                -> { fire(e, -40); }
            case "minecraft:zoglin"                      -> { fire(e, -30); }
            case "minecraft:hoglin"                      -> { fire(e, -30); }
            case "minecraft:blaze"                       -> { fire(e, -100); ice(e, +100); nature(e, +30); }
            case "minecraft:magma_cube"                  -> { fire(e, -100); ice(e, +100); }
            case "minecraft:ghast"                       -> { fire(e, -80); ice(e, +80); }
            case "minecraft:strider"                     -> { fire(e, -50); ice(e, +50); }
            case "minecraft:snow_golem"                  -> { ice(e, -50); fire(e, +50); }
            case "minecraft:enderman"                    -> { ender(e, -100); nature(e, +50); }
            case "minecraft:endermite"                   -> { ender(e, -50); }
            case "minecraft:shulker"                     -> { ender(e, -50); }
            case "minecraft:phantom"                     -> { ender(e, -30); }
            case "minecraft:ender_dragon"                -> { ender(e, -100); fire(e, -50); holy(e, +30); blood(e, -50); }
            case "minecraft:evoker"                      -> { evocation(e, -50); blood(e, -20); }
            case "minecraft:illusioner"                  -> { evocation(e, -40); }
            case "minecraft:vex"                         -> { evocation(e, -30); }
            case "minecraft:witch"                       -> { blood(e, -20); nature(e, -30); }
            case "minecraft:warden"                      -> { eldritch(e, -80); nature(e, -30); }
            case "minecraft:wither"                      -> { holy(e, +50); blood(e, -80); eldritch(e, -50); fire(e, -30); }
            case "minecraft:elder_guardian"              -> { ice(e, -30); nature(e, -50); }
            case "minecraft:guardian"                    -> { ice(e, -20); nature(e, -30); }
            case "minecraft:polar_bear"                  -> { ice(e, -50); }
            case "minecraft:spider"                      -> { nature(e, -20); }
            case "minecraft:cave_spider"                 -> { nature(e, -30); }
            case "minecraft:silverfish"                  -> { nature(e, -30); }
            case "minecraft:bee"                         -> { nature(e, -20); }
            case "minecraft:cat"                         -> { nature(e, -20); }
            case "minecraft:ocelot"                      -> { nature(e, -20); }
            case "minecraft:fox"                         -> { nature(e, -20); }
            case "minecraft:wolf"                        -> { nature(e, -20); }
            case "minecraft:panda"                       -> { nature(e, -20); }
            case "minecraft:mooshroom"                   -> { nature(e, -20); }
            case "minecraft:sniffer"                     -> { nature(e, -20); }
            case "minecraft:armadillo"                   -> { nature(e, -20); }
            case "minecraft:frog"                        -> { nature(e, -20); }
            case "minecraft:tadpole"                     -> { nature(e, -50); }
            case "minecraft:axolotl"                     -> { nature(e, -30); }
            case "minecraft:turtle"                      -> { nature(e, -30); }
            case "minecraft:dolphin"                     -> { nature(e, -30); }
            case "minecraft:squid"                       -> { nature(e, -30); }
            case "minecraft:glow_squid"                  -> { nature(e, -30); }
            case "minecraft:cod"                         -> { nature(e, -30); }
            case "minecraft:salmon"                      -> { nature(e, -30); }
            case "minecraft:tropical_fish"               -> { nature(e, -30); }
            case "minecraft:pufferfish"                  -> { nature(e, -30); }
            case "minecraft:cow"                         -> { nature(e, -10); }
            case "minecraft:rabbit"                      -> { nature(e, -10); }
            case "minecraft:parrot"                      -> { nature(e, -10); }
            case "minecraft:breeze"                      -> { ender(e, -50); fire(e, +30); }

            // ==================== Iron's Spells ====================
            case "irons_spellbooks:fire_boss"            -> { fire(e, -100); ice(e, +100); }
            case "irons_spellbooks:cryomancer"           -> { ice(e, -80); }
            case "irons_spellbooks:pyromancer"           -> { fire(e, -80); }
            case "irons_spellbooks:frozen_humanoid"      -> { ice(e, -80); }
            case "irons_spellbooks:ice_spider"           -> { ice(e, -50); nature(e, -30); }
            case "irons_spellbooks:archevoker"           -> { evocation(e, -50); }
            case "irons_spellbooks:necromancer"          -> { holy(e, +40); blood(e, -60); evocation(e, -30); }
            case "irons_spellbooks:dead_king"            -> { holy(e, +60); blood(e, -80); evocation(e, -50); }
            case "irons_spellbooks:dead_king_corpse"     -> { holy(e, +30); blood(e, -30); }
            case "irons_spellbooks:catacombs_zombie"     -> { holy(e, +30); blood(e, -30); }
            case "irons_spellbooks:citadel_keeper"       -> { fire(e, -50); holy(e, +30); }
            case "irons_spellbooks:apothecarist"         -> { blood(e, -20); nature(e, -30); }
            case "irons_spellbooks:cultist"              -> { blood(e, -30); eldritch(e, -20); }
            case "irons_spellbooks:priest"               -> { holy(e, -50); }
            case "irons_spellbooks:root"                 -> { nature(e, -50); }
            case "irons_spellbooks:scapegoat"            -> { nature(e, -20); }
            case "irons_spellbooks:firefly_swarm"        -> { nature(e, -30); }
            case "irons_spellbooks:sculk_tentacle"       -> { eldritch(e, -30); }
            case "irons_spellbooks:wisp"                 -> { holy(e, -50); }
            case "irons_spellbooks:cursed_armor_stand"   -> { holy(e, +20); blood(e, -20); }
            case "irons_spellbooks:summoned_polar_bear"  -> { ice(e, -50); }
            case "irons_spellbooks:summoned_skeleton"    -> { holy(e, +40); blood(e, -50); }
            case "irons_spellbooks:summoned_zombie"      -> { holy(e, +30); blood(e, -30); }
            case "irons_spellbooks:summoned_vex"         -> { evocation(e, -30); }

            // ==================== Cataclysm ====================
            case "cataclysm:ignis"                       -> { fire(e, -100); ice(e, +100); }
            case "cataclysm:ignited_berserker"           -> { fire(e, -80); ice(e, +50); }
            case "cataclysm:ignited_revenant"            -> { fire(e, -80); ice(e, +50); holy(e, +30); }
            case "cataclysm:cindaria"                    -> { fire(e, -80); ice(e, +50); }
            case "cataclysm:netherite_monstrosity"       -> { fire(e, -80); ice(e, +50); }
            case "cataclysm:netherite_ministrosity"      -> { fire(e, -50); }
            case "cataclysm:ancient_remnant"             -> { fire(e, -30); holy(e, +30); }
            case "cataclysm:modern_remnant"              -> { fire(e, -30); }
            case "cataclysm:aptrgangr"                   -> { holy(e, +50); blood(e, -80); ice(e, -50); }
            case "cataclysm:elite_draugr"                -> { holy(e, +40); blood(e, -50); }
            case "cataclysm:royal_draugr"                -> { holy(e, +40); blood(e, -50); ice(e, -30); }
            case "cataclysm:draugr"                      -> { holy(e, +30); blood(e, -30); }
            case "cataclysm:drowned_host"                -> { holy(e, +20); blood(e, -20); nature(e, -20); }
            case "cataclysm:koboleton"                   -> { holy(e, +20); blood(e, -20); }
            case "cataclysm:ender_guardian"              -> { ender(e, -100); }
            case "cataclysm:ender_golem"                 -> { ender(e, -80); }
            case "cataclysm:endermaptera"                -> { ender(e, -50); }
            case "cataclysm:the_harbinger"               -> { ender(e, -50); eldritch(e, -50); }
            case "cataclysm:the_watcher"                 -> { ender(e, -80); eldritch(e, -50); }
            case "cataclysm:the_prowler"                 -> { ender(e, -30); }
            case "cataclysm:maledictus"                  -> { eldritch(e, -50); holy(e, +30); }
            case "cataclysm:the_leviathan"               -> { ice(e, -50); nature(e, -60); }
            case "cataclysm:the_baby_leviathan"          -> { ice(e, -30); nature(e, -50); }
            case "cataclysm:scylla"                      -> { ender(e, -30); nature(e, -30); }
            case "cataclysm:symbiocto"                   -> { nature(e, -30); }
            case "cataclysm:coral_golem"                 -> { ice(e, -30); nature(e, -30); }
            case "cataclysm:coralssus"                   -> { nature(e, -20); }
            case "cataclysm:deepling"                    -> { nature(e, -20); }
            case "cataclysm:deepling_brute"              -> { nature(e, -20); }
            case "cataclysm:deepling_angler"             -> { nature(e, -20); ender(e, -20); }
            case "cataclysm:deepling_priest"             -> { nature(e, -20); holy(e, -20); }
            case "cataclysm:deepling_warlock"            -> { nature(e, -20); eldritch(e, -30); }
            case "cataclysm:lionfish"                    -> { nature(e, -30); }
            case "cataclysm:urchinkin"                   -> { nature(e, -30); }
            case "cataclysm:hippocamtus"                 -> { nature(e, -30); }
            case "cataclysm:wadjet"                      -> { nature(e, -30); holy(e, +30); }

            // ==================== Twilight Forest ====================
            case "twilightforest:lich"                   -> { holy(e, +50); blood(e, -80); evocation(e, -50); }
            case "twilightforest:lich_minion"            -> { holy(e, +30); blood(e, -30); }
            case "twilightforest:snow_queen"             -> { ice(e, -100); fire(e, +100); }
            case "twilightforest:ice_crystal"            -> { ice(e, -100); fire(e, +100); }
            case "twilightforest:stable_ice_core"        -> { ice(e, -80); }
            case "twilightforest:unstable_ice_core"      -> { ice(e, -80); }
            case "twilightforest:snow_guardian"          -> { ice(e, -50); }
            case "twilightforest:winter_wolf"            -> { ice(e, -50); }
            case "twilightforest:alpha_yeti"             -> { ice(e, -50); fire(e, +50); }
            case "twilightforest:yeti"                   -> { ice(e, -50); fire(e, +50); }
            case "twilightforest:hydra"                  -> { fire(e, -80); nature(e, -30); }
            case "twilightforest:carminite_golem"        -> { fire(e, -80); ice(e, +50); }
            case "twilightforest:carminite_ghastguard"   -> { fire(e, -50); ice(e, +30); }
            case "twilightforest:carminite_ghastling"    -> { fire(e, -50); }
            case "twilightforest:carminite_broodling"    -> { fire(e, -50); }
            case "twilightforest:fire_beetle"            -> { fire(e, -80); ice(e, +50); }
            case "twilightforest:ur_ghast"               -> { fire(e, -80); ice(e, +80); }
            case "twilightforest:adherent"               -> { ender(e, -50); eldritch(e, -30); }
            case "twilightforest:harbinger_cube"         -> { ender(e, -50); }
            case "twilightforest:death_tome"             -> { ender(e, -30); eldritch(e, -30); }
            case "twilightforest:wraith"                 -> { holy(e, +50); blood(e, -50); ender(e, -30); }
            case "twilightforest:knight_phantom"         -> { holy(e, +30); blood(e, -50); }
            case "twilightforest:skeleton_druid"         -> { holy(e, +40); blood(e, -50); nature(e, -30); }
            case "twilightforest:loyal_zombie"           -> { holy(e, +30); blood(e, -30); }
            case "twilightforest:rising_zombie"          -> { holy(e, +30); blood(e, -30); }
            case "twilightforest:minoshroom"             -> { nature(e, -30); }
            case "twilightforest:naga"                   -> { nature(e, -30); }
            case "twilightforest:hedge_spider"           -> { nature(e, -30); }
            case "twilightforest:king_spider"            -> { nature(e, -30); }
            case "twilightforest:swarm_spider"           -> { nature(e, -30); }
            case "twilightforest:pinch_beetle"           -> { nature(e, -30); }
            case "twilightforest:slime_beetle"           -> { nature(e, -30); }
            case "twilightforest:helmet_crab"            -> { nature(e, -30); }
            case "twilightforest:towerwood_borer"        -> { nature(e, -30); }
            case "twilightforest:troll"                  -> { nature(e, -30); }
            case "twilightforest:mosquito_swarm"         -> { nature(e, -30); }
            case "twilightforest:redcap"                 -> { nature(e, -20); }
            case "twilightforest:redcap_sapper"          -> { nature(e, -30); }
            case "twilightforest:mist_wolf"              -> { ender(e, -20); nature(e, -20); }
            case "twilightforest:hostile_wolf"           -> { nature(e, -20); }
            case "twilightforest:penguin"                -> { ice(e, -30); }
            case "twilightforest:raven"                  -> { ender(e, -20); }
            case "twilightforest:boar"                   -> { nature(e, -10); }
            case "twilightforest:deer"                   -> { nature(e, -10); }
            case "twilightforest:squirrel"               -> { nature(e, -10); }
            case "twilightforest:dwarf_rabbit"           -> { nature(e, -10); }
            case "twilightforest:tiny_bird"              -> { nature(e, -10); }

            // ==================== Mowzie's Mobs ====================
            case "mowziesmobs:frostmaw"                  -> { ice(e, -80); fire(e, +50); }
            case "mowziesmobs:umvuthi"                   -> { fire(e, -30); }
            case "mowziesmobs:foliaath"                  -> { nature(e, -30); }
            case "mowziesmobs:baby_foliaath"             -> { nature(e, -30); }
            case "mowziesmobs:grottol"                   -> { nature(e, -30); }
            case "mowziesmobs:naga"                      -> { nature(e, -30); }

            // ==================== Enderman Overhaul ====================
            case "endermanoverhaul:end_enderman"             -> { ender(e, -100); }
            case "endermanoverhaul:end_islands_enderman"     -> { ender(e, -100); }
            case "endermanoverhaul:axolotl_pet_enderman"     -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:badlands_enderman"        -> { ender(e, -80); fire(e, -20); }
            case "endermanoverhaul:cave_enderman"            -> { ender(e, -80); }
            case "endermanoverhaul:coral_enderman"           -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:crimson_forest_enderman"  -> { ender(e, -80); fire(e, -30); }
            case "endermanoverhaul:dark_oak_enderman"        -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:desert_enderman"          -> { ender(e, -80); fire(e, -20); }
            case "endermanoverhaul:flower_fields_enderman"   -> { ender(e, -80); nature(e, -30); }
            case "endermanoverhaul:hammerhead_pet_enderman"  -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:ice_spikes_enderman"      -> { ender(e, -80); ice(e, -30); }
            case "endermanoverhaul:mushroom_fields_enderman" -> { ender(e, -80); nature(e, -30); }
            case "endermanoverhaul:nether_wastes_enderman"   -> { ender(e, -80); fire(e, -40); }
            case "endermanoverhaul:pet_enderman"             -> { ender(e, -80); }
            case "endermanoverhaul:savanna_enderman"         -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:snowy_enderman"           -> { ender(e, -80); ice(e, -30); }
            case "endermanoverhaul:soulsand_valley_enderman" -> { ender(e, -80); fire(e, -30); blood(e, -20); }
            case "endermanoverhaul:swamp_enderman"           -> { ender(e, -80); nature(e, -30); }
            case "endermanoverhaul:warped_forest_enderman"   -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:windswept_hills_enderman" -> { ender(e, -80); nature(e, -20); }
            case "endermanoverhaul:scarab"                   -> { nature(e, -30); }
            case "endermanoverhaul:spirit"                   -> { ender(e, -50); blood(e, -30); }

            // ==================== Dungeon Now Loading ====================
            case "dungeonnowloading:sealed_chaos"          -> { eldritch(e, -80); }
            case "dungeonnowloading:chaos_spawner"         -> { eldritch(e, -50); }
            case "dungeonnowloading:copper_creep"          -> { lightning(e, -50); }
            case "dungeonnowloading:fairkeeper_boros"      -> { holy(e, -50); }
            case "dungeonnowloading:fairkeeper_boros_part" -> { holy(e, -30); }
            case "dungeonnowloading:fairkeeper_ouros"      -> { holy(e, -50); }
            case "dungeonnowloading:fairkeeper_ouros_part" -> { holy(e, -30); }
            case "dungeonnowloading:hollow"                -> { ender(e, -30); }
            case "dungeonnowloading:whimper"               -> { ender(e, -30); }
            case "dungeonnowloading:scuttle"               -> { nature(e, -20); }

            // ==================== ISS Magic From The East ====================
            case "iss_magicfromtheeast:jade_executioner"    -> { symmetry(e, -50); }
            case "iss_magicfromtheeast:onmyoji"             -> { spirit(e, -30); symmetry(e, -30); }
            case "iss_magicfromtheeast:taoist"              -> { symmetry(e, -40); spirit(e, -30); }
            case "iss_magicfromtheeast:jiangshi"            -> { spirit(e, -50); holy(e, +30); }
            case "iss_magicfromtheeast:summoned_jiangshi"   -> { spirit(e, -50); }
            case "iss_magicfromtheeast:spirit_ashigaru"     -> { spirit(e, -50); }
            case "iss_magicfromtheeast:spirit_samurai"      -> { spirit(e, -60); }
            case "iss_magicfromtheeast:extracted_soul"      -> { spirit(e, -50); }
            case "iss_magicfromtheeast:bone_hand"           -> { spirit(e, -30); }
            case "iss_magicfromtheeast:summoned_kitsune"    -> { spirit(e, -40); }

            // ==================== Simply Swords ====================
            case "simplyswords:skeleton_minion"      -> { holy(e, +40); blood(e, -50); }
            case "simplyswords:caelestis_riftling"   -> { ender(e, -50); }
            case "simplyswords:caelestis_dreadglare" -> { ender(e, -30); }
            case "simplyswords:caelestis_hollow"     -> { ender(e, -30); }
            case "simplyswords:riftmane_charger"     -> { ender(e, -30); }
            case "simplyswords:soulstalker_stride"   -> { ender(e, -30); }
            case "simplyswords:watcher_bat"          -> { ender(e, -30); }
            case "simplyswords:simplyaxolotlentity"  -> { nature(e, -30); }
            case "simplyswords:simplybeeentity"      -> { nature(e, -20); }
            case "simplyswords:wolf_minion"          -> { nature(e, -20); }

            default -> { /* 无抗性配置 */ }
        }
    }
    //        AttributeBookData.applyTo(event.getEntity());

    /*
     * 始终显示的属性白名单。
     * 不在这个集合里、且值等于注册默认值的属性，
     * 只有打开“显示全部”时才会出现在实体页。
     * 非默认值属性永远显示，不受白名单和开关影响。
     */
    public static final Set<String> ALWAYS_VISIBLE = Set.of(
            "irons_spellbooks:blood_magic_resist",
            "irons_spellbooks:blood_spell_power",
            "irons_spellbooks:eldritch_magic_resist",
            "irons_spellbooks:eldritch_spell_power",
            "irons_spellbooks:ender_magic_resist",
            "irons_spellbooks:ender_spell_power",
            "irons_spellbooks:evocation_magic_resist",
            "irons_spellbooks:evocation_spell_power",
            "irons_spellbooks:fire_magic_resist",
            "irons_spellbooks:fire_spell_power",
            "irons_spellbooks:holy_magic_resist",
            "irons_spellbooks:holy_spell_power",
            "irons_spellbooks:ice_magic_resist",
            "irons_spellbooks:ice_spell_power",
            "irons_spellbooks:lightning_magic_resist",
            "irons_spellbooks:lightning_spell_power",
            "irons_spellbooks:max_mana",
            "irons_spellbooks:nature_magic_resist",
            "irons_spellbooks:nature_spell_power",
            "irons_spellbooks:spell_power",
            "irons_spellbooks:spell_resist",
            "tunes_n_tomes:melody_magic_resist",
            "tunes_n_tomes:melody_spell_power",
            "iss_magicfromtheeast:dune_magic_resist",
            "iss_magicfromtheeast:dune_spell_power",
            "iss_magicfromtheeast:spirit_magic_resist",
            "iss_magicfromtheeast:spirit_spell_power",
            "iss_magicfromtheeast:symmetry_magic_resist",
            "iss_magicfromtheeast:symmetry_spell_power",
            "minecraft:generic.armor",
            "minecraft:generic.armor_toughness",
            "minecraft:generic.attack_damage",
            "minecraft:generic.attack_knockback",
            "minecraft:generic.gravity",
            "minecraft:generic.jump_strength",
            "minecraft:generic.knockback_resistance",
            "minecraft:generic.max_health",
            "minecraft:generic.movement_speed",
            "minecraft:generic.step_height",
            "minecraft:generic.water_movement_efficiency"
    );

    private static List<EntityType<? extends LivingEntity>> supportedTypes;

    @SuppressWarnings("unchecked")
    private static List<EntityType<? extends LivingEntity>> supportedTypes() {
        if (supportedTypes == null) {
            List<EntityType<? extends LivingEntity>> list = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (DefaultAttributes.hasSupplier(type)) {
                    list.add((EntityType<? extends LivingEntity>) type);
                }
            }
            supportedTypes = List.copyOf(list);
        }
        return supportedTypes;
    }
    private static Table current;

    public static final class Table {
        public int schema_version = 1;
        public TreeSet<String> excluded_entities = new TreeSet<>();
        public TreeMap<String, EntityEntry> entities = new TreeMap<>();
    }

    public static final class EntityEntry {
        public TreeMap<String, Double> attributes = new TreeMap<>();
    }

    private AttributeBookData() {
    }

    public static boolean isExcluded(String entityId) {
        return entityId != null
                && current != null
                && current.excluded_entities.contains(entityId);
    }
    public static void applyTo(LivingEntity living) {
        if (current == null) {
            synchronized (AttributeBookData.class) {
                if (current == null) {
                    try {
                        current = Files.exists(FILE) ? read() : new Table();
                    } catch (IOException exception) {
                        LOGGER.error("[EntityBook] 读取失败 {}", FILE, exception);
                        return;
                    }
                }
            }
        }

        String entityId = BuiltInRegistries.ENTITY_TYPE
                .getKey(living.getType())
                .toString();

        if (isExcluded(entityId)) {
            return;
        }

        EntityEntry entry = current.entities.get(entityId);
        if (entry == null || entry.attributes.isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> type =
                (EntityType<? extends LivingEntity>) living.getType();

        AttributeSupplier supplier = DefaultAttributes.getSupplier(type);
        for (var row : entry.attributes.entrySet()) {
            Holder.Reference<Attribute> holder = attribute(row.getKey());
            if (holder == null || !supplier.hasAttribute(holder)) {
                continue;
            }

            AttributeInstance instance = living.getAttribute(holder);
            if (instance == null) {
                continue;
            }

            double defaultBase = supplier.getBaseValue(holder);
            if (defaultBase == 0.0) {
                continue;
            }

            double multiplier = row.getValue() / defaultBase;
            double amount = multiplier - 1.0;
            if (Math.abs(amount) < 1e-9) {
                continue;
            }

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "examplemod",
                    "attr_book/" + row.getKey()
                            .replace(':', '_')
                            .replace('.', '_')
            );

            // 先移再添，保证幂等
            instance.removeModifier(id);
            instance.addPermanentModifier(new AttributeModifier(
                    id,
                    amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
    public static Holder.Reference<Attribute> attribute(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        return BuiltInRegistries.ATTRIBUTE.getHolder(location).orElse(null);
    }

    /* 值是否等于属性自身的注册默认值。 */
    public static boolean isRegisteredDefault(String id, double value) {
        Holder.Reference<Attribute> holder = attribute(id);
        return holder != null
                && Double.compare(holder.value().getDefaultValue(), value) == 0;
    }



    public static synchronized Table open() throws IOException {
        if (current == null) {
            current = Files.exists(FILE) ? read() : new Table();

            boolean changed = ensureEntitiesFilled(current);

            if (changed || !Files.exists(FILE)) {
                save(current);
            }
        }
        return current;
    }

    /*
     * 把 supportedTypes 里尚未登记的实体补进 entities。
     * 已有条目（连同用户的编辑）保持不变。
     * 返回是否有改动。
     */
    private static boolean ensureEntitiesFilled(Table table) {
        List<EntityType<? extends LivingEntity>> types = supportedTypes();
        if (types.isEmpty()) {
            LOGGER.warn("[EntityBook] 没有可用的 LivingEntity 类型，无法填充实体列表");
            return false;
        }

        boolean changed = false;
        for (EntityType<? extends LivingEntity> type : types) {
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (table.entities.putIfAbsent(entityId, new EntityEntry()) == null) {
                changed = true;
            }
        }
        return changed;
    }
    /*
     * 实体页数据：实体拥有的全部属性，值为覆盖值或默认供应器值。
     * 可见性由屏幕决定（白名单 / 非默认值 / 显示开关）。
     */
    public static TreeMap<String, Double> entityValues(String entityId) {
        TreeMap<String, Double> result = new TreeMap<>();
        AttributeSupplier supplier = supplierFor(entityId);
        if (supplier == null) {
            return result;
        }

        EntityEntry entry = current == null ? null : current.entities.get(entityId);

        for (ResourceLocation id : BuiltInRegistries.ATTRIBUTE.keySet()) {
            Holder.Reference<Attribute> holder =
                    BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
            if (holder == null || !supplier.hasAttribute(holder)) {
                continue;
            }

            String sid = id.toString();
            Double override = entry == null ? null : entry.attributes.get(sid);
            result.put(sid, override != null
                    ? override
                    : supplier.getBaseValue(holder));
        }

        return result;
    }

    /* 预览用：只返回该实体的覆盖值。 */
    public static TreeMap<String, Double> resolved(String entityId) {
        TreeMap<String, Double> result = new TreeMap<>();
        if (current == null) {
            return result;
        }
        EntityEntry entry = current.entities.get(entityId);
        if (entry != null) {
            result.putAll(entry.attributes);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier supplierFor(String entityId) {
        ResourceLocation location = ResourceLocation.tryParse(entityId);
        if (location == null) {
            return null;
        }
        EntityType<?> raw = BuiltInRegistries.ENTITY_TYPE.get(location);
        if (raw == null) {
            return null;
        }
        try {
            return DefaultAttributes.getSupplier(
                    (EntityType<? extends LivingEntity>) raw
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String valueError(String id, double value) {
        if (!Double.isFinite(value)) {
            return "数值必须是有限数字";
        }

        Holder.Reference<Attribute> holder = attribute(id);
        if (holder != null) {
            double sanitized = holder.value().sanitizeValue(value);
            if (Double.compare(sanitized, value) != 0) {
                return "超出属性范围；该值会被限制为 " + sanitized;
            }
        }

        return null;
    }

    public static synchronized void save(Table table) throws IOException {
        validate(table);
        Files.createDirectories(FILE.getParent());

        Path temporary = Files.createTempFile(
                FILE.getParent(),
                "entity_attributes_",
                ".tmp"
        );

        try {
            try (Writer writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8
            )) {
                GSON.toJson(table, writer);
            }

            try {
                Files.move(
                        temporary,
                        FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporary,
                        FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Table read() throws IOException {
        try (Reader reader = Files.newBufferedReader(
                FILE,
                StandardCharsets.UTF_8
        )) {
            Table table = GSON.fromJson(reader, Table.class);
            validate(table);
            return table;
        }
    }

    private static void validate(Table table) throws IOException {
        if (table == null
                || table.schema_version != 1
                || table.entities == null) {
            throw new IOException("JSON 结构错误或 schema_version 不受支持");
        }

        if (table.excluded_entities == null) {
            table.excluded_entities = new TreeSet<>();
        }

        for (String entityId : table.excluded_entities) {
            if (entityId == null || ResourceLocation.tryParse(entityId) == null) {
                throw new IOException("排除列表包含非法实体 ID：" + entityId);
            }
        }

        for (var row : table.entities.entrySet()) {
            if (ResourceLocation.tryParse(row.getKey()) == null) {
                throw new IOException("非法实体 ID：" + row.getKey());
            }
            EntityEntry entry = row.getValue();
            if (entry == null || entry.attributes == null) {
                throw new IOException("实体条目结构错误：" + row.getKey());
            }
            for (var attr : entry.attributes.entrySet()) {
                if (ResourceLocation.tryParse(attr.getKey()) == null
                        || attr.getValue() == null) {
                    throw new IOException("非法属性条目：" + attr.getKey());
                }
                String error = valueError(attr.getKey(), attr.getValue());
                if (error != null) {
                    throw new IOException(attr.getKey() + "：" + error);
                }
            }
        }
    }
}