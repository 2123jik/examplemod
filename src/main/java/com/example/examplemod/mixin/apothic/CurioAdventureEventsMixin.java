package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.apothic.CurioEffectHooks;
import com.example.examplemod.compat.apothic.CurioProjectileHooks;
import com.example.examplemod.compat.apothic.CurioTelepathyHooks;
import dev.shadowsoffire.apotheosis.AdventureEvents;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdventureEvents.class, remap = false)
public abstract class CurioAdventureEventsMixin {

    @Inject(method = "onDamage", at = @At("TAIL"), require = 1)
    private void examplemod$hurt(
            LivingIncomingDamageEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()
                && !event.getEntity().level().isClientSide()) {
            event.setAmount(
                    CurioEffectHooks.onHurt(
                            event.getEntity(),
                            event.getSource(),
                            event.getAmount()
                    )
            );
        }
    }

    @Inject(method = "shieldBlock", at = @At("TAIL"), require = 1)
    private void examplemod$shield(
            LivingShieldBlockEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()
                && !event.getEntity().level().isClientSide()) {
            event.setBlockedDamage(
                    CurioEffectHooks.onShieldBlock(
                            event.getEntity(),
                            event.getDamageSource(),
                            event.getBlockedDamage()
                    )
            );
        }
    }



    @Inject(method = "drops", at = @At("TAIL"), require = 1)
    private void examplemod$entityLoot(
            LivingDropsEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()
                && !event.getEntity().level().isClientSide()) {
            CurioEffectHooks.modifyEntityLoot(event);
        }
    }

    @Inject(method = "fireProjectile", at = @At("RETURN"), require = 1)
    private void examplemod$projectileFired(
            EntityJoinLevelEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()
                && !event.loadedFromDisk()
                && event.getEntity() instanceof Projectile projectile) {
            CurioProjectileHooks.onFired(projectile);
        }
    }

    @Inject(method = "impact", at = @At("TAIL"), require = 1)
    private void examplemod$projectileImpact(
            ProjectileImpactEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()) {
            CurioProjectileHooks.onImpact(
                    event.getProjectile(),
                    event.getRayTraceResult()
            );
        }
    }





    @Inject(
            method =
                    "dropsLowest("
                            + "Lnet/neoforged/neoforge/event/entity/living/"
                            + "LivingDropsEvent;)V",
            at = @At("TAIL"),
            require = 1
    )
    private void examplemod$telepathyEntity(
            LivingDropsEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()) {
            CurioTelepathyHooks.entityDrops(event);
        }
    }

    @Inject(
            method =
                    "dropsLowest("
                            + "Lnet/neoforged/neoforge/event/level/"
                            + "BlockDropsEvent;)V",
            at = @At("TAIL"),
            require = 1
    )
    private void examplemod$telepathyBlock(
            BlockDropsEvent event,
            CallbackInfo ci
    ) {
        if (!event.isCanceled()) {
            CurioTelepathyHooks.blockDrops(event);
        }
    }
}
