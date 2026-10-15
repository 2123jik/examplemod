package com.nf121.examplemod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "examplemod")
public class PlayerDeathExperienceHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

        int newLevel = Math.max(0, oldPlayer.experienceLevel - 10);

        newPlayer.experienceProgress = 0.0F;
        newPlayer.experienceLevel = newLevel;
        
        newPlayer.totalExperience = calculateTotalXpForLevel(newLevel);
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCanceled(true);         }
    }

    private static int calculateTotalXpForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
}