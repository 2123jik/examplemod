package com.example.examplemod.mixin.villna;

import com.example.examplemod.util.FastWeaponReplacer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public abstract class MobMixin {

    /**
     * 此时：
     * 1. FinalizeSpawnEvent 已经执行完毕且没被取消
     * 2. populateDefaultEquipmentSlots() 已经执行，原版剑/斧已生成在主手
     * 3. 注入在 TAIL，瞬间完成 O(1) 连续内存覆写
     */
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void onFinalizeSpawnTail(ServerLevelAccessor level, DifficultyInstance difficulty,
                                     MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                     CallbackInfoReturnable<SpawnGroupData> cir) {
        Mob mob = (Mob) (Object) this;
        
        // 直接使用数组下标寻址替换主手武器
        FastWeaponReplacer.tryMutateSlot(mob, EquipmentSlot.MAINHAND);
        FastWeaponReplacer.tryMutateSlot(mob, EquipmentSlot.OFFHAND);
    }
}