package com.nf121.examplemod.mixin.ironsspellbooks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Pseudo
@Mixin(value = DamageSources.class, remap = false)
public abstract class DamageSourcesFixMixin {

    // Unique 声明：使用支持自动超时的缓存容器替换原有的 HashMap
    // 线程安全，且写入 10 秒后自动清除，彻底解决内存泄漏
    @Unique
    private static final Cache<UUID, Integer> examplemod$KNOCKBACK_IMMUNES_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    /**
     * @author examplemod
     * @reason 修复 knockbackImmunes 导致的内存泄漏与 HashMap 线程安全隐患
     */
    @Overwrite
    public static void ignoreNextKnockback(LivingEntity livingEntity) {
        if (livingEntity.getServer() != null) {
            int tickCount = livingEntity.getServer().getTickCount();
            examplemod$KNOCKBACK_IMMUNES_CACHE.put(livingEntity.getUUID(), tickCount);
        }
    }

    /**
     * 接管 cancelKnockback 事件逻辑，改用 Cache 进行查询与移除
     */
    @Inject(method = "cancelKnockback", at = @At("HEAD"), cancellable = true)
    private static void fixCancelKnockback(LivingKnockBackEvent event, CallbackInfo ci) {
        var entity = event.getEntity();
        if (entity != null && entity.getServer() != null) {
            UUID uuid = entity.getUUID();
            Integer recordedTick = examplemod$KNOCKBACK_IMMUNES_CACHE.getIfPresent(uuid);

            if (recordedTick != null) {
                if (entity.getServer().getTickCount() - recordedTick <= 1) {
                    event.setCanceled(true);
                }
                // 触发后手动显式清除
                examplemod$KNOCKBACK_IMMUNES_CACHE.invalidate(uuid);
            }
        }
        // 阻止 Irons Spellbooks 原有的 cancelKnockback 逻辑继续执行，防止访问原版未清理的 HashMap
        ci.cancel();
    }
}