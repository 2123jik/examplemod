package com.example.examplemod.skill;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpgradeSkillPayload(String nodeId) implements CustomPacketPayload {
    public static final Type<UpgradeSkillPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "upgrade_skill"));

    public static final StreamCodec<ByteBuf, UpgradeSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UpgradeSkillPayload::nodeId,
            UpgradeSkillPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UpgradeSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 传话完毕，具体能不能点由 SkillManager 说了算
                SkillManager.tryUpgradeSkill(player, payload.nodeId());
            }
        });
    }
}