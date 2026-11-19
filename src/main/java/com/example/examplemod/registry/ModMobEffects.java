package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.event.damage.UnifiedSpellDamageBridge;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.tunes_n_tomes.registry.TSchoolRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.warphan.iss_magicfromtheeast.registries.MFTESchoolRegistries;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ExampleMod.MODID);

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

    private static final Map<String, Map<ShredType, DeferredHolder<MobEffect, MobEffect>>> SHRED_MAP = new HashMap<>();

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
            Map<ShredType, DeferredHolder<MobEffect, MobEffect>> typeMap = new EnumMap<>(ShredType.class);

            for (ShredType type : ShredType.values()) {
                String regName = entry.name() + "_" + type.id;
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, regName + "_mod");

                DeferredHolder<MobEffect, MobEffect> effectHolder = MOB_EFFECTS.register(
                        regName,
                        () -> new SchoolShredMobEffect(entry.supplier(), type, modifierId, entry.color())
                );

                typeMap.put(type, effectHolder);
            }

            SHRED_MAP.put(entry.name(), typeMap);
        }
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    public static DeferredHolder<MobEffect, MobEffect> getEffect(SchoolType school, ShredType type) {
        String path = school.getId().getPath();
        Map<ShredType, DeferredHolder<MobEffect, MobEffect>> map = SHRED_MAP.get(path);
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