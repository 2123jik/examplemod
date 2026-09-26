package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public class ApothicAttributeDifficultyHandler {
    private static final ResourceLocation EXTRA_DIFF_RL =
            ResourceLocation.fromNamespaceAndPath("l2hostility", "extra_difficulty");


    private static UUID getOrCreateInstanceUuid(ItemStack stack) {
        UUID uuid = stack.get(ModDataComponents.ITEM_INSTANCE_UUID.get());
        if (uuid == null) {
            uuid = UUID.randomUUID();
            stack.set(
                    ModDataComponents.ITEM_INSTANCE_UUID.get(),
                    uuid
            );
        }
        return uuid;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onStackAttributeModifiers(StackAttributeModifiersEvent event) {
        ItemStack stack = event.getItemStack();
        Holder<Attribute> difficultyHolder = BuiltInRegistries.ATTRIBUTE
                .getHolder(EXTRA_DIFF_RL)
                .orElseThrow(() -> new IllegalStateException(
                        "Attribute not found: " + EXTRA_DIFF_RL
                ));
        List<StackAttributeModifiers.Entry> validModifiers = event.getModifiers()
                .stream()
                .filter(entry -> !difficultyHolder.equals(entry.attribute()))
                .toList();
        long validModifierCount = validModifiers.size();

        if (validModifierCount <= 0) {return;}
        UUID instanceUuid = getOrCreateInstanceUuid(stack);

        ResourceLocation modifierId =
                ResourceLocation.fromNamespaceAndPath(
                        "examplemod",
                        "extra_difficulty/" +
                                instanceUuid.toString().replace("-", "")
                );

        event.replaceModifier(
                difficultyHolder,
                new AttributeModifier(
                        modifierId,
                        validModifierCount,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                validModifiers.getFirst().slots()
        );
    }
}