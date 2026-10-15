package com.nf121.examplemod.registry.loot;

import com.nf121.examplemod.util.ChestWeaponReplacer;
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
        // 关键安全点 1：如果是实体死亡产生的战利品（打怪掉落），跳过！
        // 只对地牢箱子、要塞、远古城市、下界要塞等箱子/容器生效
        if (context.hasParam(LootContextParams.THIS_ENTITY)) {
            return generatedLoot;
        }

        // 关键安全点 2：平级替换原版剑斧，保留原本战利品表的附魔和耐久属性
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            ItemStack replaced = ChestWeaponReplacer.tryReplaceChestWeapon(stack, context.getRandom());
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