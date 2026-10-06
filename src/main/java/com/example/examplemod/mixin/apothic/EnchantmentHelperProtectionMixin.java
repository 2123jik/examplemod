package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.CurioAffixHelper;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = EnchantmentHelper.class, remap = false)
public class EnchantmentHelperProtectionMixin {

    /**
     * 让饰品上的所有保护词缀（通用保护、弹射物保护、火焰保护、爆炸减伤）真正生效！
     */
    @Inject(
            method = "getDamageProtection",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void apoth_curiosDamageProtection(
            ServerLevel level,
            LivingEntity entity,
            DamageSource source,
            CallbackInfoReturnable<Float> cir
    ) {
        float[] extraProt = new float[]{ 0.0F };

        CurioAffixHelper.forEachCurio(entity, curio -> {
            if (curio.isEmpty()) return;

            // 1. 宝石提供的各类保护点数（包括弹射物减伤宝石）
            extraProt[0] += SocketHelper.getGems(curio).getDamageProtection(source);

            // 2. 词缀提供的各类保护点数
            Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(curio);
            for (AffixInstance inst : affixes.values()) {
                extraProt[0] += inst.getDamageProtection(source);
            }
        });

        if (extraProt[0] > 0) {
            cir.setReturnValue(cir.getReturnValueF() + extraProt[0]);
        }
    }
}