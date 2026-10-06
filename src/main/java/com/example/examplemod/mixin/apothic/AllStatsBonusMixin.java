package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.AllStatsBonus;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = AllStatsBonus.class, remap = false)
public class AllStatsBonusMixin {

    /**
     * 1. 槽位纠偏：确保全属性宝石镶嵌在 Curios 饰品上时，属性能够正确激活
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

    /**
     * 2. 数值缩放：属性数值与 Tooltip 显示同步缩放为 0.25x
     */
    @Redirect(
            method = {"addModifiers", "getSocketBonusTooltip"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object scaleAllStats(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Float f) {
            return f * 0.25F;
        }
        return val;
    }
}