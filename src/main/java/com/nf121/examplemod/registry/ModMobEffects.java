package com.nf121.examplemod.registry;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.event.spell.UnifiedSpellDamageBridge;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.tunes_n_tomes.registry.TSchoolRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.warphan.iss_magicfromtheeast.registries.MFTESchoolRegistries;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;



public class ModMobEffects {

    public enum ShredType {
        FLAT("shred_flat", "Flat Resistance Shred", AttributeModifier.Operation.ADD_VALUE),
        TOTAL("shred_total", "Total Resistance Shred", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        public final String id;
        public final String displaySuffix;
        public final AttributeModifier.Operation operation;

        ShredType(String id, String displaySuffix, AttributeModifier.Operation operation) {
            this.id = id;
            this.displaySuffix = displaySuffix;
            this.operation = operation;
        }
    }

    // 存储类型直接使用 SimpleEntry<MobEffect>
    private static final Map<String, Map<ShredType, SimpleEntry<MobEffect>>> SHRED_MAP = new HashMap<>();

    // 13 个学派配置信息
    public static final List<SchoolEntry> REGISTERED_SCHOOLS = List.of(
            new SchoolEntry("fire", "Fire", SchoolRegistry.FIRE, 0xFFAA00),
            new SchoolEntry("ice", "Ice", SchoolRegistry.ICE, 13695487),
            new SchoolEntry("lightning", "Lightning", SchoolRegistry.LIGHTNING, 0x55FFFF),
            new SchoolEntry("holy", "Holy", SchoolRegistry.HOLY, 16775380),
            new SchoolEntry("ender", "Ender", SchoolRegistry.ENDER, 0xFF55FF),
            new SchoolEntry("blood", "Blood", SchoolRegistry.BLOOD, 0xAA0000),
            new SchoolEntry("evocation", "Evocation", SchoolRegistry.EVOCATION, 0xFFFFFF),
            new SchoolEntry("nature", "Nature", SchoolRegistry.NATURE, 0x55FF55),
            new SchoolEntry("eldritch", "Eldritch", SchoolRegistry.ELDRITCH, 1016732),
            new SchoolEntry("melody", "Melody", TSchoolRegistry.MELODY, 13631442),
            new SchoolEntry("symmetry", "Symmetry", MFTESchoolRegistries.SYMMETRY, 41836),
            new SchoolEntry("spirit", "Spirit", MFTESchoolRegistries.SPIRIT, 40388),
            new SchoolEntry("dune", "Dune", MFTESchoolRegistries.DUNE, 16731392)
    );

    static {
        for (SchoolEntry entry : REGISTERED_SCHOOLS) {
            Map<ShredType, SimpleEntry<MobEffect>> typeMap = new EnumMap<>(ShredType.class);

            for (ShredType type : ShredType.values()) {
                String regName = entry.name() + "_" + type.id;
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, regName + "_mod");
                String displayName = entry.displayName() + " " + type.displaySuffix;
                String description = "Reduces target's " + entry.displayName().toLowerCase() + " magic resistance.";

                // 使用与 LCEffects 相同规范的 genEffect 注册
                SimpleEntry<MobEffect> effectEntry = genEffect(
                        regName,
                        displayName,
                        () -> new SchoolShredMobEffect(entry.supplier(), type, modifierId, entry.color()),
                        description
                );

                typeMap.put(type, effectEntry);
            }

            SHRED_MAP.put(entry.name(), typeMap);
        }
    }

    /**
     * 参照 LCEffects.genEffect 实现
     */
    private static <T extends MobEffect> SimpleEntry<MobEffect> genEffect(String name, String lang, NonNullSupplier<T> sup, String desc) {
        return new SimpleEntry<>(ExampleMod.REGISTRATE.effect(name, sup, desc).lang(MobEffect::getDescriptionId, lang).register());
    }

    /**
     * 对齐 LCEffects.register()，用于在主类中唤醒该类的加载
     */
    public static void init() {
    }

    /**
     * 获取效果的 SimpleEntry
     */
    public static SimpleEntry<MobEffect> getEffect(SchoolType school, ShredType type) {
        String path = school.getId().getPath();
        Map<ShredType, SimpleEntry<MobEffect>> map = SHRED_MAP.get(path);
        return map != null ? map.get(type) : null;
    }

    public record SchoolEntry(String name, String displayName, Supplier<SchoolType> supplier, int color) {}

    public static class SchoolShredMobEffect extends MobEffect {
        private final Supplier<SchoolType> schoolSupplier;
        private final ShredType shredType;
        private final ResourceLocation modifierId;

        public SchoolShredMobEffect(Supplier<SchoolType> schoolSupplier, ShredType shredType, ResourceLocation modifierId, int color) {
            super(MobEffectCategory.HARMFUL, color);
            this.schoolSupplier = schoolSupplier;
            this.shredType = shredType;
            this.modifierId = modifierId;
        }

        @Override
        public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
            Holder<Attribute> attr = UnifiedSpellDamageBridge.getResistanceAttributeFor(schoolSupplier.get());
            if (attr != null) {
                AttributeInstance attributeinstance = attributeMap.getInstance(attr);
                if (attributeinstance != null) {
                    attributeinstance.removeModifier(this.modifierId);
                    double shredAmount = -(amplifier + 1) * 0.01;
                    AttributeModifier modifier = new AttributeModifier(this.modifierId, shredAmount, this.shredType.operation);
                    attributeinstance.addPermanentModifier(modifier);
                }
            }
        }

        @Override
        public void removeAttributeModifiers(AttributeMap attributeMap) {
            Holder<Attribute> attr = UnifiedSpellDamageBridge.getResistanceAttributeFor(schoolSupplier.get());
            if (attr != null) {
                AttributeInstance attributeinstance = attributeMap.getInstance(attr);
                if (attributeinstance != null) {
                    attributeinstance.removeModifier(this.modifierId);
                }
            }
        }
    }
}
