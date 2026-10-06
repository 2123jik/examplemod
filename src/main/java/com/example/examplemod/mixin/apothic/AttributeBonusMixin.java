package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.AttributeBonus;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = AttributeBonus.class, remap = false)
public class AttributeBonusMixin {

    /**
     * 1. 槽位重定向：将宝石槽位精准纠偏为饰品（Curio）对应的槽位组
     */
    @Redirect(
            method = "addModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/loot/LootCategory;getSlots()Ldev/shadowsoffire/apothic_attributes/modifiers/EntitySlotGroup;"
            )
    )
    private EntitySlotGroup redirectCurioSlotGroupForGem(
            LootCategory cat,
            GemInstance gemInst,
            StackAttributeModifiersEvent event
    ) {
        ItemStack hostStack = event.getItemStack();
        if (CurioMimicHelper.isCurio(hostStack)) {
            EntitySlotGroup curioGroup = ModLootCategories.getGroupForStack(hostStack);
            if (curioGroup != null) {
                return curioGroup;
            }
        }
        return cat.getSlots();
    }

    /**
     * 2. 数值缩放：仅拦截 createModifier 内的 Map.get
     * 因为 addModifiers 和 getSocketBonusTooltip 都会调用 createModifier，
     * 所以只需拦截这里，逻辑与 Tooltip 就会 100% 自动对齐为 0.25x！
     */
    @Redirect(
            method = "createModifier",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object scaleAttributeBonusValue(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Double d) {
            return d * 0.25D;
        }
        if (val instanceof Float f) {
            return f * 0.25F;
        }
        return val;
    }
}