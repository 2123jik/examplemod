package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.CurioAffixHelper;
import dev.shadowsoffire.apotheosis.AdventureEvents;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsProjectileCurioMixin {

    /**
     * 1. 拦截弹射物生成（发射瞬间）：
     * 遍历玩家身上洗成【弓/远程】拟态的 Curios 饰品，
     * 触发其 onProjectileFired 特效（如幽灵箭分裂 SpectralShotAffix、鲜血宝石增伤 BloodyArrowBonus）
     */
    @Inject(method = "fireProjectile", at = @At("TAIL"))
    private void apoth_curiosFireProjectile(EntityJoinLevelEvent e, CallbackInfo ci) {
        if (e.getEntity() instanceof Projectile proj) {
            Entity shooter = proj.getOwner();
            if (shooter instanceof LivingEntity user) {
                CurioAffixHelper.forEachCurio(user, curio -> {
                    if (curio.isEmpty()) return;

                    LootCategory cat = LootCategory.forItem(curio);
                    if (cat.isRanged()) {
                        // 触发宝石发射特效 (SocketedGems 源码第 64 行原生支持)
                        SocketHelper.getGems(curio).onProjectileFired(user, proj);

                        // 触发词缀发射特效 (如幽灵箭多重发射)
                        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(curio);
                        for (AffixInstance inst : affixes.values()) {
                            inst.onProjectileFired(user, proj);
                        }
                    }
                });
            }
        }
    }

    /**
     * 2. 拦截弹射物命中瞬间：
     * 触发射手 Curios 饰品上的命中特效（如 MobEffectAffix 射中施加药水效果）
     */
    @Inject(method = "impact", at = @At("TAIL"))
    private void apoth_curiosProjectileImpact(ProjectileImpactEvent e, CallbackInfo ci) {
        Projectile proj = e.getProjectile();
        Entity shooter = proj.getOwner();
        HitResult res = e.getRayTraceResult();

        if (shooter instanceof LivingEntity user && res != null) {
            CurioAffixHelper.forEachCurio(user, curio -> {
                if (curio.isEmpty()) return;

                LootCategory cat = LootCategory.forItem(curio);
                if (cat.isRanged()) {
                    // 【关键修复】：SocketedGems 没有 onProjectileImpact，通过 streamValidGems() 遍历每一个 GemInstance 调用！
                    SocketHelper.getGems(curio).streamValidGems().forEach(inst -> {
                        inst.onProjectileImpact(proj, res);
                    });

                    // 触发词缀命中特效 (如击中附带虚弱/凋零等)
                    Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(curio);
                    for (AffixInstance inst : affixes.values()) {
                        inst.onProjectileImpact(proj, res, res.getType());
                    }
                }
            });
        }
    }
}