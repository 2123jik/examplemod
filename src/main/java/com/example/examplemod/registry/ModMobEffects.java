package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.event.spell.UnifiedSpellDamageBridge;
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
            DeferredRegister.create(Registries.MOB_EFFECT, ExampleMod.MOD_ID);

    public enum ShredType {
        FLAT("shred_flat", AttributeModifier.Operation.ADD_VALUE),
        TOTAL("shred_total", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        public final String id;
        public final AttributeModifier.Operation operation;

        ShredType(String id, AttributeModifier.Operation operation) {
            this.id = id;
            this.operation = operation;
        }
    }

    private static final Map<String, Map<ShredType, DeferredHolder<MobEffect, MobEffect>>> SHRED_MAP = new HashMap<>();

    // 13 个学派及其官方精确色值
    public static final List<SchoolEntry> REGISTERED_SCHOOLS = List.of(
            new SchoolEntry("fire", SchoolRegistry.FIRE, 0xFFAA00),
            new SchoolEntry("ice", SchoolRegistry.ICE, 13695487),
            new SchoolEntry("lightning", SchoolRegistry.LIGHTNING, 0x55FFFF),
            new SchoolEntry("holy", SchoolRegistry.HOLY, 16775380),
            new SchoolEntry("ender", SchoolRegistry.ENDER, 0xFF55FF),
            new SchoolEntry("blood", SchoolRegistry.BLOOD, 0xAA0000),
            new SchoolEntry("evocation", SchoolRegistry.EVOCATION, 0xFFFFFF),
            new SchoolEntry("nature", SchoolRegistry.NATURE, 0x55FF55),
            new SchoolEntry("eldritch", SchoolRegistry.ELDRITCH, 1016732),
            new SchoolEntry("melody", TSchoolRegistry.MELODY, 13631442),
            new SchoolEntry("symmetry", MFTESchoolRegistries.SYMMETRY, 41836),
            new SchoolEntry("spirit", MFTESchoolRegistries.SPIRIT, 40388),
            new SchoolEntry("dune", MFTESchoolRegistries.DUNE, 16731392)
    );

    static {
        for (SchoolEntry entry : REGISTERED_SCHOOLS) {
            Map<ShredType, DeferredHolder<MobEffect, MobEffect>> typeMap = new EnumMap<>(ShredType.class);

            for (ShredType type : ShredType.values()) {
                String regName = entry.name() + "_" + type.id;
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, regName + "_mod");

                DeferredHolder<MobEffect, MobEffect> holder = MOB_EFFECTS.register(regName, () ->
                        new SchoolShredMobEffect(entry.supplier(), type, modifierId, entry.color())
                );

                typeMap.put(type, holder);
            }

            SHRED_MAP.put(entry.name(), typeMap);
        }
    }

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }

    public static DeferredHolder<MobEffect, MobEffect> getEffect(SchoolType school, ShredType type) {
        String path = school.getId().getPath();
        Map<ShredType, DeferredHolder<MobEffect, MobEffect>> map = SHRED_MAP.get(path);
        return map != null ? map.get(type) : null;
    }

    // 结构体携带颜色信息
    public record SchoolEntry(String name, Supplier<SchoolType> supplier, int color) {}

    public static class SchoolShredMobEffect extends MobEffect {
        private final Supplier<SchoolType> schoolSupplier;
        private final ShredType shredType;
        private final ResourceLocation modifierId;

        public SchoolShredMobEffect(Supplier<SchoolType> schoolSupplier, ShredType shredType, ResourceLocation modifierId, int color) {
            // 这里传入精确颜色，游戏悬浮窗的“颜色代码：#xxxxxx”就会显示为该学派的颜色！
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