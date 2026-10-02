package com.example.examplemod.mixin.villna;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void reduceTreeDensity(FeaturePlaceContext<TreeConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        // 关键判断：仅在自然生成区块（WorldGenRegion）时削减密度
        // 这样可以避免影响玩家在生存中骨粉催熟树苗、树苗自然生长的成功率
        if (context.level() instanceof WorldGenRegion) {
            // 25% 概率保留（即 3/4 概率直接取消），密度变为 1/4
            if (context.random().nextInt(9) != 0) {
                cir.setReturnValue(false);
            }
        }
    }
}