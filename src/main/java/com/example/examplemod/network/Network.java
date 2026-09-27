package com.example.examplemod.network;

import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public final class Network {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                OpenShopSelectorPayload.TYPE,
                OpenShopSelectorPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        ViScriptShopServerUtil.serverOpenShopSelector(serverPlayer);
                    }
                }
        );

        registrar.playToServer(
                SleepButtonPayload.TYPE,
                SleepButtonPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handleSleepButton(player);
                    }
                }
        );

    }
    private static void handleSleepButton(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        
        if (!level.dimensionType().bedWorks()) {
            player.sendSystemMessage(
                    Component.translatable(
                            "examplemod.message.bed_explodes"
                    )
            );
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;

        
        if (timeOfDay < 12542L || timeOfDay >= 23460L) {
            player.sendSystemMessage(
                    Component.translatable(
                            "examplemod.message.cannot_sleep_now"
                    )
            );
            return;
        }

        BlockPos respawnPos = player.blockPosition();

        
        player.setRespawnPosition(
                level.dimension(),
                respawnPos,
                player.getYRot(),
                false,
                true
        );

        
        long nextMorning =
                ((level.getDayTime() / 24000L) + 1L) * 24000L + 1000L;

        level.setDayTime(nextMorning);

        
        player.closeContainer();

        player.sendSystemMessage(
                Component.translatable(
                        "examplemod.message.woke_up"
                )
        );
    }
}