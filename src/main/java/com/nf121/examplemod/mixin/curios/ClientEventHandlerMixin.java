package com.nf121.examplemod.mixin.curios;

import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.client.ClientEventHandler;

import java.util.*;

@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class ClientEventHandlerMixin {

    @Shadow
    private static List<String> getItemStackSlots(ItemStack stack, Player player) {
        return Collections.emptyList();
    }

    @Inject(method = "onAttributeTooltip", at = @At("HEAD"), cancellable = true)
    private void onAttributeTooltipOptimized(AddAttributeTooltipsEvent evt, CallbackInfo ci) {
        ci.cancel();

        AttributeTooltipContext context = evt.getContext();
        ItemStack stack = evt.getStack();
        GatherSkippedAttributeTooltipsEvent skipped = (GatherSkippedAttributeTooltipsEvent) NeoForge.EVENT_BUS.post(new GatherSkippedAttributeTooltipsEvent(stack, context));

        if (skipped.isSkippingAll()) {
            return;
        }

        List<Component> attributesTooltip = new ArrayList<>();
        Player player = context.player();
        List<String> availableSlots = getItemStackSlots(stack, player);

        // 聚合：按属性修饰符内容分组归类槽位
        Map<Multimap<Holder<Attribute>, AttributeModifier>, List<String>> groupedAttributes = new LinkedHashMap<>();

        for (String identifier : availableSlots) {
            SlotContext slotContext = new SlotContext(identifier, player, 0, false, true);
            Multimap<Holder<Attribute>, AttributeModifier> attributes = CuriosApi.getAttributeModifiers(slotContext, CuriosApi.getSlotId(slotContext), stack);
            attributes.values().removeIf((modifier) -> skipped.isSkipped(modifier.id()));

            if (!attributes.isEmpty()) {
                Multimap<Holder<Attribute>, AttributeModifier> matchedKey = null;
                for (Multimap<Holder<Attribute>, AttributeModifier> existing : groupedAttributes.keySet()) {
                    if (existing.equals(attributes)) {
                        matchedKey = existing;
                        break;
                    }
                }

                if (matchedKey != null) {
                    groupedAttributes.get(matchedKey).add(identifier);
                } else {
                    List<String> slots = new ArrayList<>();
                    slots.add(identifier);
                    groupedAttributes.put(attributes, slots);
                }
            }
        }

        // 渲染聚合后的属性文本
        for (Map.Entry<Multimap<Holder<Attribute>, AttributeModifier>, List<String>> entry : groupedAttributes.entrySet()) {
            Multimap<Holder<Attribute>, AttributeModifier> attributes = entry.getKey();
            List<String> slotList = entry.getValue();

            attributesTooltip.add(Component.empty());

            MutableComponent header;
            // 1. 如果仅支持 1 个槽位：使用原生对应的单独词条 (例如 "curios.modifiers.head")
            if (slotList.size() == 1) {
                header = Component.translatable("curios.modifiers." + slotList.get(0)).withStyle(ChatFormatting.GOLD);
            }
            // 2. 如果包含了该物品的所有槽位：复用 Curios 通用饰品词条 ("curios.modifiers.curio")
            else if (slotList.size() >= availableSlots.size()) {
                header = Component.translatable("curios.modifiers.curio").withStyle(ChatFormatting.GOLD);
            }
            // 3. 如果是部分多个槽位：复用 "curios.tooltip.slot" + "curios.identifier.<id>"
            else {
                header = Component.translatable("curios.tooltip.slot").append(" ").withStyle(ChatFormatting.GOLD);
                for (int i = 0; i < slotList.size(); i++) {
                    String id = slotList.get(i);
                    char firstChar = Character.toUpperCase(id.charAt(0));
                    MutableComponent slotName = Component.translatableWithFallback("curios.identifier." + id, firstChar + id.substring(1).toLowerCase());

                    if (i < slotList.size() - 1) {
                        slotName.append(", ");
                    }
                    header.append(slotName.withStyle(ChatFormatting.YELLOW));
                }
            }

            attributesTooltip.add(header);

            if (player != null) {
                AttributeUtil.applyTextFor(stack, attributesTooltip::add, attributes, AttributeTooltipContext.of(player, context, context.flag()));
            }
        }

        evt.addTooltipLines(CuriosApi.getCurio(stack)
                .map((curio) -> curio.getAttributesTooltip(attributesTooltip, context))
                .orElse(attributesTooltip)
                .toArray(new Component[0]));
    }
}