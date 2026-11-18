package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.registry.ModDataComponents;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
import java.util.Optional;
import java.util.TreeMap;

@Mixin(value = RandomizeSpellFunction.class, remap = false)
public class MixinRandomizeSpellFunction {

    @Shadow @Final private NumberProvider qualityRange;
    @Shadow @Final private SpellFilter applicableSpells;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void interceptScrollLoot(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() instanceof Scroll) {
            // 如果已经被标记为已鉴定，则跳过
            if (stack.has(ModDataComponents.ALREADY_IDENTIFIED.get())) return;

            List<AbstractSpell> pool = this.applicableSpells.getApplicableSpells();
            if (pool.isEmpty()) return;

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
            if (sampleSpell.equals(SpellRegistry.none())) return;

            float quality = this.qualityRange.getFloat(context);
            int spellLevel = 1 + Math.round(quality * (float) (sampleSpell.getMaxLevel() - 1));
            SpellRarity officialRarity = sampleSpell.getRarity(spellLevel);

            // 1. 设置最大堆叠
            stack.set(DataComponents.MAX_STACK_SIZE, 64);

            // 2. 检查是否有学派过滤
            Optional<ResourceLocation> schoolFilter = Optional.empty();
            int totalEnabled = SpellRegistry.getEnabledSpells().size();
            if (pool.size() < totalEnabled && !pool.isEmpty()) {
                SchoolType firstSchool = pool.get(0).getSchoolType();
                if (pool.stream().allMatch(s -> s.getSchoolType() == firstSchool)) {
                    schoolFilter = Optional.of(firstSchool.getId());
                }
            }

            // 3. 挂载类型安全的 DataComponent
            stack.set(ModDataComponents.IS_UNIDENTIFIED, true);
            stack.set(ModDataComponents.SCROLL_INFO.get(),
                    new ModDataComponents.UnidentifiedScrollData(officialRarity, schoolFilter));

            cir.setReturnValue(stack);
        }
    }
}