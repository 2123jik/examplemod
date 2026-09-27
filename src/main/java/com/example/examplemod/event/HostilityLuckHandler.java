package com.example.examplemod.event;

import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class HostilityLuckHandler {


    private static final ResourceLocation HOSTILITY_LUCK_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "hostility_luck");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {

            if (player.tickCount % 20 != 0) {
                return;
            }


            AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
            if (luckAttr == null) {
                return;
            }


            int hostilityLevel = DifficultyLevel.ofAny(player);
            double targetBonus = hostilityLevel * 0.1D; 


            AttributeModifier current = luckAttr.getModifier(HOSTILITY_LUCK_ID);

            if (current != null) {

                if (Math.abs(current.amount() - targetBonus) < 1.0E-4) {
                    return; 
                }
                luckAttr.removeModifier(HOSTILITY_LUCK_ID);
            }


            if (targetBonus > 0) {
                luckAttr.addTransientModifier(new AttributeModifier(
                        HOSTILITY_LUCK_ID,
                        targetBonus,
                        AttributeModifier.Operation.ADD_VALUE 
                ));
            }
        }
    }
}