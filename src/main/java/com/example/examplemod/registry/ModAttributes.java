package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.tunes_n_tomes.registry.TSchoolRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.warphan.iss_magicfromtheeast.registries.MFTESchoolRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ExampleMod.MODID);

    public record SchoolNode(String id, Supplier<SchoolType> schoolSupplier) {}

    public static final String PHYSICAL_ID = "physical";

    public static final List<SchoolNode> TOPOLOGICAL_SCHOOLS = List.of(
            new SchoolNode("fire", SchoolRegistry.FIRE),
            new SchoolNode("ice", SchoolRegistry.ICE),
            new SchoolNode("lightning", SchoolRegistry.LIGHTNING),
            new SchoolNode("nature", SchoolRegistry.NATURE),
            new SchoolNode("spirit", MFTESchoolRegistries.SPIRIT),
            new SchoolNode("blood", SchoolRegistry.BLOOD),
            new SchoolNode("ender", SchoolRegistry.ENDER),
            new SchoolNode("eldritch", SchoolRegistry.ELDRITCH),
            new SchoolNode("dune", MFTESchoolRegistries.DUNE),
            new SchoolNode("evocation", SchoolRegistry.EVOCATION),
            new SchoolNode("holy", SchoolRegistry.HOLY),
            new SchoolNode("melody", TSchoolRegistry.MELODY),
            new SchoolNode("symmetry", MFTESchoolRegistries.SYMMETRY)
    );

    public static final Map<String, Supplier<SchoolType>> ID_TO_SCHOOL = new HashMap<>();
    private static final Map<String, DeferredHolder<Attribute, Attribute>> CONVERSION_ENTRIES = new HashMap<>();

    static {
        ID_TO_SCHOOL.put(PHYSICAL_ID, PhysicalSchool.PHYSICAL); // <-- 补这一行
        for (var node : TOPOLOGICAL_SCHOOLS) {
            ID_TO_SCHOOL.put(node.id(), node.schoolSupplier());
        }
        List<String> allNodes = new ArrayList<>();
        allNodes.add(PHYSICAL_ID);
        for (var node : TOPOLOGICAL_SCHOOLS) {
            allNodes.add(node.id());
        }

// 设定最大跨步：1 = 仅相邻(13个)；2 = 相邻或隔一个(25个，推荐)
        int maxStep = 2;

        for (int i = 0; i < allNodes.size(); i++) {
            String from = allNodes.get(i);
            int maxJ = Math.min(i + maxStep, allNodes.size() - 1);
            for (int j = i + 1; j <= maxJ; j++) {
                String to = allNodes.get(j);

                String attrName = "convert_" + from + "_to_" + to;

                DeferredHolder<Attribute, Attribute> entry = ATTRIBUTES.register(
                        attrName,
                        () -> new RangedAttribute(
                                "attribute." + ExampleMod.MODID + "." + attrName,
                                0.0, 0.0, 10.0
                        ).setSyncable(true)
                );

                CONVERSION_ENTRIES.put(from + "_" + to, entry);
            }
        }
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    public static Holder<Attribute> getConversionAttribute(String from, String to) {
        return CONVERSION_ENTRIES.get(from + "_" + to);
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        CONVERSION_ENTRIES.values().forEach(entry -> event.add(EntityType.PLAYER, entry));
    }
}