package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, ExampleMod.MOD_ID);

    // 1. 魔能爆质变：虚空奇点（取消连发，聚变为吞噬黑洞）
    public static final DeferredHolder<Attribute, Attribute> ELDRITCH_SINGULARITY = ATTRIBUTES.register(
            "eldritch_singularity",
            () -> new RangedAttribute("attribute.examplemod.eldritch_singularity", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 音波共鸣质变：音爆裂解（击碎防御、震荡波次生裂解与真伤眩晕）
    public static final DeferredHolder<Attribute, Attribute> SONIC_RESONANCE_SHATTER = ATTRIBUTES.register(
            "sonic_resonance_shatter",
            () -> new RangedAttribute("attribute.examplemod.sonic_resonance_shatter", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 念动力质变：动能陨落（将抓取的目标充能为超重力炮弹轰击发射）
    public static final DeferredHolder<Attribute, Attribute> KINETIC_ANNIHILATION = ATTRIBUTES.register(
            "kinetic_annihilation",
            () -> new RangedAttribute("attribute.examplemod.kinetic_annihilation", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 幽匿触手质变：盲目巢群（宿主寄生感染，被击中的敌人体内炸裂出二重触手）
    public static final DeferredHolder<Attribute, Attribute> SCULK_HIVE_INFESTATION = ATTRIBUTES.register(
            "sculk_hive_infestation",
            () -> new RangedAttribute("attribute.examplemod.sculk_hive_infestation", 0.0, 0.0, 1.0).setSyncable(true)
    );
    // 1. 血步质变：血影断魂（闪烁留痕斩击）
    public static final DeferredHolder<Attribute, Attribute> BLOOD_STEP_SEVERANCE = ATTRIBUTES.register(
            "blood_step_severance",
            () -> new RangedAttribute("attribute.examplemod.blood_step_severance", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 针灸质变：血梏钉刑（定身穿刺与血牢引爆）
    public static final DeferredHolder<Attribute, Attribute> ACUPUNCTURE_CRUCIFIXION = ATTRIBUTES.register(
            "acupuncture_crucifixion",
            () -> new RangedAttribute("attribute.examplemod.acupuncture_crucifixion", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 血之斩击质变：血月十字斩（双重交叉回旋与穿透回血）
    public static final DeferredHolder<Attribute, Attribute> BLOOD_SLASH_CROSS = ATTRIBUTES.register(
            "blood_slash_cross",
            () -> new RangedAttribute("attribute.examplemod.blood_slash_cross", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 汲取射线质变：猩红链锁（折射多目标汲取与生命过载）
    public static final DeferredHolder<Attribute, Attribute> SIPHON_CHAIN = ATTRIBUTES.register(
            "siphon_chain",
            () -> new RangedAttribute("attribute.examplemod.siphon_chain", 0.0, 0.0, 3.0).setSyncable(true)
    );

    // 5. 凋零之首质变：死疫齐射（三联发死疫血颅）
    public static final DeferredHolder<Attribute, Attribute> WITHER_BARRAGE = ATTRIBUTES.register(
            "wither_barrage",
            () -> new RangedAttribute("attribute.examplemod.wither_barrage", 0.0, 0.0, 1.0).setSyncable(true)
    );
    // 1. 瞬移术质变：虚空置换（与目标强行空间对调）
    public static final DeferredHolder<Attribute, Attribute> TELEPORT_VOID_SWAP = ATTRIBUTES.register(
            "teleport_void_swap",
            () -> new RangedAttribute("attribute.examplemod.teleport_void_swap", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 影斩质变：瞬狱影杀阵（化作暗影展开无敌五段折跃连斩）
    public static final DeferredHolder<Attribute, Attribute> SHADOW_SLASH_OMNISLASH = ATTRIBUTES.register(
            "shadow_slash_omnislash",
            () -> new RangedAttribute("attribute.examplemod.shadow_slash_omnislash", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 魔法飞弹质变：浮游星芒（转化为环绕自身的自律索敌浮游炮）
    public static final DeferredHolder<Attribute, Attribute> MAGIC_MISSILE_SWARM = ATTRIBUTES.register(
            "magic_missile_swarm",
            () -> new RangedAttribute("attribute.examplemod.magic_missile_swarm", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 黑洞质变：引力坍缩超新星（黑洞生命周期结束时发生超新星引力核爆）
    public static final DeferredHolder<Attribute, Attribute> BLACK_HOLE_SUPERNOVA = ATTRIBUTES.register(
            "black_hole_supernova",
            () -> new RangedAttribute("attribute.examplemod.black_hole_supernova", 0.0, 0.0, 1.0).setSyncable(true)
    );
    // ==================== 唤魔系（Evocation）机制质变 ====================

    // 1. 替罪羊质变：命元代偿
    public static final DeferredHolder<Attribute, Attribute> SCAPEGOAT_SOUL_SURROGATE = ATTRIBUTES.register(
            "scapegoat_soul_surrogate",
            () -> new RangedAttribute("attribute.examplemod.scapegoat_soul_surrogate", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 魔法护盾质变：偏斜过载
    public static final DeferredHolder<Attribute, Attribute> SHIELD_DEFLECTION_OVERLOAD = ATTRIBUTES.register(
            "shield_deflection_overload",
            () -> new RangedAttribute("attribute.examplemod.shield_deflection_overload", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 尖牙结界质变：断罪咬合
    public static final DeferredHolder<Attribute, Attribute> FANG_WARD_SHRED = ATTRIBUTES.register(
            "fang_ward_shred",
            () -> new RangedAttribute("attribute.examplemod.fang_ward_shred", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 迟缓术质变：时滞停滞
    public static final DeferredHolder<Attribute, Attribute> SLOW_CHRONO_STAGNATION = ATTRIBUTES.register(
            "slow_chrono_stagnation",
            () -> new RangedAttribute("attribute.examplemod.slow_chrono_stagnation", 0.0, 0.0, 1.0).setSyncable(true)
    );
    // ==================== 火焰系（Fire）机制质变 ====================

    // 1. 烈焰风暴质变：过热超载
    public static final DeferredHolder<Attribute, Attribute> BLAZE_STORM_OVERHEAT = ATTRIBUTES.register(
            "blaze_storm_overheat",
            () -> new RangedAttribute("attribute.examplemod.blaze_storm_overheat", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 烈焰突进质变：劫火回旋充能
    public static final DeferredHolder<Attribute, Attribute> BURNING_DASH_IGNITION = ATTRIBUTES.register(
            "burning_dash_ignition",
            () -> new RangedAttribute("attribute.examplemod.burning_dash_ignition", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 火球术质变：超临界聚爆
    public static final DeferredHolder<Attribute, Attribute> FIREBALL_SUPERCRITICAL = ATTRIBUTES.register(
            "fireball_supercritical",
            () -> new RangedAttribute("attribute.examplemod.fireball_supercritical", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 热浪涌动质变：灰烬焦土领域
    public static final DeferredHolder<Attribute, Attribute> HEAT_SURGE_CINDER_DOMAIN = ATTRIBUTES.register(
            "heat_surge_cinder_domain",
            () -> new RangedAttribute("attribute.examplemod.heat_surge_cinder_domain", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // ==================== 神圣系（Holy）机制质变 ====================

    // 1. 神圣重击质变：圣裁审判
    public static final DeferredHolder<Attribute, Attribute> DIVINE_SMITE_JUDGMENT = ATTRIBUTES.register(
            "divine_smite_judgment",
            () -> new RangedAttribute("attribute.examplemod.divine_smite_judgment", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 生命祝福/治疗质变：过量光佑
    public static final DeferredHolder<Attribute, Attribute> BLESSING_OVERHEAL_AEGIS = ATTRIBUTES.register(
            "blessing_overheal_aegis",
            () -> new RangedAttribute("attribute.examplemod.blessing_overheal_aegis", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 日光射线质变：黎明耀斑
    public static final DeferredHolder<Attribute, Attribute> SUNBEAM_DAWN_FLARE = ATTRIBUTES.register(
            "sunbeam_dawn_flare",
            () -> new RangedAttribute("attribute.examplemod.sunbeam_dawn_flare", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 强化术质变：殉道光环
    public static final DeferredHolder<Attribute, Attribute> FORTIFY_MARTYR_AEGIS = ATTRIBUTES.register(
            "fortify_martyr_aegis",
            () -> new RangedAttribute("attribute.examplemod.fortify_martyr_aegis", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // ==================== 冰霜系（Ice）机制质变 ====================

    // 1. 霜冻之踏质变：绝对零度雪崩
    public static final DeferredHolder<Attribute, Attribute> FROST_STEP_AVALANCHE = ATTRIBUTES.register(
            "frost_step_avalanche",
            () -> new RangedAttribute("attribute.examplemod.frost_step_avalanche", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 冰刺术质变：极冰穿刺碎裂
    public static final DeferredHolder<Attribute, Attribute> ICE_SPIKES_SHATTER = ATTRIBUTES.register(
            "ice_spikes_shatter",
            () -> new RangedAttribute("attribute.examplemod.ice_spikes_shatter", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 暴风雪质变：永冻冰渊
    public static final DeferredHolder<Attribute, Attribute> BLIZZARD_PERMAFROST = ATTRIBUTES.register(
            "blizzard_permafrost",
            () -> new RangedAttribute("attribute.examplemod.blizzard_permafrost", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 冰封之棺质变：极寒涅槃
    public static final DeferredHolder<Attribute, Attribute> ICE_TOMB_ABSOLUTE_ZERO = ATTRIBUTES.register(
            "ice_tomb_absolute_zero",
            () -> new RangedAttribute("attribute.examplemod.ice_tomb_absolute_zero", 0.0, 0.0, 1.0).setSyncable(true)
    );



    // ==================== 雷电系（Lightning）机制质变 ====================

    // 1. 连锁闪电质变：高压电离超导
    public static final DeferredHolder<Attribute, Attribute> CHAIN_LIGHTNING_SUPERCONDUCTOR = ATTRIBUTES.register(
            "chain_lightning_superconductor",
            () -> new RangedAttribute("attribute.examplemod.chain_lightning_superconductor", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 落雷术质变：电磁雷暴引信
    public static final DeferredHolder<Attribute, Attribute> LIGHTNING_BOLT_EMP = ATTRIBUTES.register(
            "lightning_bolt_emp",
            () -> new RangedAttribute("attribute.examplemod.lightning_bolt_emp", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 充能术质变：超频聚能
    public static final DeferredHolder<Attribute, Attribute> CHARGE_OVERCLOCK = ATTRIBUTES.register(
            "charge_overclock",
            () -> new RangedAttribute("attribute.examplemod.charge_overclock", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 雷霆之踏质变：苍雷闪烁突刺
    public static final DeferredHolder<Attribute, Attribute> THUNDER_STEP_SHEAR = ATTRIBUTES.register(
            "thunder_step_shear",
            () -> new RangedAttribute("attribute.examplemod.thunder_step_shear", 0.0, 0.0, 1.0).setSyncable(true)
    );
    // ==================== 自然系（Nature）机制质变 ====================

    // 1. 酸液法球质变：强酸蚀甲聚爆
    public static final DeferredHolder<Attribute, Attribute> ACID_ORB_CORROSION = ATTRIBUTES.register(
            "acid_orb_corrosion",
            () -> new RangedAttribute("attribute.examplemod.acid_orb_corrosion", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 2. 缠绕术质变：绞杀荆棘断筋
    public static final DeferredHolder<Attribute, Attribute> ROOT_STRANGLE_RUPTURE = ATTRIBUTES.register(
            "root_strangle_rupture",
            () -> new RangedAttribute("attribute.examplemod.root_strangle_rupture", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 3. 枯萎术质变：疫病生息共振
    public static final DeferredHolder<Attribute, Attribute> BLIGHT_PESTILENCE = ATTRIBUTES.register(
            "blight_pestilence",
            () -> new RangedAttribute("attribute.examplemod.blight_pestilence", 0.0, 0.0, 1.0).setSyncable(true)
    );

    // 4. 橡木之躯质变：远古荆棘古树
    public static final DeferredHolder<Attribute, Attribute> OAKSKIN_ANCIENT_THORNS = ATTRIBUTES.register(
            "oakskin_ancient_thorns",
            () -> new RangedAttribute("attribute.examplemod.oakskin_ancient_thorns", 0.0, 0.0, 1.0).setSyncable(true)
    );
    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        ATTRIBUTES.getEntries().forEach(holder -> {
            event.add(EntityType.PLAYER, holder);
        });
    }
}