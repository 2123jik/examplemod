package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.loot.DiabloQualityRoller;
import com.example.examplemod.loot.DropContextBuilder;
import dev.xkmc.l2hostility.init.data.LHConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class EnchantedCountIncreaseFunctionMixin {

    @Shadow @Final private Holder<Enchantment> enchantment;

    @ModifyVariable(
            method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("STORE"),
            ordinal = 0
    )
    private int examplemod$applyDynamicLooting(int originalLevel, ItemStack stack, LootContext context) {
        // 1. 只对「抢夺」附魔生效（因为别的模组可能用这个类配其他附魔）
        if (!this.enchantment.is(Enchantments.LOOTING)) {
            return originalLevel;
        }

        // 2. 确认被击杀的是生物
        Entity victim = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(victim instanceof LivingEntity mob)) {
            return originalLevel;
        }

        // 3. 解析玩家（弓箭/近战时 ATTACKING_ENTITY 就是玩家；宠物/间接击杀可兜底查 LAST_DAMAGE_PLAYER）
        ServerPlayer player = null;
        if (context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer sp) {
            player = sp;
        } else if (context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer sp) {
            player = sp;
        }
        if (player == null) {
            return originalLevel;
        }

        // 4. 计算你的等级差公式（务必加硬上限，防止高难度差导致一次掉落上万个物品炸服！）
        DiabloQualityRoller.DropContext dropCtx = DropContextBuilder.build(
                context.getLevel(), mob, player, context.getLuck()
        );
        int diff = dropCtx.mobLevel() - dropCtx.playerDifficulty();
        if (diff <= 0) { // 如果想“同级也有 +1”，把 <= 0 改成 < 0
            return originalLevel;
        }

        double raw = Math.pow(1.0 + LHConfig.SERVER.healthFactor.getAsDouble(), diff);
        int bonus = Double.isFinite(raw) ? (int) Math.min(10, Math.floor(raw)) : 10; // 示例上限 +10

        return originalLevel + bonus;
    }
}