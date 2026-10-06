package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.CurioAffixHelper;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(value = EnchantmentHelper.class, remap = false)
public class SafeCurioAffixMixin {

    @Unique
    private static final ThreadLocal<Boolean> IS_TRIGGERING_CURIO_EFFECTS = ThreadLocal.withInitial(() -> false);

    @Inject(
            at = @At("TAIL"),
            method = "doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V"
    )
    private static void apoth_curiosPostAttackEffects(
            ServerLevel level,
            Entity target,
            DamageSource damageSource,
            @Nullable ItemStack itemSource,
            CallbackInfo ci
    ) {
        if (IS_TRIGGERING_CURIO_EFFECTS.get()) {
            return;
        }

        try {
            IS_TRIGGERING_CURIO_EFFECTS.set(true);

            // 1. 攻击者触发攻击特技 (攻击主动触发法术、斩杀、雷击)
            if (damageSource.getEntity() instanceof LivingEntity attacker) {
                CurioAffixHelper.forEachCurio(attacker, curio -> {
                    if (curio.isEmpty()) return;

                    // 神化系统
                    SocketHelper.getGems(curio).doPostAttack(attacker, target);
                    Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(curio);
                    for (AffixInstance inst : affixes.values()) {
                        int old = target.invulnerableTime;
                        target.invulnerableTime = 0;
                        inst.doPostAttack(attacker, target);
                        target.invulnerableTime = old;
                    }

                    // 1.21 附魔系统（攻击方主动法术）
                    triggerCurioEnchantmentPostAttack(level, curio, attacker, target, damageSource, EnchantmentTarget.ATTACKER);
                });
            }

            // 2. 受害者触发被动特技 (受到攻击施放凋零之首、受击反弹)
            if (target instanceof LivingEntity victim) {
                CurioAffixHelper.forEachCurio(victim, curio -> {
                    if (curio.isEmpty()) return;

                    // 神化系统
                    SocketHelper.getGems(curio).doPostHurt(victim, damageSource);
                    Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(curio);
                    for (AffixInstance inst : affixes.values()) {
                        inst.doPostHurt(victim, damageSource);
                    }

                    // 【核心关键】：绕过原版 matchingSlot 限制，强制触发受击附魔！
                    triggerCurioEnchantmentPostAttack(level, curio, victim, victim, damageSource, EnchantmentTarget.VICTIM);
                });
            }

        } finally {
            IS_TRIGGERING_CURIO_EFFECTS.set(false);
        }
    }

    /**
     * 手动构造 EnchantedItemInUse 并直接调用 doPostAttack，
     * 完美跨越原版对 EquipmentSlot (护甲槽位) 的严格检查！
     */
    @Unique
    private static void triggerCurioEnchantmentPostAttack(
            ServerLevel level,
            ItemStack curio,
            LivingEntity owner,
            Entity contextEntity,
            DamageSource damageSource,
            EnchantmentTarget target
    ) {
        ItemEnchantments enchantments = curio.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        // 构造 1.21 所需的 EnchantedItemInUse 运行时凭证（槽位可借用 CHEST 伪装）
        EnchantedItemInUse itemInUse = new EnchantedItemInUse(curio, EquipmentSlot.CHEST, owner, item -> {});

        for (Holder<Enchantment> enchHolder : enchantments.keySet()) {
            int enchLevel = enchantments.getLevel(enchHolder);
            Enchantment enchantment = enchHolder.value();

            // 直接调用 Enchantment 内部的 doPostAttack，不受 matchingSlot 拦截！
            enchantment.doPostAttack(level, enchLevel, itemInUse, target, contextEntity, damageSource);
        }
    }
}