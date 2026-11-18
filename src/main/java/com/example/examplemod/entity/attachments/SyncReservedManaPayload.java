package com.example.examplemod.entity.attachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = "examplemod")
public record SyncReservedManaPayload(float reservedMana) implements CustomPacketPayload {

    public static final Type<SyncReservedManaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "sync_reserved_mana"));

    public static final StreamCodec<ByteBuf, SyncReservedManaPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncReservedManaPayload::reservedMana,
            SyncReservedManaPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncReservedManaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAuraData.setReservedMana(payload.reservedMana()));
    }

    // 直接在这里监听，彻底告别 ExampleMod 构造函数
    @SubscribeEvent
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                TYPE,
                STREAM_CODEC,
                SyncReservedManaPayload::handleClient
        );
    }
}