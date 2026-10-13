package com.example.examplemod.event.defense;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundSyncDefensePayload(int entityId, boolean isDefending) implements CustomPacketPayload {
    public static final Type<ClientboundSyncDefensePayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "clientbound_sync_defense"));

    public static final StreamCodec<ByteBuf, ClientboundSyncDefensePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ClientboundSyncDefensePayload::entityId,
        ByteBufCodecs.BOOL, ClientboundSyncDefensePayload::isDefending,
        ClientboundSyncDefensePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundSyncDefensePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientDefenseManager.setDefending(payload.entityId(), payload.isDefending());
        });
    }
}