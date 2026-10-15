package com.nf121.examplemod.mixin.cataclysm;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(
    targets = {
        // === 1. 新动画体系基类与具体子类 ===
        "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.IABoss_monster",
        "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Ancient_Remnant.Ancient_Remnant_Entity",
        "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Maledictus.Maledictus_Entity",
        "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.NewNetherite_Monstrosity.Netherite_Monstrosity_Entity",
        "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Scylla.Scylla_Entity",

        // === 2. 老动画体系基类与具体子类 ===
        "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.LLibrary_Boss_Monster",
        "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ender_Guardian_Entity",
        "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity",
        "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Harbinger_Entity",
        "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Leviathan.The_Leviathan_Entity",

        // === 3. 深海城系怪物 ===
    },
    remap = false // 灾变自己的方法名 DpsCap，不需要混淆映射
)
public abstract class AllCataclysmBossDpsCapMixin {

    /**
     * require = 0 保证即使列表里有某个类没写 DpsCap()（走父类默认），Mixin 也会静默跳过而不会崩溃
     */
    @Inject(method = "DpsCap", at = @At("HEAD"), cancellable = true, require = 0)
    private void cataclysm$overrideAllDpsCaps(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        int lv = 0;
        try {
            // 获取你模组里的等级能力
            Optional<MobTraitCap> opt = ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(entity);
            if (opt.isPresent()) {
                lv = opt.get().getLevel();
            }
        } catch (Exception ignored) {
            // 实体刚生成尚未附加能力时的安全防崩
        }

        // 计算公式：20 * (1 + 0.015) ^ lv
        float customDpsCap = (float) (20.0 * Math.pow(1.0 + 0.015, lv));

        cir.setReturnValue(customDpsCap);
    }
}