package com.example.examplemod.trim;

import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.Optional;

import static com.example.examplemod.ExampleMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class AttributeEventHandler {

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        // 检查是否具备激活的符文之语
        String wordId = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (wordId == null) return;

        RunewordDefinition def = RunewordManager.RUNEWORDS.get(wordId);
        if (def == null) return;

        // 根据装备类型确定生效槽位（主手、副手、头、胸、腿、足）
        EquipmentSlotGroup validSlot = getApplicableSlotGroup(stack);

        for (int i = 0; i < def.attributes().size(); i++) {
            RunewordDefinition.AttrMod mod = def.attributes().get(i);

            // 安全获取属性：模组未安装或属性拼写错误时跳过，避免爆服
            Optional<Holder.Reference<Attribute>> attrHolderOpt = BuiltInRegistries.ATTRIBUTE.getHolder(mod.attribute());
            if (attrHolderOpt.isEmpty()) {
                continue;
            }
            Holder<Attribute> attrHolder = attrHolderOpt.get();

            AttributeModifier.Operation operation = switch (mod.operation()) {
                case "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default -> AttributeModifier.Operation.ADD_VALUE;
            };

            // 构造 Modifier ID：加入 validSlot.getSerializedName()，彻底杜绝双持相同符文之语时的 ID 碰撞
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    MOD_ID,
                    "runeword_" + wordId + "_" + validSlot.getSerializedName() + "_" + i
            );

            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    mod.amount(),
                    operation
            );

            event.addModifier(attrHolder, modifier, validSlot);
        }
    }

    private static EquipmentSlotGroup getApplicableSlotGroup(ItemStack stack) {
        // 如果是盔甲类，返回对应防具槽位；如果是武器/盾牌，默认为主手和副手
        var equipSlot = stack.getItem().getEquipmentSlot(stack);
        if (equipSlot != null && equipSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return EquipmentSlotGroup.bySlot(equipSlot);
        }
        return EquipmentSlotGroup.MAINHAND;
    }
}