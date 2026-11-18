package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Random;


@EventBusSubscriber(modid = ExampleMod.MODID)
public class AnimalKillRewardHandler {



    private static final List<Holder<MobEffect>> REWARD_EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED,    
            MobEffects.DAMAGE_BOOST,       
            MobEffects.DAMAGE_RESISTANCE,  
            MobEffects.ABSORPTION,         
            MobEffects.DIG_SPEED           
    );

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();


        if (victim.level().isClientSide()) {
            return;
        }



        boolean isTargetAnimal = victim instanceof Pig 
                              || victim instanceof Cow 
                              || victim instanceof Sheep 
                              || victim instanceof Chicken;




        if (isTargetAnimal) {

            if (event.getSource().getEntity() instanceof Player player) {

                Holder<MobEffect> chosenEffect = REWARD_EFFECTS.get(RANDOM.nextInt(REWARD_EFFECTS.size()));




                int duration = 20 * 20; 
                int amplifier = 0;


                player.addEffect(new MobEffectInstance(chosenEffect, duration, amplifier));
            }
        }
    }
}