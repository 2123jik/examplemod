package com.example.examplemod.network;

import com.example.examplemod.util.ArtifactAutoUpgradeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AutoUpgradeArtifactsPayload() implements CustomPacketPayload {
    public static final Type<AutoUpgradeArtifactsPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "auto_upgrade_artifacts"));

    public static final StreamCodec<FriendlyByteBuf, AutoUpgradeArtifactsPayload> STREAM_CODEC = 
            StreamCodec.unit(new AutoUpgradeArtifactsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoUpgradeArtifactsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ArtifactAutoUpgradeHandler.processAutoUpgrade(player);
            }
        });
    }
}