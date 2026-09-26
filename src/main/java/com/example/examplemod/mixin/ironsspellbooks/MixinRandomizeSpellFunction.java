package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

@Mixin(value = RandomizeSpellFunction.class, remap = false)
public class MixinRandomizeSpellFunction {

    @Shadow @Final private NumberProvider qualityRange;
    @Shadow @Final private SpellFilter applicableSpells;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void interceptScrollLoot(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() instanceof Scroll) {
            List<AbstractSpell> pool = this.applicableSpells.getApplicableSpells();
            if (pool.isEmpty()) {
                return;
            }

            // 1. 100% 走官方原版的加权算法抽取一次法术与计算等级
            NavigableMap<Integer, AbstractSpell> spellList = new TreeMap<>();
            int total = 0;
            for (AbstractSpell entry : pool) {
                total += switch (SpellRarity.values()[entry.getMinRarity()]) {
                    case COMMON -> 40;
                    case UNCOMMON -> 30;
                    case RARE -> 15;
                    case EPIC -> 8;
                    case LEGENDARY -> 4;
                };
                spellList.put(total, entry);
            }

            AbstractSpell sampleSpell = spellList.higherEntry(context.getRandom().nextInt(total)).getValue();
            if (sampleSpell.equals(SpellRegistry.none())) {
                return;
            }

            // 2. 官方等级公式：计算真实等级
            float quality = this.qualityRange.getFloat(context);
            int spellLevel = 1 + Math.round(quality * (float) (sampleSpell.getMaxLevel() - 1));

            // 3. 【核心】：直接调用官方方法获取该法术在该等级下的真实官方稀有度！
            SpellRarity officialRarity = sampleSpell.getRarity(spellLevel);

            // 4. 强制 64 堆叠
            stack.set(DataComponents.MAX_STACK_SIZE, 64);

            // 5. 写入官方稀有度与学派过滤（如果有）
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putBoolean("is_unidentified", true);
                tag.putString("spell_rarity", officialRarity.name());

                // 兼容学派箱子
                int totalEnabled = SpellRegistry.getEnabledSpells().size();
                if (pool.size() < totalEnabled && !pool.isEmpty()) {
                    SchoolType firstSchool = pool.get(0).getSchoolType();
                    if (pool.stream().allMatch(s -> s.getSchoolType() == firstSchool)) {
                        tag.putString("school_filter", firstSchool.getId().toString());
                    }
                }
            });

            cir.setReturnValue(stack);
        }
    }
}