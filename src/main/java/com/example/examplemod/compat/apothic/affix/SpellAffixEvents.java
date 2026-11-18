package com.example.examplemod.compat.apothic.affix;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.runeword.RunewordDefinition;
import com.example.examplemod.runeword.RunewordManager;
import com.example.examplemod.runeword.RunewordPurityHelper;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class SpellAffixEvents {

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        LivingEntity caster = event.getEntity();
        if (caster == null) return;

        int totalAffixBonus = 0;
        int totalRunewordBonus = 0;

        for (var slotHolder : BuiltInRegs.ENTITY_EQUIPMENT_SLOT.holders().toList()) {
            EntityEquipmentSlot slot = slotHolder.value();

            // 兼容 CurioEquipmentSlot（同时避开部分版本 rangeClosed 越界隐患）
            if (slot instanceof CurioEquipmentSlot(String curioType)) {
                var optHandler = CuriosApi.getCuriosInventory(caster);
                if (optHandler.isPresent()) {
                    var stacksHandler = optHandler.get().getCurios().get(curioType);
                    if (stacksHandler != null) {
                        var dyn = stacksHandler.getStacks();
                        for (int i = 0; i < dyn.getSlots(); i++) {
                            ItemStack stack = dyn.getStackInSlot(i);
                            int[] res = evaluateStack(stack, slotHolder, event);
                            totalAffixBonus += res[0];
                            totalRunewordBonus += res[1];
                        }
                    }
                }
            } else {
                for (ItemStack stack : slot.getStacks(caster)) {
                    int[] res = evaluateStack(stack, slotHolder, event);
                    totalAffixBonus += res[0];
                    totalRunewordBonus += res[1];
                }
            }
        }

        // 限制来自随机词缀的总上限（按需调整，防止无限堆叠）
        int cappedAffixBonus = Math.min(totalAffixBonus, 5);
        int finalBonus = cappedAffixBonus + totalRunewordBonus;

        if (finalBonus > 0) {
            event.addLevels(finalBonus);
        }
    }

    private static int[] evaluateStack(ItemStack stack, Holder<EntityEquipmentSlot> slotHolder, ModifySpellLevelEvent event) {
        if (stack.isEmpty()) return new int[]{0, 0};

        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone() || !cat.getSlots().test(slotHolder)) {
            return new int[]{0, 0};
        }

        int affixBonus = AffixHelper.streamAffixes(stack)
                .mapToInt(inst -> {
                    if (inst.getAffix() instanceof SpellLevelAffix spellAffix) {
                        return spellAffix.getBonusLevels(inst, event.getSpell());
                    }
                    return 0;
                })
                .sum();

        int rwBonus = 0;
        ModDataComponents.ActiveRuneword active = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (active != null && RunewordPurityHelper.isRunewordIntact(stack, active)) {
            RunewordDefinition def = RunewordManager.getAll().get(active.id());
            if (def != null && def.allSpellLevels() > 0) {
                rwBonus = RunewordPurityHelper.scaleSpellLevels(def.allSpellLevels(), active.purity());
            }
        }
        ModDataComponents.CorruptionData corrupt = stack.get(ModDataComponents.CORRUPTION.get());
        if (corrupt != null && !corrupt.bricked()) {
            for (String eff : corrupt.effects()) {
                if ("all_spell_1".equals(eff)) {
                    rwBonus += 1; // 腐化两次中两次就 +2，完美叠加
                }
            }
        }
        return new int[]{affixBonus, rwBonus};
    }
}