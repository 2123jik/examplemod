package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AttributeAffix.class, remap = false)
public class AttributeAffixMixin {

    /**
     * 重定向 cat.getSlots() 调用：
     * 如果当前处理的物品是 Curio 饰品，返回真实的 Curio 槽位组（如戒指、项链）；
     * 如果是原版装备，保持原样返回 cat.getSlots()。
     */
    @Redirect(
            method = "addModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/loot/LootCategory;getSlots()Ldev/shadowsoffire/apothic_attributes/modifiers/EntitySlotGroup;"
            )
    )
    private EntitySlotGroup redirectCurioSlotGroup(
            LootCategory cat,
            AffixInstance inst,
            StackAttributeModifiersEvent event // 捕获外层 addModifiers 方法的入参
    ) {
        if (CurioMimicHelper.isCurio(inst.stack())) {
            EntitySlotGroup curioGroup = ModLootCategories.getGroupForStack(inst.stack());
            if (curioGroup != null) {
                return curioGroup;
            }
        }
        return cat.getSlots();
    }
}