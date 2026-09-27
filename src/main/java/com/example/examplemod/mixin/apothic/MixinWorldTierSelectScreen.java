package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldTierSelectScreen.class)
public class MixinWorldTierSelectScreen {

    @Inject(
            method = "tierLocked",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void addLevelRequirements(
            WorldTier tier,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        List<Component> list = new ArrayList<>(cir.getReturnValue());

        int hostilityRequirement = switch (tier) {
            case HAVEN -> 0;
            case FRONTIER -> 30;
            case ASCENT -> 90;
            case SUMMIT -> 150;
            case PINNACLE -> 210;
        };

        int experienceRequirement = switch (tier) {
            case HAVEN -> 0;
            case FRONTIER -> 80;
            case ASCENT -> 160;
            case SUMMIT -> 240;
            case PINNACLE -> 320;
        };

        if (tier != WorldTier.HAVEN) {
            list.add(Component.empty());
            list.add(Component.literal(
                    "需要恶意等级: " + hostilityRequirement
                            + "，玩家等级: " + experienceRequirement
            ).withStyle(ChatFormatting.YELLOW));
        }

        cir.setReturnValue(list);
    }
}