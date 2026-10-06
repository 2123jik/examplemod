package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MultiAttrBonus.class, remap = false)
public class MultiAttrBonusMixin {

    /**
     * 重定向槽位：使复合/多属性宝石在 Curios 饰品槽上正常生效
     */
    @Redirect(
            method = "addModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/loot/LootCategory;getSlots()Ldev/shadowsoffire/apothic_attributes/modifiers/EntitySlotGroup;"
            )
    )
    private EntitySlotGroup redirectCurioSlotGroupForMultiAttr(
            LootCategory cat,
            GemInstance inst,
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
}