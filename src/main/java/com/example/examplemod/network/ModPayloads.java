package com.example.examplemod.network;

import com.example.examplemod.deathrecap.client.ClientPayloadHandler;
import com.example.examplemod.client.XpFlightClient;
import com.example.examplemod.deathrecap.DeathRecapPayload;
import com.example.examplemod.skill.SyncSkillDataPayload;
import com.example.examplemod.skill.UpgradeSkillPayload;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // S2C (服务端到客户端)
        registrar.playToClient(DeathRecapPayload.TYPE, DeathRecapPayload.STREAM_CODEC, ClientPayloadHandler::handleDeathRecap);
        registrar.playToClient(XpFlightPayload.TYPE, XpFlightPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> XpFlightClient.receive(payload)));
        registrar.playToClient(SyncSkillDataPayload.TYPE, SyncSkillDataPayload.STREAM_CODEC, SyncSkillDataPayload::handle);

        // C2S (客户端到服务端)
        registrar.playToServer(OpenShopSelectorPayload.TYPE, OpenShopSelectorPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) ViScriptShopServerUtil.serverOpenShopSelector(sp);
                }));
        registrar.playToServer(RequestEmiCraftingPayload.TYPE, RequestEmiCraftingPayload.STREAM_CODEC, RequestEmiCraftingPayload::handle);
        registrar.playToServer(AutoUpgradeArtifactsPayload.TYPE, AutoUpgradeArtifactsPayload.STREAM_CODEC, AutoUpgradeArtifactsPayload::handle);
        registrar.playToServer(UpgradeSkillPayload.TYPE, UpgradeSkillPayload.STREAM_CODEC, UpgradeSkillPayload::handle);
        registrar.playToServer(SortToBackpackPayload.TYPE, SortToBackpackPayload.STREAM_CODEC, SortToBackpackPayload::handle);
        registrar.playToServer(SwapEquipmentPayload.TYPE, SwapEquipmentPayload.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(SleepButtonPayload.TYPE, SleepButtonPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) handleSleepButton(player);
                }));
    }

    private static void handleSleepButton(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!level.dimensionType().bedWorks()) {
            player.sendSystemMessage(Component.translatable("examplemod.message.bed_explodes"));
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay < 12542L || timeOfDay >= 23460L) {
            player.sendSystemMessage(Component.translatable("examplemod.message.cannot_sleep_now"));
            return;
        }

        BlockPos respawnPos = player.blockPosition();
        player.setRespawnPosition(level.dimension(), respawnPos, player.getYRot(), false, true);

        long nextMorning = ((level.getDayTime() / 24000L) + 1L) * 24000L + 1000L;
        level.setDayTime(nextMorning);
        if (level.isRaining()) level.resetWeatherCycle();
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));

        player.closeContainer();
        player.sendSystemMessage(Component.translatable("examplemod.message.woke_up"));
    }
}