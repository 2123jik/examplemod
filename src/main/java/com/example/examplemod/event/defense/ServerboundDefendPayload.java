package com.example.examplemod.event.defense;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundDefendPayload(boolean isDefending) implements CustomPacketPayload {
    public static final Type<ServerboundDefendPayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "serverbound_defend"));

    public static final StreamCodec<ByteBuf, ServerboundDefendPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ServerboundDefendPayload::isDefending,
        ServerboundDefendPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundDefendPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerGrabManager.GrabData data = PlayerGrabManager.getGrabData(player);
                if (data != null && data.isDefending != payload.isDefending()) {
                    data.isDefending = payload.isDefending();
                    // 向周围玩家广播该海龟的自转状态
                    PacketDistributor.sendToPlayersTrackingEntity(
                        data.entity,
                        new ClientboundSyncDefensePayload(data.entity.getId(), data.isDefending)
                    );
                }
            }
        });
    }
}