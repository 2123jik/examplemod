package com.nf121.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.Apoth.Components;
import dev.shadowsoffire.apotheosis.Apoth.LootCategories;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    // 神化内置的防具生命值词缀 ID（可在神化 jar 包里的 data/apotheosis/affixes/ 确认具体文件名）
    @Unique
    private static final ResourceLocation HEALTH_AFFIX_ID =
            ResourceLocation.fromNamespaceAndPath("apotheosis", "armor/attribute/blessed");

    @Inject(
            method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/loot/LootCategory;Ldev/shadowsoffire/apotheosis/loot/LootRarity;Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private static void injectGuaranteedHealthAffix(ItemStack stack, LootCategory cat, LootRarity rarity, GenContext ctx, CallbackInfoReturnable<ItemStack> cir) {
        // 1. 判断是否属于防具类型（头、胸、腿、脚）
        if (isArmorCategory(cat)) {
            DynamicHolder<Affix> healthHolder = AffixRegistry.INSTANCE.holder(HEALTH_AFFIX_ID);

            // 检查词缀是否加载且未被禁用
            if (healthHolder.isBound()) {
                Affix healthAffix = healthHolder.get();

                // 2. 检查该词缀是否适用于当前物品类型和稀有度
                if (healthAffix.canApplyTo(stack, cat, rarity)) {
                    ItemAffixes currentAffixes = (ItemAffixes) stack.getOrDefault(Components.AFFIXES, ItemAffixes.EMPTY);

                    // 3. 避免重复添加：如果前面数据包的 LootRule 已经 roll 出了该词缀，则跳过
                    if (!currentAffixes.keySet().contains(healthHolder)) {
                        // 4. 生成 0.0F ~ 1.0F 的随机 roll 点（词缀浮动等级）
                        float step = ctx.rand().nextFloat();

                        // 5. 利用 ItemAffixes.Builder 将生命值词缀写入
                        ItemAffixes updated = currentAffixes.toBuilder()
                                .put(healthHolder, step)
                                .build();

                        // 6. 写回物品的数据组件（Data Components）
                        stack.set(Components.AFFIXES, updated);
                    }
                }
            }
        }
    }

    @Unique
    private static boolean isArmorCategory(LootCategory cat) {
        return cat == LootCategories.HELMET
                || cat == LootCategories.CHESTPLATE
                || cat == LootCategories.LEGGINGS
                || cat == LootCategories.BOOTS;
    }
}