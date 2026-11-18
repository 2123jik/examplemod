package com.example.examplemod.mixin.apothic;

import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.*;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AffixHelper.class, remap = false)
public abstract class AffixHelperMixin {

    @Shadow
    private static List<AffixInstance> getMaliceTargets(ItemStack stack) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean canApplyMalice(List<AffixInstance> targets) {
        throw new AssertionError();
    }

    @Inject(method = "canApplyMalice(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private static void onCanApplyMalice(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var data = stack.get(ModDataComponents.CORRUPTION.get());
        if (data != null && (data.bricked() || data.count() >= 2)) {
            cir.setReturnValue(false);
            return;
        }
        // 放行符文之语 / 有孔底材（否则原版要求必须 >=2 条神化词缀）
        if (stack.has(ModDataComponents.ACTIVE_RUNEWORD.get()) || SocketHelper.getSockets(stack) > 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "applyMalice", at = @At("HEAD"), cancellable = true)
    private static void onApplyMalice(Player player, ItemStack stack, CallbackInfo ci) {
        var prev = stack.get(ModDataComponents.CORRUPTION.get());
        if (prev != null && (prev.bricked() || prev.count() >= 2)) {
            ci.cancel();
            return;
        }

        int seed = player.getPersistentData().getInt("apoth_reforge_seed");
        RandomSource rand = new SingleThreadedRandomSource(seed);

        int newCount = (prev == null ? 0 : prev.count()) + 1;
        List<String> effects = prev == null ? new ArrayList<>() : new ArrayList<>(prev.effects());
        boolean canVanillaMalice = canApplyMalice(getMaliceTargets(stack));

        float roll = rand.nextFloat();

        // 1. 20% 变砖
        if (roll < 0.20f) {
            player.getPersistentData().putInt("apoth_reforge_seed", player.getRandom().nextInt());
            stack.remove(ModDataComponents.ACTIVE_RUNEWORD.get());
            stack.remove(Apoth.Components.SOCKETED_GEMS);
            stack.remove(Apoth.Components.AFFIXES);
            stack.remove(Apoth.Components.RARITY);
            stack.remove(Apoth.Components.AFFIX_NAME);
            SocketHelper.setSockets(stack, 0);
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, true, List.of()));
            ci.cancel();
            return;
        }

        // 2. 25% 额外 +1 孔
        if (roll < 0.45f && SocketHelper.getSockets(stack) < 8) {
            player.getPersistentData().putInt("apoth_reforge_seed", player.getRandom().nextInt());
            SocketHelper.setSockets(stack, SocketHelper.getSockets(stack) + 1);
            effects.add("extra_socket");
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, false, effects));
            stack.set(Apoth.Components.TOUCHED_BY_MALICE, newCount);
            ci.cancel();
            return;
        }

// 3. 25% 给可叠加的全法术等级 +1（两次都中就变成 +2）
        if (roll < 0.70f) {
            player.getPersistentData().putInt("apoth_reforge_seed", player.getRandom().nextInt());
            effects.add("all_spell_1");
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, false, effects));
            stack.set(Apoth.Components.TOUCHED_BY_MALICE, newCount);
            ci.cancel();
            return;
        }

        // 4. 剩下 30%：按当前物品上下文，直接新增 1 条 200% (2.0F) 的随机兼容词缀（不硬编码）
        if (addRandomContextAffix(stack, rand, AffixHelper.MALICE_LEVEL)) {
            player.getPersistentData().putInt("apoth_reforge_seed", player.getRandom().nextInt());
            effects.add("random_affix_2x");
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, false, effects));
            stack.set(Apoth.Components.TOUCHED_BY_MALICE, newCount);
            ci.cancel();
            return;
        }

        // 5. 兜底（比如 common 符文之语找不到支持 common 稀有度的词缀时）：若满足原版条件则放行 200% 强化，否则转为 all_spell_1 避免浪费次数
        if (canVanillaMalice) {
            effects.add("malice_vanilla");
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, false, effects));
        } else {
            player.getPersistentData().putInt("apoth_reforge_seed", player.getRandom().nextInt());
            effects.add("all_spell_1");
            stack.set(ModDataComponents.CORRUPTION.get(), new ModDataComponents.CorruptionData(newCount, false, effects));
            stack.set(Apoth.Components.TOUCHED_BY_MALICE, newCount);
            ci.cancel();
        }}

    private static boolean addRandomContextAffix(ItemStack stack, RandomSource rand, float level) {
        var rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound()) return false;

        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone()) return false;

        LootRarity rarity = rarityHolder.get();
        ItemAffixes existing = stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY);

        List<DynamicHolder<Affix>> candidates = new ArrayList<>();
        for (Affix affix : AffixRegistry.INSTANCE.getValues()) {
            DynamicHolder<Affix> holder = AffixRegistry.INSTANCE.holder(affix);
            if (existing.keySet().contains(holder)) continue;
            if (!affix.canApplyTo(stack, cat, rarity)) continue;

            // 互斥检查：不与装备已有词缀冲突
            boolean conflict = existing.keySet().stream().anyMatch(ex ->
                    ex.isBound() && (ex.get().definition().exclusiveSet().contains(holder)
                            || affix.definition().exclusiveSet().contains(ex))
            );
            if (!conflict) {
                candidates.add(holder);
            }
        }

        if (candidates.isEmpty()) return false;

        DynamicHolder<Affix> chosen = candidates.get(rand.nextInt(candidates.size()));
        ItemAffixes.Builder builder = existing.toBuilder();
        builder.put(chosen, level);
        AffixHelper.setAffixes(stack, builder.build());
        return true;
    }
}