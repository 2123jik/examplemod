package com.example.examplemod.skill;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncSkillDataPayload(PlayerSkillData data) implements CustomPacketPayload {
    public static final Type<SyncSkillDataPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "sync_skill_data"));

    public static final StreamCodec<ByteBuf, SyncSkillDataPayload> STREAM_CODEC = StreamCodec.composite(
            PlayerSkillData.STREAM_CODEC,
            SyncSkillDataPayload::data,
            SyncSkillDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncSkillDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端收到后更新本地玩家数据（以便 UI 渲染）
            var player = context.player();
            player.setData(ModAttachments.SKILL_DATA.get(), payload.data());
        });
    }
}