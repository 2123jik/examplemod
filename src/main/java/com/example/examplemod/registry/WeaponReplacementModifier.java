package com.example.examplemod.registry;


import com.example.examplemod.util.WeaponReplacer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class WeaponReplacementModifier extends LootModifier {

    public static final MapCodec<WeaponReplacementModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst).apply(inst, WeaponReplacementModifier::new)
    );

    public WeaponReplacementModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // 如果当前战利品上下文包含死亡的实体（说明是怪物掉落的战利品），直接跳过替换
//        if (context.hasParam(LootContextParams.THIS_ENTITY)) {
//            return generatedLoot;
//        }

        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            ItemStack replaced = WeaponReplacer.tryReplace(stack, context.getRandom());
            if (replaced != stack) {
                generatedLoot.set(i, replaced);
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}