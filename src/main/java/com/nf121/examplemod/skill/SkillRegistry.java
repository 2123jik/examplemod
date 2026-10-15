package com.nf121.examplemod.skill;

import com.nf121.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber
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

    @SubscribeEvent
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SkillRegistry::init);
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

    /**
     * 辅助方法：快速获取伤害转化属性注册名 (如 "examplemod:convert_physical_to_fire")
     */
    private static String conv(String from, String to) {
        return ExampleMod.MOD_ID + ":convert_" + from + "_to_" + to;
    }

    private static void loadDefaults() {
        // =========================================================================
        // 0. 核心根节点 (Root) - 原版保留
        // =========================================================================
        register(new SkillNode("root", 1)
                .setTitle(Component.literal("宿命觉醒"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/nether_star"))
                .addBonus("minecraft:generic.max_health", 2.0, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 1：近战重装 / 先锋狂战 / 魔剑士 / 泰坦巨神 (Vanguard & Titan)
        // =========================================================================
        // 一阶：体魄与强击 (原版保留)
        register(new SkillNode("warrior_vigor", 5, "root")
                .setTitle(Component.literal("强韧体魄"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/golden_apple"))
                .addBonus("minecraft:generic.max_health", 2.0, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("warrior_might", 5, "warrior_vigor")
                .setTitle(Component.literal("狂力挥砍"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_sword"))
                .addBonus("minecraft:generic.attack_damage", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.sweeping_damage_ratio", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：铁壁防御 / 重兵专精 (原版保留)
        register(new SkillNode("warrior_defense", 3, "warrior_vigor")
                .setTitle(Component.literal("重装铠甲"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_chestplate"))
                .addBonus("minecraft:generic.armor", 1.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.armor_toughness", 0.5, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("warrior_parry", 3, "warrior_defense")
                .setTitle(Component.literal("坚守盾阵"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/shield"))
                .addBonus("l2weaponry:shield_defense", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("l2weaponry:reflect_time", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.knockback_resistance", 0.10, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("warrior_heavy_strike", 3, "warrior_might")
                .setTitle(Component.literal("破甲重锤"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_axe"))
                .addBonus("apothic_attributes:armor_pierce", 0.05, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:charge_time", -0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 磐石之躯：针对防击退、防爆炸击退、抗火烧时间
        register(new SkillNode("warrior_unyielding", 3, "warrior_parry")
                .setTitle(Component.literal("不动明王"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/anvil"))
                .addBonus("minecraft:generic.knockback_resistance", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.explosion_knockback_resistance", 0.20, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.burning_time", -0.15, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 泰坦血脉：体型放大、攻击击退大幅提升 (泰坦流)
        register(new SkillNode("warrior_colossus", 3, "warrior_heavy_strike")
                .setTitle(Component.literal("泰坦身躯"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_block"))
                .addBonus("minecraft:generic.scale", 0.08, AttributeModifier.Operation.ADD_VALUE) // 体型变大 8% 每级
                .addBonus("minecraft:generic.attack_knockback", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("modulargolems:golem_sweep", 0.15, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士质变专精 A：熔炎战刃 (物转火) (原版保留) ---
        register(new SkillNode("spec_physical_to_fire", 3, "warrior_heavy_strike")
                .setTitle(Component.literal("专精：熔炎战刃"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/magma_cream"))
                .addBonus(conv("physical", "fire"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:fire_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:fire_damage", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：强化火系] 业火焚身：附带引燃时长与纯火伤提升
        register(new SkillNode("warrior_blazing_fury", 3, "spec_physical_to_fire")
                .setTitle(Component.literal("烈焰狂涌"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/blaze_rod"))
                .addBonus("artifacts:generic.attack_burning_duration", 2.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("l2damagetracker:fire_damage", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士质变专精 B：风暴雷震 (物转雷) (原版保留) ---
        register(new SkillNode("spec_physical_to_lightning", 3, "warrior_heavy_strike")
                .setTitle(Component.literal("专精：风暴雷震"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/lightning_rod"))
                .addBonus(conv("physical", "lightning"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.attack_speed", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:lightning_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // [新增：强化雷系] 雷霆怒击：纯雷电伤害增强
        register(new SkillNode("warrior_thunder_crash", 3, "spec_physical_to_lightning")
                .setTitle(Component.literal("雷芒崩裂"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/copper_ingot"))
                .addBonus("l2damagetracker:lightning_damage", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:crit_damage", 0.12, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士质变专精 C：猩红饮血 (物转血 + 斩杀) (原版保留) ---
        register(new SkillNode("spec_physical_to_blood", 3, "warrior_might")
                .setTitle(Component.literal("专精：猩红死狂"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/fermented_spider_eye"))
                .addBonus(conv("physical", "blood"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:life_steal", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:current_hp_damage", 0.02, AttributeModifier.Operation.ADD_VALUE));

        // [新增：吸血连携] 鲜血契约：持续生命回复 + 血魔法威力
        register(new SkillNode("warrior_blood_berserk", 3, "spec_physical_to_blood")
                .setTitle(Component.literal("狂血沸腾"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/redstone"))
                .addBonus("l2damagetracker:regen", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:blood_spell_power", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士防御专精 D：暮色不灭幻盾 (绝对防御) (原版保留) ---
        register(new SkillNode("spec_tank_fortress", 3, "warrior_parry")
                .setTitle(Component.literal("专精：不灭堡垒"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/turtle_helmet"))
                .addBonus("l2damagetracker:damage_reduction", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("twilightforest:max_shield_strength", 1.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.invincibility_ticks", 2.0, AttributeModifier.Operation.ADD_VALUE));

        // [新增：强化免伤] 刚毅磐石：吸收盾与伤害吸收
        register(new SkillNode("warrior_stone_bulwark", 3, "spec_tank_fortress")
                .setTitle(Component.literal("刚毅磐石"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/shield"))
                .addBonus("l2damagetracker:damage_absorption", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.max_absorption", 4.0, AttributeModifier.Operation.ADD_VALUE));

        // 终极终赋：不灭战神 (原版保留)
        register(new SkillNode("warrior_mastery", 1, "warrior_might")
                .setTitle(Component.literal("终赋：不灭战神"))
                .setSpell(SpellRegistry.FORTIFY_SPELL.get())
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/netherite_chestplate"))
                .addBonus("minecraft:generic.max_health", 8.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:armor_shred", 0.15, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 2：游侠敏捷 / 刺客微操 / 虚空行者 (Ranger & Shadow Assassin)
        // =========================================================================
        // 一阶：轻盈疾行 (原版保留)
        register(new SkillNode("ranger_agility", 5, "root")
                .setTitle(Component.literal("灵巧步伐"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/leather_boots"))
                .addBonus("minecraft:generic.movement_speed", 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addBonus("artifacts:generic.sprinting_speed", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：神射与穿透 (原版保留)
        register(new SkillNode("ranger_archery", 5, "ranger_agility")
                .setTitle(Component.literal("精准拉弓"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bow"))
                .addBonus("apothic_attributes:arrow_damage", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:draw_speed", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:arrow_velocity", 0.05, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：幽影闪避 (原版保留)
        register(new SkillNode("ranger_dodge", 3, "ranger_agility")
                .setTitle(Component.literal("虚实残影"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/feather"))
                .addBonus("apothic_attributes:dodge_chance", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 潜行刺客：潜行移速 + 防滑冰 + 冲刺高跳
        register(new SkillNode("ranger_stalker", 3, "ranger_dodge")
                .setTitle(Component.literal("幽影匿迹"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/phantom_membrane"))
                .addBonus("minecraft:player.sneaking_speed", 0.20, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.slip_resistance", 0.25, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.sprinting_step_height", 0.5, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 幻影微躯：缩小身形减少受击判定面 (刺客身法)
        register(new SkillNode("ranger_tiny_frame", 3, "ranger_dodge")
                .setTitle(Component.literal("灵狐幻形"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/glow_berries"))
                .addBonus("minecraft:generic.scale", -0.05, AttributeModifier.Operation.ADD_VALUE) // 身型缩小 5%
                .addBonus("irons_lib:dodge_chance", 0.05, AttributeModifier.Operation.ADD_VALUE));

        // 三阶：弱点狙杀 (原版保留)
        register(new SkillNode("ranger_crit", 3, "ranger_archery")
                .setTitle(Component.literal("致命弱点"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/spectral_arrow"))
                .addBonus("apothic_attributes:crit_chance", 0.05, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:crit_damage", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:additional_critical_damage", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // [新增：强化弓弩] 重弩轰鸣：L2 弓箭强度 + 弹射物穿甲
        register(new SkillNode("ranger_heavy_ballistics", 3, "ranger_crit")
                .setTitle(Component.literal("重弩裂甲"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/arrow"))
                .addBonus("l2damagetracker:bow_strength", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:projectile_damage", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:prot_pierce", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 游侠质变专精 E：凛冬霜矢 (物转冰) (原版保留) ---
        register(new SkillNode("spec_physical_to_ice", 3, "ranger_archery")
                .setTitle(Component.literal("专精：极地寒矢"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/snowball"))
                .addBonus(conv("physical", "ice"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:ice_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:cold_damage", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：强化冰系] 永冻寒狱：冰冻纯伤加成
        register(new SkillNode("ranger_permafrost", 3, "spec_physical_to_ice")
                .setTitle(Component.literal("永冻刺骨"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/packed_ice"))
                .addBonus("l2damagetracker:freezing_damage", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:ice_magic_resist", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // --- 游侠质变专精 F：幽冥裂隙 (物转末影) (原版保留) ---
        register(new SkillNode("spec_physical_to_ender", 3, "ranger_dodge")
                .setTitle(Component.literal("专精：虚空掠影"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/ender_pearl"))
                .addBonus(conv("physical", "ender"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:ender_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:dodge_chance", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // 终极终赋：鹰眼猎王 (原版保留)
        register(new SkillNode("ranger_mastery", 1, "ranger_crit")
                .setTitle(Component.literal("终赋：天际鹰眼"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/crossbow"))
                .addBonus("apothic_attributes:crit_chance", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:prot_pierce", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.entity_interaction_range", 1.0, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 3：奥术源泉 / 元素融合 / 东方仙道 (Arcane, Shifter & Taoist)
        // =========================================================================
        // 一阶：魔力与回涌 (原版保留)
        register(new SkillNode("magic_focus", 5, "root")
                .setTitle(Component.literal("灵能涌动"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/lapis_lazuli"))
                .addBonus("irons_spellbooks:max_mana", 25.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:mana_regen", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：奥术专注与移速 (原版保留)
        register(new SkillNode("magic_flow", 5, "magic_focus")
                .setTitle(Component.literal("施法专注"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/blaze_powder"))
                .addBonus("irons_spellbooks:spell_power", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:cast_time_reduction", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:casting_movespeed", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("magic_cdr", 3, "magic_focus")
                .setTitle(Component.literal("时光灵契"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/clock"))
                .addBonus("irons_spellbooks:cooldown_reduction", 0.05, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:cooldown_reduction", 0.05, AttributeModifier.Operation.ADD_VALUE));

        // [新增：法术纯伤] 魔能破灭：L2 魔法直接增伤与全系抗性
        register(new SkillNode("magic_raw_destruction", 3, "magic_flow")
                .setTitle(Component.literal("源力轰鸣"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/dragon_breath"))
                .addBonus("l2damagetracker:magic_damage", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:spell_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 1：等离子融爆 (火转雷) (原版保留) ---
        register(new SkillNode("spec_fire_to_lightning", 3, "magic_flow")
                .setTitle(Component.literal("奥术裂变：等离子电涌"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/fire_charge"))
                .addBonus(conv("fire", "lightning"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:lightning_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 2：仙灵归真 (自然转灵能 - ISS Magic from the East) (原版保留) ---
        register(new SkillNode("spec_nature_to_spirit", 3, "magic_flow")
                .setTitle(Component.literal("仙法通灵：化境生魂"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/ghast_tear"))
                .addBonus(conv("nature", "spirit"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:spirit_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:spirit_magic_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 3：圣音咏叹 (神圣转旋律 - Tunes 'n Tomes) (原版保留) ---
        register(new SkillNode("spec_holy_to_melody", 3, "magic_flow")
                .setTitle(Component.literal("福音交响：天界咏叹"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/music_disc_wait"))
                .addBonus(conv("holy", "melody"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("tunes_n_tomes:melody_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("tunes_n_tomes:melody_magic_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 异端深渊：古神秘术 (血转古神)
        register(new SkillNode("spec_blood_to_eldritch", 3, "magic_flow")
                .setTitle(Component.literal("深渊呢喃：虚空异化"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/echo_shard"))
                .addBonus(conv("blood", "eldritch"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:eldritch_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:eldritch_magic_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 仙法沙海：狂沙奔流 (冰转沙暴 Dune - 东方仙道)
        register(new SkillNode("spec_ice_to_dune", 3, "magic_flow")
                .setTitle(Component.literal("仙法归寂：黄沙万仞"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/sand"))
                .addBonus(conv("ice", "dune"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:dune_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:dune_magic_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 召唤唤魔：唤魔系专精 (物转召唤唤魔)
        register(new SkillNode("spec_physical_to_evocation", 3, "magic_flow")
                .setTitle(Component.literal("役灵秘术：唤魔天谴"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/emerald"))
                .addBonus(conv("physical", "evocation"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:evocation_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:evocation_magic_resist", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 唤灵统御：召唤与暮色使徒 (原版保留) ---
        register(new SkillNode("magic_summoner", 3, "magic_flow")
                .setTitle(Component.literal("统御军势"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bone"))
                .addBonus("irons_spellbooks:summon_damage", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // --- 终极核心仙法：万法归一 (物转对称 - 东方仙道至高) (原版保留) ---
        register(new SkillNode("spec_physical_to_symmetry", 1, "magic_flow")
                .setTitle(Component.literal("天道终赋：万象归元"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard"))
                .addBonus(conv("physical", "symmetry"), 0.40, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:symmetry_spell_power", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("iss_magicfromtheeast:symmetry_magic_resist", 0.20, AttributeModifier.Operation.ADD_VALUE));

        // 终极法师终赋：真理大魔导 (原版保留)
        register(new SkillNode("magic_archmage", 1, "magic_flow")
                .setTitle(Component.literal("终赋：全知魔导"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/enchanted_book"))
                .addBonus("irons_spellbooks:max_mana", 100.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:spell_power", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:cooldown_reduction", 0.10, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 4：圣辉愈合 / 护体生机 / 神恩壁垒 (Paladin & Living Grace)
        // =========================================================================
        // (原版保留)
        register(new SkillNode("holy_vitality", 5, "root")
                .setTitle(Component.literal("圣灵甘霖"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/glistering_melon_slice"))
                .addBonus("apothic_attributes:healing_received", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_lib:healing_received", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:nature_heal", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 质变专精 G：圣裁誓约 (物转神圣) (原版保留)
        register(new SkillNode("spec_physical_to_holy", 3, "holy_vitality")
                .setTitle(Component.literal("专精：黎明圣裁"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/gold_ingot"))
                .addBonus(conv("physical", "holy"), 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:holy_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 过量护盾与幽魂生机 (原版保留)
        register(new SkillNode("holy_overheal", 3, "holy_vitality")
                .setTitle(Component.literal("过量圣佑"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying"))
                .addBonus("apothic_attributes:overheal", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:ghost_health", 2.0, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 圣辉制裁：削减敌方抗性 + 自身神圣抗性
        register(new SkillNode("holy_retribution", 3, "spec_physical_to_holy")
                .setTitle(Component.literal("神圣制裁"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/glowstone_dust"))
                .addBonus("irons_spellbooks:holy_magic_resist", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:prot_shred", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 圣灵不朽：大幅提升幽魂血量 + 减免受到的魔法伤害
        register(new SkillNode("holy_divine_shield", 1, "holy_overheal")
                .setTitle(Component.literal("终赋：圣灵庇护"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/golden_apple"))
                .addBonus("apothic_attributes:ghost_health", 6.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("l2damagetracker:magic_damage", -0.10, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 5：探索漫游 / 工匠采金 / 遗物大师 (Explorer, Artisan & Trickster)
        // =========================================================================
        // (原版保留)
        register(new SkillNode("util_survival", 5, "root")
                .setTitle(Component.literal("荒野求生"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bread"))
                .addBonus("minecraft:generic.safe_fall_distance", 1.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.eating_speed", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:eat_speed", 0.15, AttributeModifier.Operation.ADD_VALUE));

        // [新增：暴饮药剂] 牛饮大师：快速喝药
        register(new SkillNode("util_potions", 3, "util_survival")
                .setTitle(Component.literal("药剂快饮"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/potion"))
                .addBonus("artifacts:generic.drinking_speed", 0.25, AttributeModifier.Operation.ADD_VALUE));

        // (原版保留)
        register(new SkillNode("util_mining", 5, "util_survival")
                .setTitle(Component.literal("巨力开凿"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_pickaxe"))
                .addBonus("minecraft:player.block_break_speed", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.block_interaction_range", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:mining_speed", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 深潜矿工：水下挖掘 + 潜水含氧量
        register(new SkillNode("util_deep_digger", 3, "util_mining")
                .setTitle(Component.literal("地脉采矿"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/diamond_pickaxe"))
                .addBonus("minecraft:player.submerged_mining_speed", 0.35, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.mining_efficiency", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.oxygen_bonus", 50.0, AttributeModifier.Operation.ADD_VALUE));

        // (原版保留)
        register(new SkillNode("util_traversal", 3, "util_survival")
                .setTitle(Component.literal("畅游四海"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/rabbit_foot"))
                .addBonus("minecraft:generic.step_height", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("neoforge:swim_speed", 0.20, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.water_movement_efficiency", 0.25, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 低重力轻功：跳跃力增强 + 重力减轻 (羽落跳跃)
        register(new SkillNode("util_feather_step", 3, "util_traversal")
                .setTitle(Component.literal("梯云纵"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/feather"))
                .addBonus("minecraft:generic.jump_strength", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.gravity", -0.15, AttributeModifier.Operation.ADD_VALUE) // 降低重力 15%
                .addBonus("minecraft:generic.fall_damage_multiplier", -0.15, AttributeModifier.Operation.ADD_VALUE));

        // [新增：盲点补全] 鞘翅与坐骑大师：强化飞行与骑乘
        register(new SkillNode("util_aeronaut", 3, "util_traversal")
                .setTitle(Component.literal("御风飞骑"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/elytra"))
                .addBonus("apothic_attributes:elytra_flight", 0.20, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.mount_speed", 0.15, AttributeModifier.Operation.ADD_VALUE));


        // (原版保留)
        register(new SkillNode("util_wisdom", 3, "util_survival")
                .setTitle(Component.literal("智者洞察"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/experience_bottle"))
                .addBonus("apothic_attributes:experience_gained", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:player.entity_experience", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.luck", 1.0, AttributeModifier.Operation.ADD_VALUE));

        // [新增：附魔与名望] 神秘学者：神化 Eterna 附魔上限 + 村民好感 + 名字标签超距感知
        register(new SkillNode("util_enchanter", 3, "util_wisdom")
                .setTitle(Component.literal("奥秘通识"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/enchanted_book"))
                .addBonus("apothic_enchanting:max_eterna", 5.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:player.villager_reputation", 50.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("neoforge:nametag_distance", 16.0, AttributeModifier.Operation.ADD_VALUE));

        // [新增：自选挑战硬核] 逆境修罗：L2 Hostility 增加难度与僵尸围攻率，但获得极高全局属性汇报
        register(new SkillNode("util_hostility_challenge", 1, "util_wisdom")
                .setTitle(Component.literal("终赋：修罗试炼"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/wither_skeleton_skull"))
                .addBonus("l2hostility:extra_difficulty", 10.0, AttributeModifier.Operation.ADD_VALUE) // 难度陡增
                .addBonus("minecraft:generic.luck", 5.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:experience_gained", 0.50, AttributeModifier.Operation.ADD_VALUE));
    }
}
