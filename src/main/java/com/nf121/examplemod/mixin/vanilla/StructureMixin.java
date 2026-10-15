package com.nf121.examplemod.mixin.vanilla;

import com.nf121.examplemod.content.CustomStructureBiomes;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public class StructureMixin implements CustomStructureBiomes {
    @Unique
    private HolderSet<Biome> examplemod$customBiomes = null;

    @Override
    public void examplemod$setCustomBiomes(HolderSet<Biome> biomes) {
        this.examplemod$customBiomes = biomes;
    }

    @Inject(method = "biomes", at = @At("HEAD"), cancellable = true)
    private void examplemod$overrideBiomes(CallbackInfoReturnable<HolderSet<Biome>> cir) {
        if (this.examplemod$customBiomes != null) {
            cir.setReturnValue(this.examplemod$customBiomes);
        }
    }
}