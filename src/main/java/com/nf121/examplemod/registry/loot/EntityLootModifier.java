package com.nf121.examplemod.registry.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.function.BiFunction;

public class EntityLootModifier extends LootModifier {

    public static final MapCodec<EntityLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(
                    inst.group(
                            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
                            LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(m -> m.rawFunctions)
                    )
            ).apply(inst, EntityLootModifier::new)
    );

    private final Item item;
    private final List<LootItemFunction> rawFunctions;
    // 2. 利用 LootItemFunctions.compose 将多个函数压平成一个单次调用的合成函数
    private final BiFunction<ItemStack, LootContext, ItemStack> combinedFunction;

    public EntityLootModifier(LootItemCondition[] conditionsIn, Item item, List<LootItemFunction> functions) {
        super(conditionsIn);
        this.item = item;
        this.rawFunctions = functions;
        this.combinedFunction = LootItemFunctions.compose(functions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ItemStack stack = new ItemStack(this.item);
stack = this.combinedFunction.apply(stack, context);

        if (!stack.isEmpty()) {
            generatedLoot.add(stack);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}