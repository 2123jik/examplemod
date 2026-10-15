package com.nf121.examplemod.mixin.apothic;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.compat.apothic.CurioSlots;
import com.nf121.examplemod.compat.apothic.CurioStackIdentity;
import com.nf121.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.affix.effect.MultiAttrAffix;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.AttributeBonus;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.AllStatsBonus;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(
        value = {
                AttributeAffix.class,
                MultiAttrAffix.class,
                AttributeBonus.class,
                MultiAttrBonus.class,
                AllStatsBonus.class
        },
        remap = false
)
public abstract class CurioAttributeMixin {

    @Redirect(
            method = "addModifiers",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Ldev/shadowsoffire/apothic_attributes/modifiers/"
                            + "StackAttributeModifiersEvent;"
                            + "addModifier("
                            + "Lnet/minecraft/core/Holder;"
                            + "Lnet/minecraft/world/entity/ai/attributes/"
                            + "AttributeModifier;"
                            + "Ldev/shadowsoffire/apothic_attributes/modifiers/"
                            + "EntitySlotGroup;)Z"
            ),
            require = 1
    )
    private boolean examplemod$adaptCurioModifier(
            StackAttributeModifiersEvent event,
            Holder<Attribute> attribute,
            AttributeModifier modifier,
            EntitySlotGroup originalSlots
    ) {
        ItemStack stack = event.getItemStack();

        EntitySlotGroup curioSlots = CurioSlots.forStack(stack);

        // 非目标饰品沿用原行为。
        if (curioSlots == null) {
            return event.addModifier(
                    attribute,
                    modifier,
                    originalSlots
            );
        }

        CurioStackIdentity.initialize(stack);

        UUID instanceId =
                stack.get(ModDataComponents.ITEM_INSTANCE_UUID.get());

        AttributeModifier resolvedModifier = modifier;

        if (instanceId != null) {
            ResourceLocation originalId = modifier.id();

            ResourceLocation uniqueId =
                    ResourceLocation.fromNamespaceAndPath(
                            ExampleMod.MOD_ID,
                            "curio_modifier/"
                                    + instanceId
                                    + "/"
                                    + originalId.getNamespace()
                                    + "/"
                                    + originalId.getPath()
                    );

            resolvedModifier = new AttributeModifier(
                    uniqueId,
                    modifier.amount(),
                    modifier.operation()
            );
        }

        return event.addModifier(
                attribute,
                resolvedModifier,
                curioSlots
        );
    }
}
