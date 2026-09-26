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

        /*
         * 与原版床相同：
         * 下界、末地等 bed_works=false 的维度不能使用床。
         */
        if (!level.dimensionType().bedWorks()) {
            player.sendSystemMessage(
                    Component.translatable(
                            "examplemod.message.bed_explodes"
                    )
            );
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;

        /*
         * 原版床只能在夜间使用。
         * 12542：夜晚开始
         * 23460：夜晚结束
         */
        if (timeOfDay < 12542L || timeOfDay >= 23460L) {
            player.sendSystemMessage(
                    Component.translatable(
                            "examplemod.message.cannot_sleep_now"
                    )
            );
            return;
        }

        BlockPos respawnPos = player.blockPosition();

        /*
         * 设置玩家重生点。
         *
         * false：不是强制重生点
         * true：向玩家发送重生点设置提示
         */
        player.setRespawnPosition(
                level.dimension(),
                respawnPos,
                player.getYRot(),
                false,
                true
        );

        /*
         * 推进到第二天早晨。
         *
         * 24000：下一天的开始
         * 1000：原版睡醒后的清晨时间
         */
        long nextMorning =
                ((level.getDayTime() / 24000L) + 1L) * 24000L + 1000L;

        level.setDayTime(nextMorning);

        /*
         * 关闭背包界面，模拟上床后关闭当前界面。
         */
        player.closeContainer();

        player.sendSystemMessage(
                Component.translatable(
                        "examplemod.message.woke_up"
                )
        );
    }
}