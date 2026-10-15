package com.example.examplemod.skill;

import com.example.examplemod.ExampleMod;
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
        // 0. 核心根节点 (Root)
        // =========================================================================
        register(new SkillNode("root", 1)
                .setTitle(Component.literal("宿命觉醒"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/nether_star"))
                .addBonus("minecraft:generic.max_health", 2.0, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 1：近战重装 / 先锋狂战 / 魔剑士 (Vanguard & Spellblade)
        // =========================================================================
        // 一阶：体魄与强击
        register(new SkillNode("warrior_vigor", 5, "root")
                .setTitle(Component.literal("强韧体魄"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/golden_apple"))
                .addBonus("minecraft:generic.max_health", 2.0, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("warrior_might", 5, "warrior_vigor")
                .setTitle(Component.literal("狂力挥砍"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_sword"))
                .addBonus("minecraft:generic.attack_damage", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.sweeping_damage_ratio", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：铁壁防御 / 重兵专精
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
                .addBonus("cataclysm:charge_time", -0.08, AttributeModifier.Operation.ADD_VALUE)); // 减少蓄力时间

        // --- 战士质变专精 A：熔炎战刃 (物转火) ---
        register(new SkillNode("spec_physical_to_fire", 3, "warrior_heavy_strike")
                .setTitle(Component.literal("专精：熔炎战刃"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/magma_cream"))
                .addBonus(conv("physical", "fire"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转火
                .addBonus("irons_spellbooks:fire_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:fire_damage", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士质变专精 B：风暴雷震 (物转雷) ---
        register(new SkillNode("spec_physical_to_lightning", 3, "warrior_heavy_strike")
                .setTitle(Component.literal("专精：风暴雷震"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/lightning_rod"))
                .addBonus(conv("physical", "lightning"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转雷
                .addBonus("minecraft:generic.attack_speed", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:lightning_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士质变专精 C：猩红饮血 (物转血 + 斩杀) ---
        register(new SkillNode("spec_physical_to_blood", 3, "warrior_might")
                .setTitle(Component.literal("专精：猩红死狂"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/fermented_spider_eye"))
                .addBonus(conv("physical", "blood"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转血
                .addBonus("apothic_attributes:life_steal", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:current_hp_damage", 0.02, AttributeModifier.Operation.ADD_VALUE));

        // --- 战士防御专精 D：暮色不灭幻盾 (绝对防御) ---
        register(new SkillNode("spec_tank_fortress", 3, "warrior_parry")
                .setTitle(Component.literal("专精：不灭堡垒"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/turtle_helmet"))
                .addBonus("l2damagetracker:damage_reduction", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("twilightforest:max_shield_strength", 1.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.invincibility_ticks", 2.0, AttributeModifier.Operation.ADD_VALUE));

        // 终极终赋：不灭战神
        register(new SkillNode("warrior_mastery", 1, "warrior_might")
                .setTitle(Component.literal("终赋：不灭战神"))
                .setSpell(SpellRegistry.FORTIFY_SPELL.get())
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/netherite_chestplate"))
                .addBonus("minecraft:generic.max_health", 8.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:armor_shred", 0.15, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 2：游侠敏捷 / 暗影刺客 / 异能射手 (Ranger & Shadow Assassin)
        // =========================================================================
        // 一阶：轻盈疾行
        register(new SkillNode("ranger_agility", 5, "root")
                .setTitle(Component.literal("灵巧步伐"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/leather_boots"))
                .addBonus("minecraft:generic.movement_speed", 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addBonus("artifacts:generic.sprinting_speed", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：神射与穿透
        register(new SkillNode("ranger_archery", 5, "ranger_agility")
                .setTitle(Component.literal("精准拉弓"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bow"))
                .addBonus("apothic_attributes:arrow_damage", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:draw_speed", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:arrow_velocity", 0.05, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：幽影闪避
        register(new SkillNode("ranger_dodge", 3, "ranger_agility")
                .setTitle(Component.literal("虚实残影"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/feather"))
                .addBonus("apothic_attributes:dodge_chance", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // 三阶：弱点狙杀
        register(new SkillNode("ranger_crit", 3, "ranger_archery")
                .setTitle(Component.literal("致命弱点"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/spectral_arrow"))
                .addBonus("apothic_attributes:crit_chance", 0.05, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:crit_damage", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:additional_critical_damage", 0.10, AttributeModifier.Operation.ADD_VALUE));

        // --- 游侠质变专精 E：凛冬霜矢 (物转冰) ---
        register(new SkillNode("spec_physical_to_ice", 3, "ranger_archery")
                .setTitle(Component.literal("专精：极地寒矢"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/snowball"))
                .addBonus(conv("physical", "ice"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转冰
                .addBonus("irons_spellbooks:ice_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:cold_damage", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 游侠质变专精 F：幽冥裂隙 (物转末影) ---
        register(new SkillNode("spec_physical_to_ender", 3, "ranger_dodge")
                .setTitle(Component.literal("专精：虚空掠影"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/ender_pearl"))
                .addBonus(conv("physical", "ender"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转末影
                .addBonus("irons_spellbooks:ender_spell_power", 0.06, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:dodge_chance", 0.04, AttributeModifier.Operation.ADD_VALUE));

        // 终极终赋：鹰眼猎王
        register(new SkillNode("ranger_mastery", 1, "ranger_crit")
                .setTitle(Component.literal("终赋：天际鹰眼"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/crossbow"))
                .addBonus("apothic_attributes:crit_chance", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:prot_pierce", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.entity_interaction_range", 1.0, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 3：奥术源泉 / 元素融合 / 东方仙道 (Arcane, Shifter & Taoist)
        // =========================================================================
        // 一阶：魔力与回涌
        register(new SkillNode("magic_focus", 5, "root")
                .setTitle(Component.literal("灵能涌动"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/lapis_lazuli"))
                .addBonus("irons_spellbooks:max_mana", 25.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:mana_regen", 0.06, AttributeModifier.Operation.ADD_VALUE));

        // 二阶：奥术专注与移速
        register(new SkillNode("magic_flow", 5, "magic_focus")
                .setTitle(Component.literal("施法专注"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/blaze_powder"))
                .addBonus("irons_spellbooks:spell_power", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:cast_time_reduction", 0.04, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:casting_movespeed", 0.06, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("magic_cdr", 3, "magic_focus")
                .setTitle(Component.literal("时光灵契"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/clock"))
                .addBonus("irons_spellbooks:cooldown_reduction", 0.05, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 1：等离子融爆 (火转雷) ---
        register(new SkillNode("spec_fire_to_lightning", 3, "magic_flow")
                .setTitle(Component.literal("奥术裂变：等离子电涌"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/fire_charge"))
                .addBonus(conv("fire", "lightning"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 火转雷
                .addBonus("irons_spellbooks:lightning_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 2：仙灵归真 (自然转灵能 - ISS Magic from the East) ---
        register(new SkillNode("spec_nature_to_spirit", 3, "magic_flow")
                .setTitle(Component.literal("仙法通灵：化境生魂"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/ghast_tear"))
                .addBonus(conv("nature", "spirit"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 自然转 Spirit
                .addBonus("iss_magicfromtheeast:spirit_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 元素转化进阶 3：圣音咏叹 (神圣转旋律 - Tunes 'n Tomes) ---
        register(new SkillNode("spec_holy_to_melody", 3, "magic_flow")
                .setTitle(Component.literal("福音交响：天界咏叹"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/music_disc_wait"))
                .addBonus(conv("holy", "melody"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 神圣转 Melody
                .addBonus("tunes_n_tomes:melody_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // --- 唤灵统御：召唤与暮色使徒 ---
        register(new SkillNode("magic_summoner", 3, "magic_flow")
                .setTitle(Component.literal("统御军势"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bone"))
                .addBonus("irons_spellbooks:summon_damage", 0.10, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("twilightforest:minion_count", 1.0, AttributeModifier.Operation.ADD_VALUE));

        // --- 终极核心仙法：万法归一 (物转对称 - 东方仙道至高) ---
        register(new SkillNode("spec_physical_to_symmetry", 1, "magic_flow")
                .setTitle(Component.literal("天道终赋：万象归元"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard"))
                .addBonus(conv("physical", "symmetry"), 0.40, AttributeModifier.Operation.ADD_VALUE) // 40% 物转对称
                .addBonus("iss_magicfromtheeast:symmetry_spell_power", 0.15, AttributeModifier.Operation.ADD_VALUE));

        // 终极法师终赋：真理大魔导
        register(new SkillNode("magic_archmage", 1, "magic_flow")
                .setTitle(Component.literal("终赋：全知魔导"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/enchanted_book"))
                .addBonus("irons_spellbooks:max_mana", 100.0, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:spell_power", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("irons_spellbooks:cooldown_reduction", 0.10, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 4：圣辉愈合 / 护体生机 (Paladin & Living Grace)
        // =========================================================================
        register(new SkillNode("holy_vitality", 5, "root")
                .setTitle(Component.literal("圣灵甘霖"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/glistering_melon_slice"))
                .addBonus("apothic_attributes:healing_received", 0.08, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("cataclysm:nature_heal", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 质变专精 G：圣裁誓约 (物转神圣)
        register(new SkillNode("spec_physical_to_holy", 3, "holy_vitality")
                .setTitle(Component.literal("专精：黎明圣裁"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/gold_ingot"))
                .addBonus(conv("physical", "holy"), 0.15, AttributeModifier.Operation.ADD_VALUE) // 满级 45% 物转神圣
                .addBonus("irons_spellbooks:holy_spell_power", 0.08, AttributeModifier.Operation.ADD_VALUE));

        // 过量护盾与幽魂生机
        register(new SkillNode("holy_overheal", 3, "holy_vitality")
                .setTitle(Component.literal("过量圣佑"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying"))
                .addBonus("apothic_attributes:overheal", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("apothic_attributes:ghost_health", 2.0, AttributeModifier.Operation.ADD_VALUE));


        // =========================================================================
        // 分支 5：探索漫游 / 工匠采金 (Explorer & Artisan)
        // =========================================================================
        register(new SkillNode("util_survival", 5, "root")
                .setTitle(Component.literal("荒野求生"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/bread"))
                .addBonus("minecraft:generic.safe_fall_distance", 1.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("artifacts:generic.eating_speed", 0.15, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("util_mining", 5, "util_survival")
                .setTitle(Component.literal("巨力开凿"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/iron_pickaxe"))
                .addBonus("minecraft:player.block_break_speed", 0.12, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:player.block_interaction_range", 0.5, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("util_traversal", 3, "util_survival")
                .setTitle(Component.literal("畅游四海"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/rabbit_foot"))
                .addBonus("minecraft:generic.step_height", 0.5, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("neoforge:swim_speed", 0.20, AttributeModifier.Operation.ADD_VALUE));

        register(new SkillNode("util_wisdom", 3, "util_survival")
                .setTitle(Component.literal("智者洞察"))
                .setIcon(ResourceLocation.withDefaultNamespace("textures/item/experience_bottle"))
                .addBonus("apothic_attributes:experience_gained", 0.15, AttributeModifier.Operation.ADD_VALUE)
                .addBonus("minecraft:generic.luck", 1.0, AttributeModifier.Operation.ADD_VALUE));
    }
}