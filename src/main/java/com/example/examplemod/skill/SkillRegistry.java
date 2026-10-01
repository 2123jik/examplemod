package com.example.examplemod.skill;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillRegistry {
    private static final Map<String, SkillNode> REGISTRY = new LinkedHashMap<>();
    public static final Map<String, SkillNode> NODES = Collections.unmodifiableMap(REGISTRY);

    public static void register(SkillNode node) {
        REGISTRY.put(node.getId(), node);
    }

    public static void remove(String id) {
        REGISTRY.remove(id);
    }

    public static void clear() {
        REGISTRY.clear();
    }

    public static void init() {
        reload();
    }

    /**
     * 重载技能树：优先从 config/examplemod/skills.json 读取
     * 若不存在则装载默认节点，并自动输出一份初始 JSON 供游戏内可视化编辑
     */
    public static void reload() {
        REGISTRY.clear();

        List<SkillNode> loadedNodes = SkillNodeData.loadAll();
        if (!loadedNodes.isEmpty()) {
            for (SkillNode node : loadedNodes) {
                register(node);
            }
            return;
        }

        // 首次启动且无配置文件时，加载默认预设
        loadDefaults();

        // 自动计算默认初始布局
        SkillLayoutEngine.computeLayout(REGISTRY.values());

        // 首次自动导出为 JSON 文件，后续直接由此文件驱动
        SkillNodeData.saveAll(REGISTRY.values());
    }

    private static void loadDefaults()
    {

        // 根节点
        register(new SkillNode("1", 5)
                .addBonus("minecraft:generic.max_health", 2, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("symmetry", 5, "1")
                .addBonus("iss_magicfromtheeast:symmetry_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("spirit", 5, "1")
                .addBonus("iss_magicfromtheeast:spirit_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("dune", 5, "1")
                .addBonus("iss_magicfromtheeast:dune_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("fire", 5, "1")
                .addBonus("irons_spellbooks:fire_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("ice", 5, "1")
                .addBonus("irons_spellbooks:ice_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("lightning", 5, "1")
                .addBonus("irons_spellbooks:lightning_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("holy", 5, "1")
                .addBonus("irons_spellbooks:holy_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("ender", 5, "1")
                .addBonus("irons_spellbooks:ender_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("blood", 5, "1")
                .addBonus("irons_spellbooks:blood_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));
// === 怪异系（Eldritch）核心质变天赋挂载 ===

// 1. 魔能爆质变：异界奇点
        register(new SkillNode("keystone_eldritch_singularity", 1, "eldritch")
                .setSpell(SpellRegistry.ELDRITCH_BLAST_SPELL.get())
                .addBonus("examplemod:eldritch_singularity", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 音波共鸣质变：音爆裂解
        register(new SkillNode("keystone_sonic_resonance_shatter", 1, "eldritch")
                .setSpell(SpellRegistry.SONIC_BOOM_SPELL.get())
                .addBonus("examplemod:sonic_resonance_shatter", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 念动力质变：动能陨落
        register(new SkillNode("keystone_kinetic_annihilation", 1, "eldritch")
                .setSpell(SpellRegistry.TELEKINESIS_SPELL.get())
                .addBonus("examplemod:kinetic_annihilation", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 幽匿触手质变：盲目巢群
        register(new SkillNode("keystone_sculk_hive_infestation", 1, "eldritch")
                .setSpell(SpellRegistry.SCULK_TENTACLES_SPELL.get())
                .addBonus("examplemod:sculk_hive_infestation", 1.0, AttributeModifier.Operation.ADD_VALUE));
        // === 在 blood 分支后挂载机制质变核心节点 ===

// 1. 血步质变：血影断魂
        register(new SkillNode("keystone_blood_step_severance", 1, "blood")
                .setSpell(SpellRegistry.BLOOD_STEP_SPELL.get())
                .addBonus("examplemod:blood_step_severance", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 针灸质变：血梏钉刑
        register(new SkillNode("keystone_acupuncture_crucifixion", 1, "blood")
                .setSpell(SpellRegistry.ACUPUNCTURE_SPELL.get())
                .addBonus("examplemod:acupuncture_crucifixion", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 血之斩击质变：血月十字
        register(new SkillNode("keystone_blood_slash_cross", 1, "blood")
                .setSpell(SpellRegistry.BLOOD_SLASH_SPELL.get())
                .addBonus("examplemod:blood_slash_cross", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 凋零之首质变：死疫齐射
        register(new SkillNode("keystone_wither_barrage", 1, "blood")
                .setSpell(SpellRegistry.WITHER_SKULL_SPELL.get())
                .addBonus("examplemod:wither_barrage", 1.0, AttributeModifier.Operation.ADD_VALUE));
        register(new SkillNode("nature", 5, "1")
                .addBonus("irons_spellbooks:nature_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));
// ==================== 末影系核心质变天赋节点 ====================

// 1. 瞬移术质变：虚空置换
        register(new SkillNode("keystone_teleport_void_swap", 1, "ender")
                .setSpell(SpellRegistry.TELEPORT_SPELL.get())
                .addBonus("examplemod:teleport_void_swap", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 影斩质变：瞬狱影杀阵
        register(new SkillNode("keystone_shadow_slash_omnislash", 1, "ender")
                .setSpell(SpellRegistry.SHADOW_SLASH.get())
                .addBonus("examplemod:shadow_slash_omnislash", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 魔法飞弹质变：浮游星芒
        register(new SkillNode("keystone_magic_missile_swarm", 1, "ender")
                .setSpell(SpellRegistry.MAGIC_MISSILE_SPELL.get())
                .addBonus("examplemod:magic_missile_swarm", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 黑洞质变：坍缩超新星
        register(new SkillNode("keystone_black_hole_supernova", 1, "ender")
                .setSpell(SpellRegistry.BLACK_HOLE_SPELL.get())
                .addBonus("examplemod:black_hole_supernova", 1.0, AttributeModifier.Operation.ADD_VALUE));
        register(new SkillNode("evocation", 5, "1")
                .addBonus("irons_spellbooks:evocation_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // ==================== 唤魔系（Evocation）机制质变大点 ====================

// 1. 替罪羊：命元代偿
        register(new SkillNode("keystone_scapegoat_soul_surrogate", 1, "evocation")
                .setSpell(SpellRegistry.SCAPEGOAT_SPELL.get())
                .addBonus("examplemod:scapegoat_soul_surrogate", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 魔法护盾：偏斜过载
        register(new SkillNode("keystone_shield_deflection_overload", 1, "evocation")
                .setSpell(SpellRegistry.SHIELD_SPELL.get())
                .addBonus("examplemod:shield_deflection_overload", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 尖牙结界：断罪咬合
        register(new SkillNode("keystone_fang_ward_shred", 1, "evocation")
                .setSpell(SpellRegistry.FANG_WARD_SPELL.get())
                .addBonus("examplemod:fang_ward_shred", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 迟缓术：时滞停滞
        register(new SkillNode("keystone_slow_chrono_stagnation", 1, "evocation")
                .setSpell(SpellRegistry.SLOW_SPELL.get())
                .addBonus("examplemod:slow_chrono_stagnation", 1.0, AttributeModifier.Operation.ADD_VALUE));
// ==================== 火焰系（Fire）机制质变大点 ====================

// 1. 烈焰风暴：过热超载
        register(new SkillNode("keystone_blaze_storm_overheat", 1, "fire")
                .setSpell(SpellRegistry.BLAZE_STORM_SPELL.get())
                .addBonus("examplemod:blaze_storm_overheat", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 烈焰突进：劫火回旋充能
        register(new SkillNode("keystone_burning_dash_ignition", 1, "fire")
                .setSpell(SpellRegistry.BURNING_DASH_SPELL.get())
                .addBonus("examplemod:burning_dash_ignition", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 火球术：超临界聚爆
        register(new SkillNode("keystone_fireball_supercritical", 1, "fire")
                .setSpell(SpellRegistry.FIREBALL_SPELL.get())
                .addBonus("examplemod:fireball_supercritical", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 热浪涌动：灰烬焦土领域
        register(new SkillNode("keystone_heat_surge_cinder_domain", 1, "fire")
                .setSpell(SpellRegistry.HEAT_SURGE_SPELL.get())
                .addBonus("examplemod:heat_surge_cinder_domain", 1.0, AttributeModifier.Operation.ADD_VALUE));

// ==================== 神圣系（Holy）机制质变大点 ====================

// 1. 神圣重击：圣裁审判
        register(new SkillNode("keystone_divine_smite_judgment", 1, "holy")
                .setSpell(SpellRegistry.DIVINE_SMITE_SPELL.get())
                .addBonus("examplemod:divine_smite_judgment", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 治疗过量：过量光佑
        register(new SkillNode("keystone_blessing_overheal_aegis", 1, "holy")
                .setSpell(SpellRegistry.BLESSING_OF_LIFE_SPELL.get())
                .addBonus("examplemod:blessing_overheal_aegis", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 日光射线：黎明耀斑
        register(new SkillNode("keystone_sunbeam_dawn_flare", 1, "holy")
                .setSpell(SpellRegistry.SUNBEAM_SPELL.get())
                .addBonus("examplemod:sunbeam_dawn_flare", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 强化术：殉道光环
        register(new SkillNode("keystone_fortify_martyr_aegis", 1, "holy")
                .setSpell(SpellRegistry.FORTIFY_SPELL.get())
                .addBonus("examplemod:fortify_martyr_aegis", 1.0, AttributeModifier.Operation.ADD_VALUE));

// ==================== 冰霜系（Ice）机制质变大点 ====================

// 1. 霜冻之踏：绝对零度雪崩
        register(new SkillNode("keystone_frost_step_avalanche", 1, "ice")
                .setSpell(SpellRegistry.FROST_STEP_SPELL.get())
                .addBonus("examplemod:frost_step_avalanche", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 冰刺术：极冰穿刺碎裂
        register(new SkillNode("keystone_ice_spikes_shatter", 1, "ice")
                .setSpell(SpellRegistry.ICE_SPIKES_SPELL.get())
                .addBonus("examplemod:ice_spikes_shatter", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 暴风雪：永冻冰渊
        register(new SkillNode("keystone_blizzard_permafrost", 1, "ice")
                .setSpell(SpellRegistry.BLIZZARD_SPELL.get())
                .addBonus("examplemod:blizzard_permafrost", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 冰封之棺：极寒涅槃
        register(new SkillNode("keystone_ice_tomb_absolute_zero", 1, "ice")
                .setSpell(SpellRegistry.ICE_TOMB_SPELL.get())
                .addBonus("examplemod:ice_tomb_absolute_zero", 1.0, AttributeModifier.Operation.ADD_VALUE));


        // ==================== 雷电系（Lightning）机制质变大点 ====================

// 1. 连锁闪电：高压电离超导
        register(new SkillNode("keystone_chain_lightning_superconductor", 1, "lightning")
                .setSpell(SpellRegistry.CHAIN_LIGHTNING_SPELL.get())
                .addBonus("examplemod:chain_lightning_superconductor", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 落雷术：电磁雷暴引信
        register(new SkillNode("keystone_lightning_bolt_emp", 1, "lightning")
                .setSpell(SpellRegistry.LIGHTNING_BOLT_SPELL.get())
                .addBonus("examplemod:lightning_bolt_emp", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 充能术：超频聚能
        register(new SkillNode("keystone_charge_overclock", 1, "lightning")
                .setSpell(SpellRegistry.CHARGE_SPELL.get())
                .addBonus("examplemod:charge_overclock", 1.0, AttributeModifier.Operation.ADD_VALUE));



// 1. 酸液法球：强酸蚀甲聚爆
        register(new SkillNode("keystone_acid_orb_corrosion", 1, "nature")
                .setSpell(SpellRegistry.ACID_ORB_SPELL.get())
                .addBonus("examplemod:acid_orb_corrosion", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 2. 缠绕术：绞杀荆棘断筋
        register(new SkillNode("keystone_root_strangle_rupture", 1, "nature")
                .setSpell(SpellRegistry.ROOT_SPELL.get())
                .addBonus("examplemod:root_strangle_rupture", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 3. 枯萎术：疫病生息共振
        register(new SkillNode("keystone_blight_pestilence", 1, "nature")
                .setSpell(SpellRegistry.BLIGHT_SPELL.get())
                .addBonus("examplemod:blight_pestilence", 1.0, AttributeModifier.Operation.ADD_VALUE));

// 4. 橡木之躯：远古荆棘古树
        register(new SkillNode("keystone_oakskin_ancient_thorns", 1, "nature")
                .setSpell(SpellRegistry.OAKSKIN_SPELL.get())
                .addBonus("examplemod:oakskin_ancient_thorns", 1.0, AttributeModifier.Operation.ADD_VALUE));
    }
}