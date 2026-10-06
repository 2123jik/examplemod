package com.example.examplemod.compat;

import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;

public class ModLootCategories {

    public static final String MODID = "examplemod";

    public static final Map<String, EntitySlotGroup> REGISTERED_GROUPS = new HashMap<>();

    public static final String[] SLOTS = {
            "ring", "necklace", "belt", "charm", "head", "body", "bracelet", "back", "hands", "feet", "curio",
            "artifact_belt", "artifact_body", "artifact_bracelet", "artifact_head", "artifact_necklace"
    };

    public static void onRegister(RegisterEvent event) {
        // 1. 注册 Curio 装备槽
        if (event.getRegistryKey().equals(BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key())) {
            event.register(BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(), helper -> {
                for (String slotType : SLOTS) {
                    helper.register(
                            ResourceLocation.fromNamespaceAndPath(MODID, slotType),
                            new CurioEquipmentSlot(slotType)
                    );
                }
            });
        }

        // 2. 注册槽位组（供属性修饰符生效）
        if (event.getRegistryKey().equals(BuiltInRegs.ENTITY_SLOT_GROUP.key())) {
            event.register(BuiltInRegs.ENTITY_SLOT_GROUP.key(), helper -> {
                for (String slotType : SLOTS) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, slotType);

                    Holder<EntityEquipmentSlot> slotHolder = BuiltInRegs.ENTITY_EQUIPMENT_SLOT.getHolder(id).orElse(null);
                    if (slotHolder != null) {
                        EntitySlotGroup group = new EntitySlotGroup(id, HolderSet.direct(slotHolder));
                        helper.register(id, group);
                        REGISTERED_GROUPS.put(slotType, group);
                    }
                }
            });
        }
    }

    public static EntitySlotGroup getGroupForStack(ItemStack stack) {
        var slots = CuriosApi.getItemStackSlots(stack, true);
        if (!slots.isEmpty()) {
            String firstSlot = slots.keySet().iterator().next();
            if (REGISTERED_GROUPS.containsKey(firstSlot)) {
                return REGISTERED_GROUPS.get(firstSlot);
            }
        }
        return REGISTERED_GROUPS.getOrDefault("curio", REGISTERED_GROUPS.get("necklace"));
    }
}