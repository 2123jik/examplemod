package com.example.examplemod.mixin.vanilla;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("imageWidth") int enderinterface$getImageWidth();
    @Accessor("imageHeight") int enderinterface$getImageHeight();
    @Accessor("leftPos") int enderinterface$getLeftPos();
    @Accessor("topPos") int enderinterface$getTopPos();
}