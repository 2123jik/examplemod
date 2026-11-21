package com.example.examplemod.compat.apothic.affix;

import com.example.examplemod.ExampleMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class DynamicMobEffectAffixes {

    public static final String MOD_ID = "examplemod";
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * 如果只想为特定命名空间的药水效果生成词缀，往这里填 modid；
     * 留空集合表示对所有已注册的 MobEffect 生效（经过黑名单过滤）。
     */
    private static final Set<String> ALLOWED_NAMESPACES = Set.of(
            // "examplemod", "cataclysm", "irons_spellbooks", "iss_magicfromtheeast",
            // "simplyswords", "l2complements", "tunes_n_tomes", "mowziesmobs"
    );

    /**
     * 不适合做成装备词缀的效果黑名单（内部计时器、机制锁定、非战斗状态等）
     */
    private static final Set<ResourceLocation> EFFECT_BLACKLIST = Set.of(
            rl("apothic_attributes", "flying"),
            rl("apothic_attributes", "detonation"),
            rl("cataclysm", "ghost_sickness"),
            rl("l2hostility", "antibuild"),
            rl("minecraft", "bad_omen"),
            rl("minecraft", "raid_omen"),
            rl("minecraft", "trial_omen"),
            rl("minecraft", "hero_of_the_village"),
            rl("minecraft", "nausea"),
            rl("minecraft", "unluck"),
            rl("minecraft", "luck"),
            rl("minecraft", "conduit_power"),
            rl("minecraft", "dolphins_grace"),
            rl("irons_spellbooks", "polar_bear_timer"),
            rl("irons_spellbooks", "raise_dead_timer"),
            rl("irons_spellbooks", "summon_horse_timer"),
            rl("irons_spellbooks", "summon_swords_timer"),
            rl("irons_spellbooks", "vex_timer"),
            rl("irons_spellbooks", "fall_damage_immunity"),
            rl("irons_spellbooks", "true_invisibility")
    );

    /**
     * 跳过神化 / irons_apothic 原生已经存在相同 (target + mob_effect) 的词缀，避免重复进入奖池
     */
    private static final Set<String> EXISTING_NATIVE_COMBOS = Set.of(
            combo("hurt_attacker", "minecraft:blindness"),
            combo("hurt_self", "minecraft:resistance"),
            combo("hurt_self", "apothic_attributes:vitality"),
            combo("hurt_self", "minecraft:speed"),
            combo("hurt_self", "minecraft:instant_health"),
            combo("break_self", "minecraft:speed"),
            combo("break_self", "minecraft:haste"),
            combo("attack_target", "apothic_attributes:bleeding"),
            combo("attack_target", "apothic_attributes:sundering"),
            combo("attack_target", "minecraft:weakness"),
            combo("attack_self", "minecraft:speed"),
            combo("attack_self", "apothic_attributes:knowledge"),
            combo("arrow_target", "apothic_attributes:sundering"),
            combo("arrow_target", "minecraft:wither"),
            combo("arrow_target", "minecraft:slowness"),
            combo("arrow_target", "apothic_attributes:grievous"),
            combo("arrow_target", "minecraft:poison"),
            combo("arrow_target", "minecraft:levitation"),
            combo("arrow_self", "minecraft:speed"),
            combo("block_attacker", "apothic_attributes:bleeding"),
            combo("block_attacker", "minecraft:poison"),
            combo("block_attacker", "minecraft:wither"),
            combo("block_self", "minecraft:resistance"),
            combo("spell_damage_self", "minecraft:speed"),
            combo("spell_damage_self", "apothic_attributes:knowledge"),
            combo("spell_damage_target", "apothic_attributes:grievous"),
            combo("spell_damage_target", "minecraft:darkness"),
            combo("spell_heal_target", "minecraft:absorption"),
            combo("spell_heal_target", "minecraft:strength"),
            combo("spell_heal_target", "minecraft:regeneration"),
            combo("spell_heal_target", "minecraft:resistance")
    );

    private record TargetRule(
            String target,
            MobEffectCategory category,
            List<String> types,
            boolean requiresIronsApothic,
            String enPrefix,
            String enSuffix
    ) {}

    private static final List<String> ARMOR_TYPES = List.of(
            "apotheosis:helmet", "apotheosis:chestplate", "apotheosis:leggings", "apotheosis:boots"
    );
    private static final List<String> MELEE_TYPES = List.of("apotheosis:melee_weapon", "apotheosis:trident");
    private static final List<String> RANGED_TYPES = List.of("apotheosis:bow", "apotheosis:trident");
    private static final List<String> SHIELD_TYPES = List.of("apotheosis:shield");
    private static final List<String> BREAKER_TYPES = List.of("apotheosis:breaker");
    private static final List<String> STAFF_TYPES = List.of("irons_apothic:staff");

    private static final List<TargetRule> TARGET_RULES = List.of(
            // 负面效果 -> 施加给敌人/攻击者
            new TargetRule("attack_target", MobEffectCategory.HARMFUL, MELEE_TYPES, false, "Afflicting", "of Affliction"),
            new TargetRule("arrow_target", MobEffectCategory.HARMFUL, RANGED_TYPES, false, "Bane", "of Bane"),
            new TargetRule("hurt_attacker", MobEffectCategory.HARMFUL, ARMOR_TYPES, false, "Retaliating", "of Retaliation"),
            new TargetRule("block_attacker", MobEffectCategory.HARMFUL, SHIELD_TYPES, false, "Repelling", "of Repulsion"),
            new TargetRule("spell_damage_target", MobEffectCategory.HARMFUL, STAFF_TYPES, true, "Hexed", "of Hexing"),

            // 增益效果 -> 施加给自己
            new TargetRule("attack_self", MobEffectCategory.BENEFICIAL, MELEE_TYPES, false, "Empowered", "of Empowerment"),
            new TargetRule("arrow_self", MobEffectCategory.BENEFICIAL, RANGED_TYPES, false, "Swiftshot", "of Focus"),
            new TargetRule("hurt_self", MobEffectCategory.BENEFICIAL, ARMOR_TYPES, false, "Warding", "of Warding"),
            new TargetRule("block_self", MobEffectCategory.BENEFICIAL, SHIELD_TYPES, false, "Bastion", "of the Bastion"),
            new TargetRule("break_self", MobEffectCategory.BENEFICIAL, BREAKER_TYPES, false, "Digger's", "of Excavation"),
            new TargetRule("spell_damage_self", MobEffectCategory.BENEFICIAL, STAFF_TYPES, true, "Arcane", "of Surge")
    );

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (!ModList.get().isLoaded("apotheosis")) {
            return;
        }
        PackType packType = event.getPackType();
        if (packType != PackType.SERVER_DATA && packType != PackType.CLIENT_RESOURCES) {
            return;
        }

        String packId = MOD_ID + "_dynamic_mob_effect_affixes_" + packType.name().toLowerCase();
        Component title = Component.literal("ExampleMod Dynamic MobEffect Affixes");

        event.addRepositorySource(consumer -> {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    packId,
                    title,
                    PackSource.BUILT_IN,
                    Optional.empty()
            );
            // BOTTOM 优先级：这样你后续手写的 datapack 或 zh_cn.json 可以随时覆盖自动生成的条目
            PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);

            Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo info) {
                    return new VirtualAffixPack(info, packType);
                }

                @Override
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                    return new VirtualAffixPack(info, packType);
                }
            };

            Pack pack = Pack.readMetaAndCreate(locationInfo, supplier, packType, selectionConfig);
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private static final class VirtualAffixPack extends AbstractPackResources {

        private final PackType packType;
        private final byte[] packMcmetaBytes;
        private volatile Map<ResourceLocation, byte[]> cache;

        private VirtualAffixPack(PackLocationInfo location, PackType packType) {
            super(location);
            this.packType = packType;
            int format = SharedConstants.getCurrentVersion().getPackVersion(packType);
            String meta = "{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Dynamic MobEffect Affixes\"}}";
            this.packMcmetaBytes = meta.getBytes(StandardCharsets.UTF_8);
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            if (elements.length == 1 && PACK_META.equals(elements[0])) {
                return () -> new ByteArrayInputStream(packMcmetaBytes);
            }
            return null;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            if (type != this.packType || !MOD_ID.equals(location.getNamespace())) {
                return null;
            }
            byte[] data = getOrBuild().get(location);
            return data != null ? () -> new ByteArrayInputStream(data) : null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != this.packType || !MOD_ID.equals(namespace)) {
                return;
            }
            String prefix = path.isEmpty() ? "" : (path.endsWith("/") ? path : path + "/");
            for (Map.Entry<ResourceLocation, byte[]> entry : getOrBuild().entrySet()) {
                ResourceLocation rl = entry.getKey();
                if (rl.getNamespace().equals(namespace) && rl.getPath().startsWith(prefix)) {
                    byte[] data = entry.getValue();
                    output.accept(rl, () -> new ByteArrayInputStream(data));
                }
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == this.packType ? Set.of(MOD_ID) : Set.of();
        }

        @Override
        public void close() {
        }

        private Map<ResourceLocation, byte[]> getOrBuild() {
            Map<ResourceLocation, byte[]> local = this.cache;
            if (local == null) {
                synchronized (this) {
                    local = this.cache;
                    if (local == null) {
                        local = this.packType == PackType.SERVER_DATA
                                ? buildServerAffixes()
                                : buildClientLangFallback();
                        this.cache = local;
                    }
                }
            }
            return local;
        }
    }

    private static Map<ResourceLocation, byte[]> buildServerAffixes() {
        Map<ResourceLocation, byte[]> out = new HashMap<>();
        boolean hasIronsApothic = ModList.get().isLoaded("irons_apothic");

        for (Map.Entry<ResourceKey<MobEffect>, MobEffect> entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            ResourceLocation effectId = entry.getKey().location();
            MobEffect effect = entry.getValue();
            if (!shouldGenerateFor(effectId, effect)) {
                continue;
            }

            for (TargetRule rule : TARGET_RULES) {
                if (rule.requiresIronsApothic() && !hasIronsApothic) {
                    continue;
                }
                if (rule.category() != effect.getCategory()) {
                    continue;
                }
                if (EXISTING_NATIVE_COMBOS.contains(combo(rule.target(), effectId.toString()))) {
                    continue;
                }

                String affixPath = "mob_effect/" + rule.target() + "/" + effectId.getNamespace() + "/" + effectId.getPath();
                ResourceLocation fileLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, "affixes/" + affixPath + ".json");
                JsonObject json = buildAffixJson(effectId, effect, rule);
                out.put(fileLoc, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private static Map<ResourceLocation, byte[]> buildClientLangFallback() {
        JsonObject lang = new JsonObject();
        boolean hasIronsApothic = ModList.get().isLoaded("irons_apothic");

        for (Map.Entry<ResourceKey<MobEffect>, MobEffect> entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            ResourceLocation effectId = entry.getKey().location();
            MobEffect effect = entry.getValue();
            if (!shouldGenerateFor(effectId, effect)) {
                continue;
            }

            String niceEffect = prettify(effectId.getPath());
            for (TargetRule rule : TARGET_RULES) {
                if (rule.requiresIronsApothic() && !hasIronsApothic) {
                    continue;
                }
                if (rule.category() != effect.getCategory()) {
                    continue;
                }
                if (EXISTING_NATIVE_COMBOS.contains(combo(rule.target(), effectId.toString()))) {
                    continue;
                }

                // 神化 1.21 的词缀名称翻译键格式为: affix.<namespace>:<path> 和 .suffix
                String key = "affix." + MOD_ID + ":mob_effect/" + rule.target() + "/"
                        + effectId.getNamespace() + "/" + effectId.getPath();
                lang.addProperty(key, rule.enPrefix() + " (" + niceEffect + ")");
                lang.addProperty(key + ".suffix", rule.enSuffix() + " (" + niceEffect + ")");
            }
        }

        Map<ResourceLocation, byte[]> out = new HashMap<>();
        out.put(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "lang/en_us.json"),
                GSON.toJson(lang).getBytes(StandardCharsets.UTF_8)
        );
        return out;
    }

    private static boolean shouldGenerateFor(ResourceLocation effectId, MobEffect effect) {
        if (!ALLOWED_NAMESPACES.isEmpty() && !ALLOWED_NAMESPACES.contains(effectId.getNamespace())) {
            return false;
        }
        if (EFFECT_BLACKLIST.contains(effectId)) {
            return false;
        }
        if (effectId.getPath().endsWith("_timer")) {
            return false;
        }
        return effect.getCategory() != MobEffectCategory.NEUTRAL;
    }

    private static JsonObject buildAffixJson(ResourceLocation effectId, MobEffect effect, TargetRule rule) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "apotheosis:mob_effect");

        // 1. definition
        JsonObject definition = new JsonObject();
        definition.addProperty("affix_type", "basic_effect");
        definition.add("exclusive_set", new JsonArray());

        JsonObject weights = new JsonObject();
        // 批量生成的模组效果众多，建议权重给低一些（本模组减抗给 12，其他模组给 6），防止稀释原本的属性词缀
        int weight = MOD_ID.equals(effectId.getNamespace()) ? 12 : 6;
        weights.addProperty("weight", weight);
        weights.addProperty("quality", 0.1);
        definition.add("weights", weights);
        root.add("definition", definition);

        // 2. mob_effect / target / types
        root.addProperty("mob_effect", effectId.toString());
        root.addProperty("target", rule.target());

        JsonArray types = new JsonArray();
        for (String t : rule.types()) {
            types.add(t);
        }
        root.add("types", types);

        // 针对你模组自己的 *_shred_* 减抗效果或常见持续伤害效果，开启叠加
        if (isStackable(effectId)) {
            root.addProperty("stack_on_reapply", true);
            root.addProperty("stacking_limit", 3);
        }

        // 3. values（按稀有度分级）
        JsonObject values = new JsonObject();
        if (effect.isInstantenous()) {
            values.add("apotheosis:epic", instantTier(0.0, null, 300));
            values.add("apotheosis:mythic", instantTier(null, step(0.0, 1.0, 0.25), 240));
        } else {
            values.add("apotheosis:uncommon", durationTier(40.0, 80.0, 20.0, 0.0, null, 300));
            values.add("apotheosis:rare", durationTier(60.0, 120.0, 20.0, 0.0, null, 240));
            values.add("apotheosis:epic", durationTier(80.0, 160.0, 20.0, null, step(0.0, 1.0, 0.25), 200));
            values.add("apotheosis:mythic", durationTier(100.0, 200.0, 20.0, null, step(0.0, 2.0, 0.25), 160));
        }
        root.add("values", values);
        return root;
    }

    private static boolean isStackable(ResourceLocation effectId) {
        String path = effectId.getPath();
        return MOD_ID.equals(effectId.getNamespace())
                || path.contains("bleed")
                || path.contains("shred")
                || path.contains("burn")
                || path.contains("corrosion");
    }

    private static JsonObject durationTier(
            double minDur, double maxDur, double stepDur,
            @Nullable Double fixedAmp, @Nullable JsonObject stepAmp,
            int cooldown
    ) {
        JsonObject tier = new JsonObject();
        tier.add("duration", step(minDur, maxDur, stepDur));
        if (stepAmp != null) {
            tier.add("amplifier", stepAmp);
        } else if (fixedAmp != null) {
            tier.addProperty("amplifier", fixedAmp);
        }
        tier.addProperty("cooldown", cooldown);
        return tier;
    }

    private static JsonObject instantTier(
            @Nullable Double fixedAmp, @Nullable JsonObject stepAmp,
            int cooldown
    ) {
        JsonObject tier = new JsonObject();
        tier.addProperty("duration", 1.0);
        if (stepAmp != null) {
            tier.add("amplifier", stepAmp);
        } else if (fixedAmp != null) {
            tier.addProperty("amplifier", fixedAmp);
        }
        tier.addProperty("cooldown", cooldown);
        return tier;
    }

    private static JsonObject step(double min, double max, double step) {
        JsonObject obj = new JsonObject();
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        obj.addProperty("step", step);
        return obj;
    }

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static String combo(String target, String effectId) {
        return target + "|" + effectId;
    }

    private static String prettify(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}