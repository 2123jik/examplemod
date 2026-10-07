package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record XpFlightPayload(
        int orbId,
        int playerId,
        double x,
        double y,
        double z,
        long startTick,
        int duration
) implements CustomPacketPayload {

    public static final Type<XpFlightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MOD_ID, "xp_flight"
            )
    );

    public static final StreamCodec<FriendlyByteBuf, XpFlightPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public XpFlightPayload decode(FriendlyByteBuf buffer) {
                    return new XpFlightPayload(
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readLong(),
                            buffer.readVarInt()
                    );
                }

                @Override
                public void encode(
                        FriendlyByteBuf buffer,
                        XpFlightPayload payload
                ) {
                    buffer.writeVarInt(payload.orbId());
                    buffer.writeVarInt(payload.playerId());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeLong(payload.startTick());
                    buffer.writeVarInt(payload.duration());
                }
            };

    public static XpFlightPayload stop(int orbId) {
        return new XpFlightPayload(orbId, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public Type<XpFlightPayload> type() {
        return TYPE;
    }
}
