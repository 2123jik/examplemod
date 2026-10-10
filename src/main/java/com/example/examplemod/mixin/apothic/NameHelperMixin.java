package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.util.NameHelper;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// 将 TargetClassName 替换为该方法所在的实际类名，例如 NameHelper.class
@Mixin(NameHelper.class)
public class NameHelperMixin {

    @ModifyArg(
        method = "setEntityName",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;setCustomNameVisible(Z)V"
        ),
        index = 0 // 修改第0个参数（即 boolean 值）
    )
    private static boolean modifyCustomNameVisible(boolean original) {
        // 将原本传入的 true 强制改为 false
        return false;
    }
}