package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.effect.MultiAttrAffix;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MultiAttrAffix.class, remap = false)
public class MultiAttrAffixMixin {

    @Redirect(
            method = "addModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/loot/LootCategory;getSlots()Ldev/shadowsoffire/apothic_attributes/modifiers/EntitySlotGroup;"
            )
    )
    private EntitySlotGroup redirectCurioSlotGroupMulti(
            LootCategory cat,
            AffixInstance inst,
            StackAttributeModifiersEvent event
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