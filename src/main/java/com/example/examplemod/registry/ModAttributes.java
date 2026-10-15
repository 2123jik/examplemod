package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.tunes_n_tomes.registry.TSchoolRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
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

// 核心修复 2：必须明确指定 modid 和 bus = Bus.MOD，才能接收 EntityAttributeModificationEvent！
@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, ExampleMod.MOD_ID);


    // ==================== DAG 伤害转化拓扑定义 ====================
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
    private static final Map<String, Holder<Attribute>> CONVERSION_ATTRIBUTES = new HashMap<>();

    // 核心优化 3：使用静态块自动执行，类加载时 100% 注册完毕，杜绝漏调
    static {
        for (var node : TOPOLOGICAL_SCHOOLS) {
            ID_TO_SCHOOL.put(node.id(), node.schoolSupplier());
        }

        List<String> allNodes = new ArrayList<>();
        allNodes.add(PHYSICAL_ID);
        for (var node : TOPOLOGICAL_SCHOOLS) {
            allNodes.add(node.id());
        }

        for (int i = 0; i < allNodes.size(); i++) {
            String from = allNodes.get(i);
            for (int j = i + 1; j < allNodes.size(); j++) {
                String to = allNodes.get(j);

                String attrName = "convert_" + from + "_to_" + to;
                var holder = ATTRIBUTES.register(attrName, () -> new RangedAttribute(
                        "attribute." + ExampleMod.MOD_ID + "." + attrName,
                        0.0, 0.0, 10.0
                ).setSyncable(true));

                CONVERSION_ATTRIBUTES.put(from + "_" + to, holder);
            }
        }
    }

    public static Holder<Attribute> getConversionAttribute(String from, String to) {
        return CONVERSION_ATTRIBUTES.get(from + "_" + to);
    }

    // 核心修复 1：一行代码安全注入所有属性，杜绝重复添加导致的 Crash！
    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        ATTRIBUTES.getEntries().forEach(holder -> {
            event.add(EntityType.PLAYER, holder);
        });
    }
}