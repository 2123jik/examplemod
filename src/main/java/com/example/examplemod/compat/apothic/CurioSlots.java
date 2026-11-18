package com.example.examplemod.compat.apothic;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@EventBusSubscriber
public final class CurioSlots {

    /*
     * 保留你原工程中的注册 ID。
     *
     * 整合包有其他槽位时，在这里追加它的准确类型名称。
     * 这些注册不会创建玩家饰品栏，玩家栏位仍由 Curios 配置决定。
     */
    private static final List<String> SLOT_TYPES = List.of(
            "ring",
            "necklace",
            "belt",
            "charm",
            "head",
            "body",
            "bracelet",
            "back",
            "hands",
            "feet",
            "curio",
            "artifact_belt",
            "artifact_body",
            "artifact_bracelet",
            "artifact_head",
            "artifact_necklace"
    );

    private static final Map<
            String,
            Holder<EntityEquipmentSlot>
            > SLOT_HOLDERS = new HashMap<>();

    private static final Map<String, EntitySlotGroup> SINGLE_GROUPS =
            new HashMap<>();

    private static final Map<List<String>, EntitySlotGroup> COMBINED_GROUPS =
            new HashMap<>();

    private CurioSlots() {
    }
@SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(
                BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key()
        )) {
            event.register(
                    BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(),
                    helper -> {
                        for (String slotType : SLOT_TYPES) {
                            helper.register(
                                    id(slotType),
                                    new CurioEquipmentSlot(slotType)
                            );
                        }
                    }
            );
        }

        if (event.getRegistryKey().equals(
                BuiltInRegs.ENTITY_SLOT_GROUP.key()
        )) {
            event.register(
                    BuiltInRegs.ENTITY_SLOT_GROUP.key(),
                    helper -> {
                        SLOT_HOLDERS.clear();
                        SINGLE_GROUPS.clear();
                        COMBINED_GROUPS.clear();

                        for (String slotType : SLOT_TYPES) {
                            ResourceLocation slotId = id(slotType);

                            Holder<EntityEquipmentSlot> holder =
                                    BuiltInRegs.ENTITY_EQUIPMENT_SLOT
                                            .getHolder(slotId)
                                            .orElseThrow();

                            EntitySlotGroup group = new EntitySlotGroup(
                                    slotId,
                                    HolderSet.direct(holder)
                            );

                            helper.register(slotId, group);

                            SLOT_HOLDERS.put(slotType, holder);
                            SINGLE_GROUPS.put(slotType, group);
                        }
                    }
            );
        }
    }

    /**
     * 返回物品支持的所有已注册 Curios 槽位组成的属性组。
     */
    @Nullable
    public static EntitySlotGroup forStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        List<String> types =
                CuriosApi.getItemStackSlots(stack, true)
                        .keySet()
                        .stream()
                        .filter(SLOT_HOLDERS::containsKey)
                        .sorted()
                        .toList();

        if (types.isEmpty()) {
            return null;
        }

        if (types.size() == 1) {
            return SINGLE_GROUPS.get(types.getFirst());
        }

        return COMBINED_GROUPS.computeIfAbsent(
                types,
                CurioSlots::createCombinedGroup
        );
    }

    private static EntitySlotGroup createCombinedGroup(List<String> types) {
        List<Holder<EntityEquipmentSlot>> holders = new ArrayList<>();

        StringBuilder path = new StringBuilder("curio_combined");

        for (String type : types) {
            holders.add(SLOT_HOLDERS.get(type));

            // 长度前缀避免不同槽位名称组合得到相同 ID。
            path.append('/')
                    .append(type.length())
                    .append('_')
                    .append(type);
        }

        /*
         * 这是事件中的临时组合组。
         * 原生 Curios 桥接使用 group.test(slotHolder) 匹配其中的真实槽位。
         * 不将这个临时组写入物品组件。
         */
        return new EntitySlotGroup(
                id(path.toString()),
                HolderSet.direct(holders)
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                ExampleMod.MODID,
                path
        );
    }
}
