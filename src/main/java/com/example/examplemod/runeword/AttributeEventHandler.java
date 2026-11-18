package com.example.examplemod.runeword;

import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class AttributeEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeEventHandler.class);
    private static final TagKey<Item> SHIELD_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "base/shields")
    );

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        ModDataComponents.ActiveRuneword active = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (active == null) return;

        // 若玩家中途拆掉宝石或重铸破坏了底材条件，直接失效
        if (!RunewordPurityHelper.isRunewordIntact(stack, active)) {
            return;
        }

        RunewordDefinition def = RunewordManager.getAll().get(active.id());
        if (def == null) return;

        EquipmentSlotGroup validSlot = getApplicableSlotGroup(stack);

        for (int i = 0; i < def.attributes().size(); i++) {
            RunewordDefinition.AttrMod mod = def.attributes().get(i);

            Optional<Holder.Reference<Attribute>> attrHolderOpt = BuiltInRegistries.ATTRIBUTE.getHolder(mod.attribute());
            if (attrHolderOpt.isEmpty()) {
                LOGGER.warn("符文之语 [{}] 引用的属性不存在，已跳过: {}", def.id(), mod.attribute());
                continue;
            }
            Holder<Attribute> attrHolder = attrHolderOpt.get();

            AttributeModifier.Operation operation = switch (mod.operation()) {
                case "MULTIPLY_BASE"  -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default               -> AttributeModifier.Operation.ADD_VALUE;
            };

            double scaledAmount = RunewordPurityHelper.scaleAttributeAmount(mod.amount(), active.purity());

            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "runeword_" + def.id() + "_" + active.purity() + "_" + validSlot.getSerializedName() + "_" + i
            );

            event.addModifier(attrHolder, new AttributeModifier(modifierId, scaledAmount, operation), validSlot);
        }
    }

    private static EquipmentSlotGroup getApplicableSlotGroup(ItemStack stack) {
        var equipSlot = stack.getItem().getEquipmentSlot(stack);
        if (equipSlot != null && equipSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return EquipmentSlotGroup.bySlot(equipSlot);
        }
        // 盾牌必须在副手生效
        if (stack.getItem() instanceof ShieldItem || stack.is(SHIELD_TAG)) {
            return EquipmentSlotGroup.OFFHAND;
        }
        return EquipmentSlotGroup.MAINHAND;
    }
}