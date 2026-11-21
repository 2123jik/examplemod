package com.example.examplemod.tactical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = "examplemod", bus = EventBusSubscriber.Bus.MOD)
public class TacticalNet {

    public record UnitView(UUID uuid, int entityId, String name, BlockPos pos,
                           float hp, float maxHp, int team, int moveRange, int ap, boolean active) {}

    public record S2CSyncBoard(boolean open, BlockPos center, int radius,
                               UUID currentTurnUnit, List<UnitView> units) implements CustomPacketPayload {
        public static final Type<S2CSyncBoard> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "sync_board"));
        public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncBoard> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.open);
                    buf.writeBlockPos(p.center);
                    buf.writeVarInt(p.radius);
                    buf.writeBoolean(p.currentTurnUnit != null);
                    if (p.currentTurnUnit != null) buf.writeUUID(p.currentTurnUnit);
                    buf.writeVarInt(p.units.size());
                    for (UnitView u : p.units) {
                        buf.writeUUID(u.uuid);
                        buf.writeVarInt(u.entityId);
                        buf.writeUtf(u.name);
                        buf.writeBlockPos(u.pos);
                        buf.writeFloat(u.hp);
                        buf.writeFloat(u.maxHp);
                        buf.writeVarInt(u.team);
                        buf.writeVarInt(u.moveRange);
                        buf.writeVarInt(u.ap);
                        buf.writeBoolean(u.active);
                    }
                },
                buf -> {
                    boolean open = buf.readBoolean();
                    BlockPos center = buf.readBlockPos();
                    int radius = buf.readVarInt();
                    UUID turn = buf.readBoolean() ? buf.readUUID() : null;
                    int size = buf.readVarInt();
                    List<UnitView> units = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        units.add(new UnitView(
                                buf.readUUID(), buf.readVarInt(), buf.readUtf(), buf.readBlockPos(),
                                buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(),
                                buf.readVarInt(), buf.readBoolean()
                        ));
                    }
                    return new S2CSyncBoard(open, center, radius, turn, units);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2SAction(int op, UUID unitId, BlockPos targetPos, int targetEntityId) implements CustomPacketPayload {
        public static final Type<C2SAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "tactical_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, C2SAction> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.op);
                    buf.writeBoolean(p.unitId != null);
                    if (p.unitId != null) buf.writeUUID(p.unitId);
                    buf.writeBlockPos(p.targetPos);
                    buf.writeVarInt(p.targetEntityId);
                },
                buf -> new C2SAction(buf.readVarInt(), buf.readBoolean() ? buf.readUUID() : null, buf.readBlockPos(), buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(S2CSyncBoard.TYPE, S2CSyncBoard.STREAM_CODEC,
                (payload, ctx) -> TacticalClient.handleSync(payload));
        registrar.playToServer(C2SAction.TYPE, C2SAction.STREAM_CODEC,
                (payload, ctx) -> TacticalServer.handleAction(payload, ctx));
    }
}