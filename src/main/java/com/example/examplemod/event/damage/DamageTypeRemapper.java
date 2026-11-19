package com.example.examplemod.event.damage;

import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.Map;

public class DamageTypeRemapper {

    // 在这里写死映射：原伤害类型 ID -> 目标伤害类型 ID（全都在这一处改）
    private static final Map<String, ResourceKey<DamageType>> REMAP = Map.ofEntries(
            // ================= 1. 铁魔法：火焰 (fire_magic) =================
            Map.entry("apothic_attributes:fire_damage", key("irons_spellbooks:fire_magic")),
            Map.entry("apothic_attributes:detonation", key("irons_spellbooks:fire_magic")),
            Map.entry("cataclysm:flame_strike", key("irons_spellbooks:fire_magic")),
            Map.entry("cataclysm:deathlaser", key("irons_spellbooks:fire_magic")),
            Map.entry("cataclysm:laser", key("irons_spellbooks:fire_magic")),
            Map.entry("golemdungeons:flame_attack", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:fiery", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:fire_jet", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:ghast_tear", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:hydra_fire", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:lich_bomb", key("irons_spellbooks:fire_magic")),
            Map.entry("twilightforest:scorched", key("irons_spellbooks:fire_magic")),

            // ================= 2. 铁魔法：冰霜 (ice_magic) =================
            Map.entry("apothic_attributes:cold_damage", key("irons_spellbooks:ice_magic")),
            Map.entry("l2complements:freeze", key("irons_spellbooks:ice_magic")),
            Map.entry("twilightforest:chilling_breath", key("irons_spellbooks:ice_magic")),
            Map.entry("twilightforest:falling_ice", key("irons_spellbooks:ice_magic")),
            Map.entry("twilightforest:frozen", key("irons_spellbooks:ice_magic")),
            Map.entry("twilightforest:snowball_fight", key("irons_spellbooks:ice_magic")),

            // ================= 3. 铁魔法：雷电 (lightning_magic) =================
            Map.entry("cataclysm:lightning", key("irons_spellbooks:lightning_magic")),
            Map.entry("cataclysm:emp", key("irons_spellbooks:lightning_magic")),
            Map.entry("cataclysm:storm_bringer", key("irons_spellbooks:lightning_magic")),

            // ================= 4. 铁魔法：鲜血 (blood_magic) =================
            Map.entry("apothic_attributes:bleeding", key("irons_spellbooks:blood_magic")),
            Map.entry("l2complements:bleed", key("irons_spellbooks:blood_magic")),
            Map.entry("twilightforest:lifedrain", key("irons_spellbooks:blood_magic")),

            // ================= 5. 铁魔法：自然 (nature_magic) =================
            Map.entry("neoforge:poison", key("irons_spellbooks:nature_magic")),
            Map.entry("twilightforest:acid_rain", key("irons_spellbooks:nature_magic")),
            Map.entry("twilightforest:stale_sandwich", key("irons_spellbooks:nature_magic")),

            // ================= 6. 铁魔法：末影 (ender_magic) =================
            Map.entry("twilightforest:lich_bolt", key("irons_spellbooks:ender_magic")),
            Map.entry("twilightforest:twilight_scepter", key("irons_spellbooks:ender_magic")),

            // ================= 7. 铁魔法：唤魔 (evocation_magic) =================
            Map.entry("l2complements:emerald", key("irons_spellbooks:evocation_magic")),
            Map.entry("twilightforest:lost_words", key("irons_spellbooks:evocation_magic")),
            Map.entry("twilightforest:schooled", key("irons_spellbooks:evocation_magic")),

            // ================= 8. 铁魔法：邪术 (eldritch_magic) =================
            Map.entry("apotheosis:psychic", key("irons_spellbooks:eldritch_magic")),
            Map.entry("cataclysm:abyssal_burn", key("irons_spellbooks:eldritch_magic")),
            Map.entry("cataclysm:maledictio_magicae", key("irons_spellbooks:eldritch_magic")),
            Map.entry("l2hostility:killer_aura", key("irons_spellbooks:eldritch_magic")),
            Map.entry("twilightforest:ominous_fire", key("irons_spellbooks:eldritch_magic")),

            // ================= 9. 铁魔法附属：幽魂 / 对称 / 音律 =================
            Map.entry("cataclysm:maledictio_anima", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("l2complements:soul_flame", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("twilightforest:haunt", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("cataclysm:sword_dance", key("iss_magicfromtheeast:symmetry_magic")),
            Map.entry("golemdungeons:echo_attack", key("tunes_n_tomes:melody_magic")),
            Map.entry("modulargolems:echo_attack", key("tunes_n_tomes:melody_magic")),

            // ================= 10. 原版：近战、弹射物与环境 =================
            Map.entry("bookshelf:fake_player", key("minecraft:player_attack")),
            Map.entry("cataclysm:shredder", key("minecraft:player_attack")),

            Map.entry("cataclysm:maledictio", key("minecraft:mob_attack")),
            Map.entry("cataclysm:penetrate", key("minecraft:mob_attack")),
            Map.entry("modulargolems:bacon_attack", key("minecraft:mob_attack")),
            Map.entry("takesapillage:legioner_slay", key("minecraft:mob_attack")),
            Map.entry("takesapillage:skirmisher_hack", key("minecraft:mob_attack")),
            Map.entry("twilightforest:ant", key("minecraft:mob_attack")),
            Map.entry("twilightforest:axing", key("minecraft:mob_attack")),
            Map.entry("twilightforest:clamped", key("minecraft:mob_attack")),
            Map.entry("twilightforest:hydra_bite", key("minecraft:mob_attack")),
            Map.entry("twilightforest:leaf_brain", key("minecraft:mob_attack")),
            Map.entry("twilightforest:slam", key("minecraft:mob_attack")),
            Map.entry("twilightforest:slider", key("minecraft:mob_attack")),

            Map.entry("cataclysm:maledictio_sagitta", key("minecraft:arrow")),
            Map.entry("takesapillage:archer_snipe", key("minecraft:arrow")),

            Map.entry("cataclysm:player_ceraunus", key("minecraft:trident")),

            Map.entry("twilightforest:moonworm", key("minecraft:mob_projectile")),
            Map.entry("twilightforest:thrown_axe", key("minecraft:mob_projectile")),
            Map.entry("twilightforest:thrown_block", key("minecraft:mob_projectile")),
            Map.entry("twilightforest:thrown_pickaxe", key("minecraft:mob_projectile")),

            Map.entry("twilightforest:hydra_mortar", key("minecraft:explosion")),
            Map.entry("twilightforest:reactor", key("minecraft:explosion")),

            Map.entry("l2hostility:reflect", key("minecraft:thorns")),
            Map.entry("twilightdelight:thorn_rose_tea", key("minecraft:thorns")),
            Map.entry("twilightforest:knightmetal", key("minecraft:thorns")),
            Map.entry("twilightforest:spiked", key("minecraft:thorns")),

            Map.entry("dungeonnowloading:spike", key("minecraft:cactus")),
            Map.entry("twilightforest:thorns", key("minecraft:cactus")),
            Map.entry("twilightforest:oreberry", key("minecraft:sweet_berry_bush")),
            Map.entry("farmersdelight:stove_burn", key("minecraft:hot_floor")),
            Map.entry("endersdelight:watered", key("minecraft:drown")),
            Map.entry("twilightforest:squish", key("minecraft:falling_block")),
            Map.entry("twilightforest:yeeted", key("minecraft:fall")),

            // ================= 11. 虚空伤害 / 机制真伤 (out_of_world) =================
            Map.entry("apotheosis:execute", key("minecraft:out_of_world")),
            Map.entry("apothic_attributes:current_hp_damage", key("minecraft:out_of_world")),
            Map.entry("apothic_enchanting:corrupted", key("minecraft:out_of_world")),
            Map.entry("bosses_of_mass_destruction:shield_piercing", key("minecraft:out_of_world")),
            Map.entry("incendium:sacrifice", key("minecraft:out_of_world")),
            Map.entry("l2complements:life_sync", key("minecraft:out_of_world")),
            Map.entry("l2complements:void_eye", key("minecraft:out_of_world")),
            Map.entry("twilightforest:expired", key("minecraft:out_of_world")),
            Map.entry("twilightforest:failed_challenge", key("minecraft:out_of_world")),

            // ================= 12. 铁魔法及其附属的非 SpellDamageSource 衍生伤害 =================
            // 注意：原生 SpellDamageSource 会在 remap() 入口直接放行，不会丢失学派上下文
            Map.entry("irons_spellbooks:fire_field", key("irons_spellbooks:fire_magic")),
            Map.entry("irons_spellbooks:blood_cauldron", key("irons_spellbooks:blood_magic")),
            Map.entry("irons_spellbooks:dragon_breath_pool", key("irons_spellbooks:ender_magic")),
            Map.entry("irons_spellbooks:poison_cloud", key("irons_spellbooks:nature_magic")),
            Map.entry("irons_spellbooks:heartstop", key("minecraft:out_of_world")),
            Map.entry("iss_magicfromtheeast:soul_damage", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("iss_magicfromtheeast:soul_weapons_bonus", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("iss_magicfromtheeast:spirit_arrow", key("iss_magicfromtheeast:spirit_magic")),
            Map.entry("iss_magicfromtheeast:pendant_reflect", key("iss_magicfromtheeast:symmetry_magic")),

            // ================= 13. L2 穿甲/破无敌帧变体（会覆盖穿甲机制，按需保留或删除） =================
            Map.entry("l2archery:arrow-bypass_armor", key("minecraft:arrow")),
            Map.entry("l2archery:arrow-bypass_armor-bypass_magic", key("minecraft:arrow")),
            Map.entry("l2archery:arrow-bypass_invul", key("minecraft:arrow")),
            Map.entry("l2archery:arrow-bypass_magic", key("minecraft:arrow")),

            Map.entry("l2damagetracker:mob_attack-bypass_armor", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_armor-bypass_cooldown", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_armor-bypass_cooldown-bypass_magic", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_armor-bypass_magic", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_cooldown", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_cooldown-bypass_magic", key("minecraft:mob_attack")),
            Map.entry("l2damagetracker:mob_attack-bypass_magic", key("minecraft:mob_attack")),

            Map.entry("l2damagetracker:player_attack-bypass_armor", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_armor-bypass_cooldown", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_armor-bypass_cooldown-bypass_magic", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_armor-bypass_magic", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_cooldown", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_cooldown-bypass_magic", key("minecraft:player_attack")),
            Map.entry("l2damagetracker:player_attack-bypass_magic", key("minecraft:player_attack")),

            Map.entry("l2weaponry:mob_attack-bypass_armor-bypass_magic-no_anger", key("minecraft:mob_attack")),
            Map.entry("l2weaponry:mob_attack-bypass_armor-no_anger", key("minecraft:mob_attack")),
            Map.entry("l2weaponry:mob_attack-bypass_magic-no_anger", key("minecraft:mob_attack")),
            Map.entry("l2weaponry:mob_attack-no_anger", key("minecraft:mob_attack")),

            Map.entry("l2weaponry:player_attack-bypass_armor-bypass_magic-no_anger", key("minecraft:player_attack")),
            Map.entry("l2weaponry:player_attack-bypass_armor-no_anger", key("minecraft:player_attack")),
            Map.entry("l2weaponry:player_attack-bypass_magic-no_anger", key("minecraft:player_attack")),
            Map.entry("l2weaponry:player_attack-no_anger", key("minecraft:player_attack")),

            Map.entry("l2weaponry:trident-bypass_armor", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-bypass_magic", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-bypass_magic-no_anger", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-bypass_magic-no_anger-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-bypass_magic-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-no_anger", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-no_anger-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_armor-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_magic", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_magic-no_anger", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_magic-no_anger-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-bypass_magic-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-no_anger", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-no_anger-no_projectile", key("minecraft:trident")),
            Map.entry("l2weaponry:trident-no_projectile", key("minecraft:trident"))
    );

    private static ResourceKey<DamageType> key(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(id));
    }

    public static DamageSource remap(ServerLevel level, DamageSource source) {
        // 严禁剥离原生 SpellDamageSource，否则会破坏 isNativeSpell 判断与 NativeSpellDamageContext
        if (source instanceof SpellDamageSource) {
            return source;
        }

        String id = source.typeHolder()
                .unwrapKey()
                .map(k -> k.location().toString())
                .orElse(null);
        if (id == null) {
            return source;
        }

        ResourceKey<DamageType> targetKey = REMAP.get(id);
        if (targetKey == null) {
            return source;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(targetKey)
                .map(holder -> new DamageSource(
                        holder,
                        source.getDirectEntity(),
                        source.getEntity(),
                        source.getSourcePosition()
                ))
                .orElse(source);
    }
}