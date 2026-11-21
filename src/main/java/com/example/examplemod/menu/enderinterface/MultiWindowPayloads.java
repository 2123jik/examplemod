package com.example.examplemod.menu.enderinterface;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Optional;

public final class MultiWindowPayloads {
    private MultiWindowPayloads() {}

    public static final int PREVIEW_NONE = 0;
    public static final int PREVIEW_ITEMS = 1;
    public static final int PREVIEW_TRADES = 2;

    public record ItemPreviewEntry(ItemStack stack, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemPreviewEntry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, ItemPreviewEntry::stack,
                ByteBufCodecs.VAR_INT, ItemPreviewEntry::count,
                ItemPreviewEntry::new
        );
    }

    public record TradeOfferPreview(ItemStack costA, ItemStack costB, ItemStack result, boolean outOfStock) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeOfferPreview> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, TradeOfferPreview::costA,
                ItemStack.OPTIONAL_STREAM_CODEC, TradeOfferPreview::costB,
                ItemStack.OPTIONAL_STREAM_CODEC, TradeOfferPreview::result,
                ByteBufCodecs.BOOL, TradeOfferPreview::outOfStock,
                TradeOfferPreview::new
        );
    }

    public record PreviewData(
            int kind,
            int totalSlots,
            int occupiedSlots,
            List<ItemPreviewEntry> items,
            List<TradeOfferPreview> trades
    ) {
        public static final PreviewData EMPTY = new PreviewData(PREVIEW_NONE, 0, 0, List.of(), List.of());

        public static final StreamCodec<RegistryFriendlyByteBuf, PreviewData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PreviewData::kind,
                ByteBufCodecs.VAR_INT, PreviewData::totalSlots,
                ByteBufCodecs.VAR_INT, PreviewData::occupiedSlots,
                ItemPreviewEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), PreviewData::items,
                TradeOfferPreview.STREAM_CODEC.apply(ByteBufCodecs.list()), PreviewData::trades,
                PreviewData::new
        );
    }

    // S2C: 通知客户端接下来收到的 OpenScreen 是远程接口窗口
    public record PrepareRemotePayload(
            EnderInterface.RemoteData data,
            Optional<RemoteBlockSnapshot> snapshot
    ) implements CustomPacketPayload {
        public static final Type<PrepareRemotePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "prepare_remote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PrepareRemotePayload> STREAM_CODEC = StreamCodec.composite(
                EnderInterface.RemoteData.STREAM_CODEC, PrepareRemotePayload::data,
                ByteBufCodecs.optional(RemoteBlockSnapshot.STREAM_CODEC), PrepareRemotePayload::snapshot,
                PrepareRemotePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: 如果 openMenu 返回 empty，清除客户端的 pending
    public record ClearPendingPayload() implements CustomPacketPayload {
        public static final Type<ClearPendingPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "clear_pending_remote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearPendingPayload> STREAM_CODEC =
                StreamCodec.unit(new ClearPendingPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: 重复点同一个已开接口时，聚焦已有窗口
    public record FocusRemoteClientPayload(int containerId) implements CustomPacketPayload {
        public static final Type<FocusRemoteClientPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "focus_remote_client"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FocusRemoteClientPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, FocusRemoteClientPayload::containerId, FocusRemoteClientPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 切换当前激活窗口
    public record FocusWindowPayload(int containerId) implements CustomPacketPayload {
        public static final Type<FocusWindowPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "focus_window"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FocusWindowPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, FocusWindowPayload::containerId, FocusWindowPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 关闭单个窗口 (×按钮)
    public record CloseWindowPayload(int containerId) implements CustomPacketPayload {
        public static final Type<CloseWindowPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "close_window"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseWindowPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, CloseWindowPayload::containerId, CloseWindowPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 按 E 最小化
    public record MinimizeDesktopPayload() implements CustomPacketPayload {
        public static final Type<MinimizeDesktopPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "minimize_desktop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MinimizeDesktopPayload> STREAM_CODEC =
                StreamCodec.unit(new MinimizeDesktopPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: 某个已打开的远程窗口内部跳转新界面（如精妙存储点击设置），原地替换它
    public record ReplaceRemotePayload(
            int oldContainerId,
            EnderInterface.RemoteData data,
            Optional<RemoteBlockSnapshot> snapshot
    ) implements CustomPacketPayload {
        public static final Type<ReplaceRemotePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "replace_remote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReplaceRemotePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ReplaceRemotePayload::oldContainerId,
                EnderInterface.RemoteData.STREAM_CODEC, ReplaceRemotePayload::data,
                ByteBufCodecs.optional(RemoteBlockSnapshot.STREAM_CODEC), ReplaceRemotePayload::snapshot,
                ReplaceRemotePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 按 ESC 关闭全部远程窗口
    public record CloseAllWindowsPayload() implements CustomPacketPayload {
        public static final Type<CloseAllWindowsPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "close_all_windows"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseAllWindowsPayload> STREAM_CODEC =
                StreamCodec.unit(new CloseAllWindowsPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 鼠标悬停请求预览右键内容
    public record RequestPreviewPayload(EnderInterface.RemoteData data) implements CustomPacketPayload {
        public static final Type<RequestPreviewPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "request_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPreviewPayload> STREAM_CODEC =
                EnderInterface.RemoteData.STREAM_CODEC.map(RequestPreviewPayload::new, RequestPreviewPayload::data);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: 返回右键内容预览
    public record PreviewResponsePayload(String targetKey, PreviewData preview) implements CustomPacketPayload {
        public static final Type<PreviewResponsePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "preview_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PreviewResponsePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PreviewResponsePayload::targetKey,
                PreviewData.STREAM_CODEC, PreviewResponsePayload::preview,
                PreviewResponsePayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: 手持 Shift+左键切换自动归类模式
    public record CyclePickupModePayload() implements CustomPacketPayload {
        public static final Type<CyclePickupModePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "cycle_pickup_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CyclePickupModePayload> STREAM_CODEC =
                StreamCodec.unit(new CyclePickupModePayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(PrepareRemotePayload.TYPE, PrepareRemotePayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> MultiWindowClient.setPendingRemote(p.data(), p.snapshot().orElse(null))));
        registrar.playToClient(ClearPendingPayload.TYPE, ClearPendingPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(MultiWindowClient::clearPendingRemote));
        registrar.playToClient(FocusRemoteClientPayload.TYPE, FocusRemoteClientPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> MultiWindowClient.restoreAndFocus(p.containerId())));

        registrar.playToServer(FocusWindowPayload.TYPE, FocusWindowPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) MultiMenuManager.focus(sp, p.containerId());
                }));
        registrar.playToServer(CloseWindowPayload.TYPE, CloseWindowPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) MultiMenuManager.closeOne(sp, p.containerId());
                }));
        registrar.playToServer(MinimizeDesktopPayload.TYPE, MinimizeDesktopPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) MultiMenuManager.parkActive(sp);
                }));
        registrar.playToServer(CloseAllWindowsPayload.TYPE, CloseAllWindowsPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) MultiMenuManager.closeAll(sp);
                }));
        registrar.playToClient(ReplaceRemotePayload.TYPE, ReplaceRemotePayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> MultiWindowClient.setPendingReplace(p.oldContainerId(), p.data(), p.snapshot().orElse(null))));

        registrar.playToServer(RequestPreviewPayload.TYPE, RequestPreviewPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        EnderInterface.handlePreviewRequest(sp, p.data());
                    }
                }));
        registrar.playToClient(PreviewResponsePayload.TYPE, PreviewResponsePayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> EnderInterfaceClient.receivePreviewResponse(p.targetKey(), p.preview())));

        registrar.playToServer(CyclePickupModePayload.TYPE, CyclePickupModePayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        EnderInterface.handleCyclePickupMode(sp);
                    }
                }));
    }
}