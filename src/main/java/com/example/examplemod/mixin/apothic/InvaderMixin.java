package com.example.examplemod.mixin.apothic;


import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Invader.class, remap = false)
public abstract class InvaderMixin {

    @Inject(method = "initBoss", at = @At("RETURN"))
    private void hideInvaderName(
        Mob mob,
        GenContext ctx,
        @Nullable LootRarity rarity,
        CallbackInfo ci
    ) {
        mob.setCustomNameVisible(false);
    }
}
