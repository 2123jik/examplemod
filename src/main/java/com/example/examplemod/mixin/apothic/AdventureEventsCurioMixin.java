package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.CurioAffixHelper;
import dev.shadowsoffire.apotheosis.AdventureEvents;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsCurioMixin {

    @Inject(method = "onDamage", at = @At("TAIL"))
    private void apoth_curiosOnDamage(LivingIncomingDamageEvent e, CallbackInfo ci) {
        LivingEntity ent = e.getEntity();

        DamageSource src = e.getSource();
        float[] amount = new float[]{ e.getAmount() };

        // 遍历所有 Curios 槽位的物品
        CurioAffixHelper.forEachCurio(ent, s -> {
            // 宝石防御减伤
            amount[0] = SocketHelper.getGems(s).onHurt(src, ent, amount[0]);
            // 词缀减伤（如物理/魔法/火焰减伤等）
            Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(s);
            for (AffixInstance inst : affixes.values()) {
                amount[0] = inst.onHurt(src, ent, amount[0]);
            }
        });

        e.setAmount(amount[0]);
    }
}