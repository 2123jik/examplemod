package com.example.examplemod.engine;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class ModifierCatalog {

    public static final int MAX_MOD_LEVEL = 5;

    public enum Category {
        SALVO, MULTICAST, MIMIC, LEVEL_BOOST, WIDE_AMPLIFY,
        TRIGGER_EXPIRE, TRIGGER_TICK, TRIGGER_HIT, TRIGGER_ENTANGLE,
        PASSIVE_COOLDOWN, PASSIVE_RESERVE_CAP, PASSIVE_REGEN, PASSIVE_EXPAND,
        PASSIVE_VINE, PASSIVE_GUARDIAN, PASSIVE_RESONANCE, PASSIVE_BARRIER,
        PASSIVE_HAMMER, PASSIVE_PRISM, PASSIVE_CHARGE_MODE, PASSIVE_FLYING_SWORD,
        PASSIVE_CONVERTER, POST_SLOT,
        STAT_MODIFIER
    }

    public record ModSpec(
            String id,
            String displayName,
            String effectDesc,
            Category category,
            int slotCost,
            int bonusSlots,
            float flatReserve,
            float percentReserve,
            float manaMult,
            float flatAddMana,
            float damageMult,
            float critAdd,
            float radiusMult,
            float speedAdd,
            float speedMult,
            float durationAddSec,
            float durationMult,
            float scatterAddDeg,
            boolean uniformScatter,
            int countParam,
            float secondaryFloat,
            String specialTag
    ) {
        public boolean isPassiveTab() {
            return switch (category) {
                case PASSIVE_COOLDOWN, PASSIVE_RESERVE_CAP, PASSIVE_REGEN, PASSIVE_EXPAND,
                     PASSIVE_VINE, PASSIVE_GUARDIAN, PASSIVE_RESONANCE, PASSIVE_BARRIER,
                     PASSIVE_HAMMER, PASSIVE_PRISM, PASSIVE_CHARGE_MODE, PASSIVE_FLYING_SWORD,
                     PASSIVE_CONVERTER, POST_SLOT, WIDE_AMPLIFY -> true;
                default -> false;
            };
        }

        public float getFlatReserve(int lvl) {
            int l = Math.max(1, lvl);
            return flatReserve * (1.0F + 0.5F * (l - 1));
        }

        public float getPercentReserve(int lvl) {
            int l = Math.max(1, lvl);
            return Math.min(0.75F, percentReserve * (1.0F + 0.30F * (l - 1)));
        }

        public float getManaMult(int lvl) {
            int l = Math.max(1, lvl);
            if (manaMult < 1.0F) {
                return Math.max(0.35F, manaMult - 0.06F * (l - 1));
            }
            return (manaMult == 1.0F) ? 1.0F : (manaMult + 0.15F * (l - 1));
        }

        public float getFlatAddMana(int lvl) {
            int l = Math.max(1, lvl);
            return flatAddMana * (1.0F + 0.45F * (l - 1));
        }

        public float getDamageMult(int lvl) {
            int l = Math.max(1, lvl);
            if (damageMult > 1.0F) return 1.0F + (damageMult - 1.0F) * l;
            if (damageMult < 1.0F) return Math.min(0.95F, damageMult + 0.05F * (l - 1));
            return 1.0F;
        }

        public int getCount(int lvl) {
            int l = Math.max(1, lvl);
            if (countParam <= 0) return 0;
            if (category == Category.PASSIVE_EXPAND) return countParam + (l - 1);
            if (category == Category.SALVO || category == Category.MULTICAST || "SPLIT".equals(specialTag)
                    || "PIERCE".equals(specialTag) || "BOUNCE".equals(specialTag) || "REFRACT".equals(specialTag)) {
                return countParam + (l - 1);
            }
            return countParam;
        }

        public float getSecondary(int lvl) {
            int l = Math.max(1, lvl);
            if ("CD_MULT".equals(specialTag)) {
                return Math.max(0.20F, 0.60F - 0.10F * (l - 1));
            }
            if ("CHAIN_TICK".equals(specialTag) || "ECHO_HIT".equals(specialTag)) {
                return Math.max(0.10F, secondaryFloat - 0.05F * (l - 1));
            }
            return secondaryFloat * (1.0F + 0.40F * (l - 1));
        }

        public int getBonusSlots(int lvl) {
            if (category == Category.PASSIVE_EXPAND) return Math.min(6, bonusSlots + (Math.max(1, lvl) - 1));
            return bonusSlots;
        }
    }

    private static final Map<ResourceLocation, ModSpec> REGISTRY = new LinkedHashMap<>();

    static {
        // 1. 并发与触发链
        reg("salvo", "齐射", "同时施放右侧 %count 个主动法术并散射，每发独立扣蓝",
                Category.SALVO, 1, 0, 10, 0, 0.85f, 1, 1, 0, 1, 0, 1, 0, 1, 10, false, 2, 0, "");
        reg("overload_scatter", "超载散射", "同时并发右侧 %count 个主动法术，大散射且减冷却",
                Category.SALVO, 1, 0, 20, 0, 0.95f, 2, 1, 0, 1, 0, 1, 0, 1, 60, false, 4, -0.15f, "CD_REDUCE");
        reg("multicast", "多重施法", "右侧首个法术额外多释放 %count 次，每次独立高耗蓝",
                Category.MULTICAST, 1, 0, 15, 0, 1.45f, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "");
        reg("mimic_cube", "拟态魔方", "复制右侧首个节点额外执行一次（照常扣蓝）",
                Category.MIMIC, 1, 0, 15, 0, 1.00f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 99, 0, "");
        reg("arcane_upgrade", "魔能升阶", "令左右紧邻的法术/词条额外 +%count 级（含蓝耗提升）",
                Category.LEVEL_BOOST, 1, 0, 25, 0, 1.00f, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "NEIGHBOR_LVL_UP");
        reg("wide_amplify", "广域增幅", "将右侧首个增强效果共享给全部编排节点（高额锁蓝）",
                Category.WIDE_AMPLIFY, 4, 0, 0, 0.25f, 1.00f, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "");

        reg("fuse", "引线", "左侧法术结束时，在当前坐标扣蓝放出右侧法术",
                Category.TRIGGER_EXPIRE, 1, 0, 0, 0, 0.90f, 1, 1.0f, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "FUSE");
        reg("duet", "二重奏", "左侧法术结束时，在当前坐标扣蓝放出右侧法术并强化伤害",
                Category.TRIGGER_EXPIRE, 1, 0, 10, 0, 1.00f, 1, 0.60f, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "DUET");
        reg("fireworks", "绚丽烟火", "左侧法术结束时，向四周散射释放 %count 次右侧法术",
                Category.TRIGGER_EXPIRE, 1, 0, 20, 0, 0.85f, 1, 0.45f, 0, 1, 0, 1, 0, 1, 0, false, 4, 0, "FIREWORKS");
        reg("arcane_nova", "奥术新星", "飞行途中连续触发最多 20 次右侧法术（每次 500% 耗蓝）",
                Category.TRIGGER_TICK, 1, 0, 25, 0, 5.00f, 0, 0.50f, 0, 1, 0, 1, 0, 1, 0, false, 20, 0.20f, "NOVA");
        reg("chain", "连环", "左侧法术飞行途中每 %sec 秒扣蓝触发一次右侧法术",
                Category.TRIGGER_TICK, 1, 0, 10, 0, 0.75f, 1, 1.0f, 0, 1, 0, 1, 0, 1, 0, false, 99, 0.40f, "CHAIN_TICK");
        reg("entangle", "缠绕", "发射 %count 个右侧法术绕着左侧主法术螺旋旋转飞行",
                Category.TRIGGER_ENTANGLE, 2, 0, 20, 0, 0.85f, 1, 1.0f, 0, 1, 3, 1, 0, 1, 0, false, 3, 0, "ENTANGLE");
        reg("echo", "回响", "左侧法术命中敌人时在命中点扣蓝释放右侧法术（冷却 %sec 秒）",
                Category.TRIGGER_HIT, 1, 0, 15, 0, 0.80f, 1, 1.0f, 0, 1, 0, 1, 0, 1, 0, false, 99, 0.35f, "ECHO_HIT");

        // 2. 弹道与抽象中心坐标
        reg("pierce", "穿透", "魔法投射物额外穿透 +%count 个目标",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 1, 0, "PIERCE");
        reg("ricochet", "反弹", "投射物撞墙反弹 +%count 次",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 2, 0, "BOUNCE");
        reg("refract", "折射", "命中敌人后自动折射转向附近下个目标（%count 次）",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 1, 0.85f, 0, 1, 0, 1, 0, 1, 0, false, 2, 0, "REFRACT");
        reg("split", "分裂", "法术结束时分裂出 3 个残影法术（仅1层，独立扣蓝）",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.50f, 2, 0.35f, 0, 1, 0, 1, 0, 1, 0, false, 3, 0, "SPLIT");
        reg("hover", "悬停", "法术飞行片刻后在原地悬停 %sec 秒再消散",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.05f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 2.0f, "HOVER");
        reg("orbit", "公转", "让法术抽象中心坐标（含黑洞/治愈环/飞弹）以半径 %sec 米绕你公转",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.30f, 3, 1, 0, 1, 0, 1, 4.0f, 1, 0, false, 0, 3.0f, "ORBIT");
        reg("free_orbit", "自由公转", "以随机波动半径绕施法者公转",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.25f, 2, 1, 0, 1, 0, 1, 3.0f, 1, 0, false, 0, 3.5f, "FREE_ORBIT");
        reg("cursor_homing", "寻踪", "飞弹实时朝向你的准星所指坐标追踪",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 1, 1, 0, 1, 2, 1, 1.0f, 1, 0, false, 0, 0, "CURSOR_HOMING");
        reg("auto_homing", "自动导航", "自动拐弯追踪 12 格内最近敌方生物",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 18.0f, "ENTITY_HOMING");
        reg("self_homing", "自我追踪", "飞弹绕回追踪施法者自身（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 25, 0, 1.05f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 25.0f, "SELF_HOMING");
        reg("end_teleport", "结束传送", "法术消散时把你瞬移至它的最终坐标（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 30, 0, 1.20f, 4, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0, "END_TP");
        reg("mid_teleport", "半途传送", "法术飞行中途随机空间折跃（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 25, 0, 1.10f, 2, 1, 0, 1, 2, 1, 2.0f, 1, 0, false, 0, 0, "MID_TP");
        reg("fall_down", "坠落", "法术改为从目标正上方 10 格垂直坠落砸地",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 1.5f, "FALL");
        reg("bloated", "虚胖法术", "大幅放大 AOE 体积与范围（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 25, 0, 1.15f, 2, 1, 0, 1.4f, 0, 1, 0, 1, 0, false, 0, 0, "SCALE_UP");
        reg("reverse_cast", "反向施法", "从背后反向发射且速度大幅提升（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 20, 0, 1.00f, 1, 1, 0, 1, 4, 1, 0, 1, 0, false, 0, 0, "REVERSE");
        reg("slow_time", "主观缓时", "飞弹速度减半，持续时间延长（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 20, 0, 1.05f, 1, 1, 0, 1, 0, 0.5f, 2.0f, 1, 0, false, 0, 0, "SLOW_TIME");
        reg("remote_cast", "远程施法", "直接在准星 24 格内指中的坐标点生成法术（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 35, 0, 1.20f, 3, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0, "REMOTE_CAST");
        reg("uniform_scatter", "均匀散射", "使齐射的多发法术按等角扇形均匀分布（容量+1）",
                Category.STAT_MODIFIER, 1, 1, 20, 0, 1.00f, 1, 1, 0, 1, 0, 1, 0, 1, 0, true, 0, 0, "");

        // 3. 数值增幅与晶石
        reg("damage_up", "伤害强化", "提升法术最终伤害（当前 ×%dmg），随等级提高蓝耗",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.25f, 1, 1.25f, 0, 1, 0, 1, 0, 1, 0, false, 0, 0, "");
        reg("overload", "过载", "高额法术增伤（当前 ×%dmg）并提高蓝耗",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.35f, 2, 1.50f, 0, 1, 0, 1, 0, 1, 0, false, 0, 0, "");
        reg("duration_up", "时长强化", "延长持续时间 +2 秒/级",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 1, 1, 0, 1, 0, 1, 2.0f, 1, 0, false, 0, 0, "");
        reg("mana_save", "节能模式", "降低法术魔耗（当前 ×%mana）并稍微牺牲伤害，最低仍耗 5 蓝",
                Category.STAT_MODIFIER, 1, 0, 15, 0, 0.65f, 0, 0.75f, 0, 1, 0, 1, 0, 0.8f, 0, false, 0, 0, "");
        reg("precise_cast", "精准施法", "提升暴击率并收束散射角度",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 1, 1, 0.15f, 1, 0, 1, 0, 1, -40, false, 0, 0, "");
        reg("accelerator", "加速器", "提高投射物飞行速度",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.05f, 1, 1, 0, 1, 4, 1, 0, 1, 0, false, 0, 0, "");
        reg("aoe_up", "范围增强", "放大黑洞/治愈环等 AOE 半径",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.25f, 2, 1, 0, 1.30f, 0, 1, 0, 1, 0, false, 0, 0, "");
        reg("implode", "聚爆", "AOE 半径减半，大幅浓缩提升伤害",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.20f, 2, 1.60f, 0, 0.50f, 0, 1, 0, 1, 0, false, 0, 0, "IMPLODE");

        reg("chain_lightning_mod", "闪电链", "将上一个施放法术的坐标与本法术用雷电相连",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 18.0f, "LINK_PREV");
        reg("slime_crystal", "粘液晶石", "命中目标施加大幅减速",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 4.0f, "HIT_SLOW");
        reg("poison_crystal", "毒液晶石", "命中目标附加持续剧毒",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.10f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 2, 4.0f, "HIT_POISON");
        reg("frost_crystal", "寒霜晶石", "命中目标叠加深度冰冻值",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 2, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 3.0f, "HIT_FREEZE");
        reg("thunder_crystal", "雷霆晶石", "法术结束时概率召下雷击",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.20f, 3, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.25f, "END_THUNDER");
        reg("flame_crystal", "炽焰晶石", "占 2 格，命中造成敌方最大生命百分比伤害（Boss 0.5%）",
                Category.STAT_MODIFIER, 2, 0, 0, 0, 1.35f, 4, 1.50f, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.05f, "HIT_MAX_HP_BURN");
        reg("pull_crit", "强力牵引", "暴击时牵引周围敌人并传导伤害",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.15f, 2, 1, 0.10f, 1, 0, 1, 0, 1, 0, false, 2, 8.0f, "CRIT_PULL");
        reg("collapse_crystal", "坍缩晶石", "命中残血敌人直接斩杀并向周围引爆均摊生命伤害",
                Category.STAT_MODIFIER, 1, 0, 0, 0, 1.25f, 3, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.80f, "HIT_COLLAPSE");

        // 4. 法术被动（全带蓝量预留税或代价）
        reg("mana_chest", "储魔之匣", "法术耗蓝增加，换取高阶构筑承载力",
                Category.PASSIVE_RESERVE_CAP, 1, 0, 0, 0, 1.25f, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.40f, "BONUS_CAP_TAX");
        reg("tranquil_flower", "宁静之花", "每秒额外回蓝 +%sec，代价是持续锁定一部分最大蓝量",
                Category.PASSIVE_REGEN, 1, 0, 0, 0.16f, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 6.0f, "REGEN_PER_SEC");
        reg("charge_mode", "蓄力模式", "按住蓄力积攒多次释放（每次独立扣蓝）",
                Category.PASSIVE_CHARGE_MODE, 1, 0, 20, 0, 0.90f, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 6, 0, "");
        reg("guardian_spirit", "守护杖灵", "锁定最大蓝量，附近有敌人时自动替你推进施法（照常扣蓝）",
                Category.PASSIVE_GUARDIAN, 1, 0, 0, 0.20f, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 1.0f, "AUTO_CAST");
        reg("forced_cooldown", "强制冷却", "全法术冷却缩放至 %sec 倍，代价是持续锁定最大蓝量",
                Category.PASSIVE_COOLDOWN, 1, 0, 0, 0.18f, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.60f, "CD_MULT");
        reg("expander_stone", "扩容石", "解锁额外槽位容量，代价是高额锁蓝与略微降伤",
                Category.PASSIVE_EXPAND, 1, 2, 100, 0, 1, 0, 0.85f, 0, 1, 0, 1, 0, 1, 0, false, 2, 0, "EXPAND");
        reg("resonance_rune", "共鸣符文", "锁定上限，联动触发时魔耗打折（仍不免单）",
                Category.PASSIVE_RESONANCE, 1, 0, 0, 0.15f, 0.65f, 1, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.50f, "RESONANCE");
        reg("arcane_barrier", "奥术屏障", "占 3 格+锁蓝，受击按 30 蓝/1 伤扣蓝抵消伤害并反伤",
                Category.PASSIVE_BARRIER, 3, 0, 80, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 30.0f, "MANA_SHIELD");
        reg("rune_hammer", "符文战锤", "锁蓝提升移速并附带近身冲击",
                Category.PASSIVE_HAMMER, 1, 0, 40, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 0.25f, "MOVE_SMASH");
        reg("prism_core", "棱镜核心", "持续命中叠伤但耗蓝逐步攀升",
                Category.PASSIVE_PRISM, 1, 0, 25, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 10.0f, "RAMP_ON_HIT");
        reg("mana_vine", "魔力藤蔓", "锁定部分蓝上限换取持续回蓝",
                Category.PASSIVE_VINE, 1, 0, 0, 0.12f, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 5.0f, "CONSUME_EMPTY");
        reg("flying_sword", "狴犴飞剑", "锁蓝，施法时附加飞剑协同攻击（容量+1）",
                Category.PASSIVE_FLYING_SWORD, 1, 1, 50, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 15.0f, "EXTRA_SWORD");
        reg("mana_converter", "魔能转化仪", "停止自然回蓝，换取后置充能槽效率翻倍",
                Category.PASSIVE_CONVERTER, 1, 0, 0, 0.10f, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 1.0f, "NO_REGEN_BOOST_POST");

        reg("post_move", "破界石-移动", "移动充能，满 100% 自动扣蓝释放右侧法术",
                Category.POST_SLOT, 1, 0, 30, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 3.2f, "MOVE");
        reg("post_stand", "破界石-站立", "站立充能，满 100% 自动扣蓝释放右侧法术",
                Category.POST_SLOT, 1, 0, 30, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 18.4f, "STAND");
        reg("post_time", "破界石-时间", "随时间自动充能，满 100% 自动扣蓝释放右侧法术",
                Category.POST_SLOT, 1, 0, 30, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 13.0f, "TIME");
        reg("post_cast", "破界石-施法", "每次施法充能，满 100% 自动扣蓝释放右侧法术",
                Category.POST_SLOT, 1, 0, 30, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, false, 0, 8.0f, "CAST");
    }

    private static void reg(String path, String name, String desc, Category cat, int slotCost, int bonusSlots,
                            float flatRes, float pctRes, float manaMult, float flatMana,
                            float dmgMult, float critAdd, float radMult, float spdAdd, float spdMult,
                            float durAdd, float durMult, float scatterAdd, boolean uniform,
                            int countParam, float secFloat, String tag) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("examplemod", path);
        REGISTRY.put(id, new ModSpec(path, name, desc, cat, slotCost, bonusSlots, flatRes, pctRes,
                manaMult, flatMana, dmgMult, critAdd, radMult, spdAdd, spdMult, durAdd, durMult,
                scatterAdd, uniform, countParam, secFloat, tag));
    }

    public static ModSpec get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static Set<ResourceLocation> allIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    public static String formatDesc(ModSpec spec, int level) {
        return spec.effectDesc()
                .replace("%count", String.valueOf(spec.getCount(level)))
                .replace("%sec", String.format("%.2f", spec.getSecondary(level)))
                .replace("%dmg", String.format("%.2f", spec.getDamageMult(level)))
                .replace("%mana", String.format("%.2f", spec.getManaMult(level)));
    }
}