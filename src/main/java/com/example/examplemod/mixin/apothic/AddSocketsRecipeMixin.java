package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.apothic.CurioSocketEventHandler;
import dev.shadowsoffire.apotheosis.socket.AddSocketsRecipe;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AddSocketsRecipe.class, remap = false)
public abstract class AddSocketsRecipeMixin {

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void disallowCurioSocketExpansion(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        // 锻造台输入中的基底装备（Slot 1）
        ItemStack base = input.getItem(1);

        // 如果基底是饰品且已经有至少 1 个孔，禁止匹配打孔配方
        if (CurioSocketEventHandler.isCurio(base) && SocketHelper.getSockets(base) >= 1) {
            cir.setReturnValue(false);
        }
    }
}