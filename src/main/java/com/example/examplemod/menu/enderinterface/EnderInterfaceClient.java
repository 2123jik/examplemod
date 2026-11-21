package com.example.examplemod.menu.enderinterface;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnderInterfaceClient {
    private static BlockPos pendingRemotePos;
    private static long pendingRemoteAtMs;
    private static final Map<BlockPos, EnderInterface.RemoteData> REMOTE_ICON_CACHE = new ConcurrentHashMap<>();

    private record EntityCacheKey(ResourceLocation typeId, CompoundTag tag) {}
    private static final Map<EntityCacheKey, Entity> ENTITY_PREVIEW_CACHE =
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<EntityCacheKey, Entity> eldest) {
                    return size() > 128;
                }
            };
    private static ClientLevel cachedForLevel;

    private static final class CachedPreview {
        @Nullable MultiWindowPayloads.PreviewData data;
        long fetchedAtMs;
        long requestedAtMs;
    }

    private static final Map<String, CachedPreview> TOOLTIP_PREVIEW_CACHE = new ConcurrentHashMap<>();

    public static void receivePreviewResponse(String targetKey, MultiWindowPayloads.PreviewData preview) {
        CachedPreview entry = TOOLTIP_PREVIEW_CACHE.computeIfAbsent(targetKey, k -> new CachedPreview());
        entry.data = preview;
        entry.fetchedAtMs = System.currentTimeMillis();
    }

    @Nullable
    private static MultiWindowPayloads.PreviewData requestOrGetPreview(EnderInterface.RemoteData data) {
        String key = data.targetKey();
        long now = System.currentTimeMillis();
        CachedPreview entry = TOOLTIP_PREVIEW_CACHE.computeIfAbsent(key, k -> new CachedPreview());
        boolean expired = (entry.data == null) || (now - entry.fetchedAtMs > 1000L);
        if (expired && now - entry.requestedAtMs > 300L) {
            entry.requestedAtMs = now;
            PacketDistributor.sendToServer(new MultiWindowPayloads.RequestPreviewPayload(data));
        }
        return entry.data;
    }

    public static void clearEntityPreviewCache() {
        ENTITY_PREVIEW_CACHE.clear();
        TOOLTIP_PREVIEW_CACHE.clear();
        cachedForLevel = null;
    }

    @Nullable
    private static Entity getOrCreatePreviewEntity(EnderInterface.EntitySnapshot snap) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (cachedForLevel != level) {
            ENTITY_PREVIEW_CACHE.clear();
            cachedForLevel = level;
        }
        EntityCacheKey key = new EntityCacheKey(snap.entityTypeId(), snap.visualTag());
        if (ENTITY_PREVIEW_CACHE.containsKey(key)) {
            return ENTITY_PREVIEW_CACHE.get(key);
        }
        Entity created = BuiltInRegistries.ENTITY_TYPE
                .getOptional(snap.entityTypeId())
                .map(t -> t.create(level))
                .orElse(null);
        if (created != null) {
            if (!snap.visualTag().isEmpty()) {
                try {
                    created.load(snap.visualTag().copy());
                } catch (Throwable ignored) {}
            }
            created.setCustomName(null);
            created.setCustomNameVisible(false);
        }
        ENTITY_PREVIEW_CACHE.put(key, created);
        return created;
    }

    public static void markPendingRemote(EnderInterface.RemoteData data) {
        BlockPos pos = data.pos().immutable();
        pendingRemotePos = pos;
        pendingRemoteAtMs = System.currentTimeMillis();
        REMOTE_ICON_CACHE.put(pos, data.withPos(pos));
    }

    @Nullable
    public static BlockPos consumePendingRemotePos() {
        BlockPos pos = pendingRemotePos;
        long t = pendingRemoteAtMs;
        pendingRemotePos = null;
        pendingRemoteAtMs = 0L;
        if (pos != null && System.currentTimeMillis() - t < 1500L) {
            return pos;
        }
        return null;
    }

    @Nullable
    public static EnderInterface.RemoteData getRemoteIconData(BlockPos pos) {
        return REMOTE_ICON_CACHE.get(pos);
    }

    @EventBusSubscriber(modid = EnderInterface.MODID, value = Dist.CLIENT)
    public static class GameEvents {
        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().isClientSide() && !event.getEntity().isShiftKeyDown()) {
                ItemStack stack = event.getItemStack();
                if (stack.is(EnderInterface.INTERFACE_ITEM.get())) {
                    EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
                    if (data != null) {
                        markPendingRemote(data);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            Player player = event.getEntity();
            if (!player.isShiftKeyDown()) return;
            ItemStack stack = event.getItemStack();
            if (stack.is(EnderInterface.INTERFACE_ITEM.get()) && stack.has(EnderInterface.REMOTE_DATA.get())) {
                PacketDistributor.sendToServer(new MultiWindowPayloads.CyclePickupModePayload());
            }
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getLevel().isClientSide()) return;
            Player player = event.getEntity();
            if (!player.isShiftKeyDown()) return;
            ItemStack stack = event.getItemStack();
            if (stack.is(EnderInterface.INTERFACE_ITEM.get()) && stack.has(EnderInterface.REMOTE_DATA.get())) {
                event.setCanceled(true);
                if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
                    PacketDistributor.sendToServer(new MultiWindowPayloads.CyclePickupModePayload());
                }
            }
        }

        @SubscribeEvent
        public static void onScreenRightClick(ScreenEvent.MouseButtonPressed.Pre event) {
            if (event.getButton() != 1) return;

            double mx = event.getMouseX();
            double my = event.getMouseY();

            if (event.getScreen() instanceof MultiWindowScreen mws) {
                Slot sharedSlot = mws.getHoveredSharedSlot(mx, my);
                if (sharedSlot != null) {
                    var activeMenu = mws.getSharedInventoryMenu();
                    if (activeMenu != null && activeMenu.getCarried().isEmpty()) {
                        ItemStack slotStack = sharedSlot.getItem();
                        if (slotStack.is(EnderInterface.INTERFACE_ITEM.get())
                                && slotStack.has(EnderInterface.REMOTE_DATA.get())) {
                            event.setCanceled(true);
                            PacketDistributor.sendToServer(new EnderInterface.OpenPacket(sharedSlot.index, slotStack));
                            return;
                        }
                        if (L2MenuStackerCompat.Client.canHandleSlotRightClick(sharedSlot, activeMenu.containerId)) {
                            mws.focusSharedTargetWindow();
                            if (L2MenuStackerCompat.Client.tryHandleSlotRightClick(sharedSlot, activeMenu.containerId)) {
                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                    return;
                }

                MultiWindowClient.WindowEntry hit = mws.getTopWindowAt(mx, my);
                if (hit != null) {
                    Slot hoveredSlot = hit.screen.getSlotUnderMouse();
                    if (hoveredSlot != null && hit.screen.getMenu().getCarried().isEmpty()) {
                        ItemStack slotStack = hoveredSlot.getItem();
                        if (slotStack.is(EnderInterface.INTERFACE_ITEM.get())
                                && slotStack.has(EnderInterface.REMOTE_DATA.get())) {
                            event.setCanceled(true);
                            PacketDistributor.sendToServer(new EnderInterface.OpenPacket(hoveredSlot.index, slotStack));
                            return;
                        }
                        if (L2MenuStackerCompat.Client.canHandleSlotRightClick(hoveredSlot, hit.containerId())) {
                            mws.bringWindowToFront(hit);
                            if (L2MenuStackerCompat.Client.tryHandleSlotRightClick(hoveredSlot, hit.containerId())) {
                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                }
                return;
            }

            if (event.getScreen() instanceof AbstractContainerScreen<?> targetScreen) {
                Slot hoveredSlot = targetScreen.getSlotUnderMouse();
                if (hoveredSlot != null && targetScreen.getMenu().getCarried().isEmpty()) {
                    ItemStack slotStack = hoveredSlot.getItem();
                    if (slotStack.is(EnderInterface.INTERFACE_ITEM.get())
                            && slotStack.has(EnderInterface.REMOTE_DATA.get())) {
                        event.setCanceled(true);
                        PacketDistributor.sendToServer(new EnderInterface.OpenPacket(hoveredSlot.index, slotStack));
                    }
                }
            }
        }
        @SubscribeEvent
        public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
            ItemStack stack = event.getItemStack();
            if (!stack.is(EnderInterface.INTERFACE_ITEM.get())) return;
            EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
            if (data == null) return;

            MultiWindowPayloads.PreviewData preview = requestOrGetPreview(data);
            if (preview != null && preview.kind() != MultiWindowPayloads.PREVIEW_NONE) {
                event.getTooltipElements().add(Either.right(new EnderInterface.RemotePreviewTooltip(preview)));
            }
        }

        @SubscribeEvent
        public static void onClientLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            MultiWindowClient.clearOnDisconnect();
            clearEntityPreviewCache();
        }
    }

    @EventBusSubscriber(modid = EnderInterface.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(EnderInterface.RemotePreviewTooltip.class, InterfacePreviewClientTooltip::new);
        }

        @SubscribeEvent
        public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> {
                EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
                if (data != null && data.entitySnapshot().isEmpty()
                        && !data.iconStack().isEmpty() && !data.iconStack().is(Items.ENDER_EYE)) {
                    return event.getItemColors().getColor(data.iconStack(), tintIndex);
                }
                return -1;
            }, EnderInterface.INTERFACE_ITEM.get());
        }

        @SubscribeEvent
        public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (this.renderer == null) {
                        Minecraft mc = Minecraft.getInstance();
                        this.renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                            @Override
                            public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack,
                                                     MultiBufferSource buffer, int light, int overlay) {
                                EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
                                if (data != null && data.entitySnapshot().isPresent()) {
                                    if (renderEntityIcon(data.entitySnapshot().get(), ctx, poseStack, buffer, light)) {
                                        return;
                                    }
                                }
                                if (data == null || data.iconStack().isEmpty() || data.iconStack().is(Items.ENDER_EYE)) {
                                    return;
                                }
                                ItemStack proxy = data.iconStack();
                                IClientItemExtensions.of(proxy).getCustomRenderer()
                                        .renderByItem(proxy, ctx, poseStack, buffer, light, overlay);
                            }
                        };
                    }
                    return this.renderer;
                }
            }, EnderInterface.INTERFACE_ITEM.get());
        }

        private static boolean renderEntityIcon(EnderInterface.EntitySnapshot snap, ItemDisplayContext ctx,
                                                PoseStack poseStack, MultiBufferSource buffer, int light) {
            Entity entity = getOrCreatePreviewEntity(snap);
            if (entity == null) return false;

            float bbW = Math.max(0.4F, entity.getBbWidth());
            float bbH = Math.max(0.4F, entity.getBbHeight());
            float scale = 0.88F / Math.max(bbW, bbH);
            int renderLight = (ctx == ItemDisplayContext.GUI) ? LightTexture.FULL_BRIGHT : light;

            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F - (bbH * scale) * 0.5F, 0.5F);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));

            entity.setYRot(0.0F);
            entity.setXRot(0.0F);
            entity.yRotO = 0.0F;
            entity.xRotO = 0.0F;
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = 0.0F;
                living.yBodyRotO = 0.0F;
                living.yHeadRot = 0.0F;
                living.yHeadRotO = 0.0F;
            }

            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            boolean oldHitBoxes = dispatcher.shouldRenderHitBoxes();
            dispatcher.setRenderHitBoxes(false);
            dispatcher.setRenderShadow(false);
            if (ctx == ItemDisplayContext.GUI) {
                Lighting.setupFor3DItems();
            }
            try {
                RenderSystem.runAsFancy(() ->
                        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, buffer, renderLight)
                );
                if (ctx == ItemDisplayContext.GUI && buffer instanceof MultiBufferSource.BufferSource bs) {
                    bs.endBatch();
                }
                return true;
            } catch (Throwable t) {
                return false;
            } finally {
                dispatcher.setRenderShadow(true);
                dispatcher.setRenderHitBoxes(oldHitBoxes);
                poseStack.popPose();
            }
        }

        @SubscribeEvent
        public static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
            ModelResourceLocation myModelLoc = ModelResourceLocation.inventory(
                    BuiltInRegistries.ITEM.getKey(Items.ENDER_EYE)
            );

            event.getModels().computeIfPresent(myModelLoc, (loc, originalModel) -> new BakedModelWrapper<>(originalModel) {
                private final BakedModel entityCustomModel = new BakedModelWrapper<>(originalModel) {
                    @Override public boolean isCustomRenderer() { return true; }
                    @Override public boolean isGui3d() { return true; }
                    @Override public boolean usesBlockLight() { return true; }

                    @Override
                    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
                        originalModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
                        return this;
                    }
                };

                private final ItemOverrides overrides = new ItemOverrides() {
                    @Override
                    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                        EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
                        if (data == null) {
                            return originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                        }
                        if (data.entitySnapshot().isPresent()) {
                            return entityCustomModel;
                        }
                        ItemStack proxy = data.iconStack();
                        if (proxy.isEmpty() || proxy.is(Items.ENDER_EYE)) {
                            return originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                        }
                        BakedModel resolved = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(proxy);
                        return resolved.getOverrides().resolve(resolved, proxy, level, entity, seed);
                    }
                };

                @Override
                public ItemOverrides getOverrides() {
                    return this.overrides;
                }
            });
        }
    }

    public static class InterfacePreviewClientTooltip implements ClientTooltipComponent {
        private final MultiWindowPayloads.PreviewData preview;

        public InterfacePreviewClientTooltip(EnderInterface.RemotePreviewTooltip component) {
            this.preview = component.preview();
        }

        @Override
        public int getHeight() {
            if (this.preview.kind() == MultiWindowPayloads.PREVIEW_ITEMS) {
                List<MultiWindowPayloads.ItemPreviewEntry> items = this.preview.items();
                if (items.isEmpty()) return 16;
                int rows = (items.size() + 8) / 9;
                boolean truncated = this.preview.occupiedSlots() > items.size();
                return 14 + rows * 18 + (truncated ? 12 : 0) + 4;
            }
            if (this.preview.kind() == MultiWindowPayloads.PREVIEW_TRADES) {
                int count = this.preview.trades().size();
                if (count == 0) return 16;
                return 14 + count * 20 + 4;
            }
            return 0;
        }

        @Override
        public int getWidth(Font font) {
            if (this.preview.kind() == MultiWindowPayloads.PREVIEW_ITEMS) {
                List<MultiWindowPayloads.ItemPreviewEntry> items = this.preview.items();
                String header = items.isEmpty()
                        ? "容器为空 (0/" + this.preview.totalSlots() + ")"
                        : "内容预览 (" + this.preview.occupiedSlots() + "/" + this.preview.totalSlots() + ")";
                int gridW = items.isEmpty() ? 0 : Math.min(9, items.size()) * 18 + 4;
                return Math.max(font.width(header) + 6, gridW);
            }
            if (this.preview.kind() == MultiWindowPayloads.PREVIEW_TRADES) {
                List<MultiWindowPayloads.TradeOfferPreview> trades = this.preview.trades();
                int maxW = font.width("交易列表 (" + this.preview.totalSlots() + ")") + 6;
                for (MultiWindowPayloads.TradeOfferPreview t : trades) {
                    int rowW = (t.costB().isEmpty() ? 58 : 82) + Math.min(96, font.width(tradeResultLabel(t.result()))) + 8;
                    if (rowW > maxW) maxW = rowW;
                }
                return maxW;
            }
            return 0;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics gg) {
            if (this.preview.kind() == MultiWindowPayloads.PREVIEW_ITEMS) {
                renderItemsPreview(font, x, y, gg);
            } else if (this.preview.kind() == MultiWindowPayloads.PREVIEW_TRADES) {
                renderTradesPreview(font, x, y, gg);
            }
        }

        private void renderItemsPreview(Font font, int x, int y, GuiGraphics gg) {
            List<MultiWindowPayloads.ItemPreviewEntry> items = this.preview.items();
            if (items.isEmpty()) {
                gg.drawString(font, "容器为空 (0/" + this.preview.totalSlots() + ")", x + 2, y + 2, 0xAAAAAA, false);
                return;
            }

            String header = "内容预览 (" + this.preview.occupiedSlots() + "/" + this.preview.totalSlots() + ")";
            gg.drawString(font, header, x + 2, y + 2, 0xB8C7FF, false);

            int startY = y + 14;
            for (int i = 0; i < items.size(); i++) {
                int col = i % 9;
                int row = i / 9;
                int sx = x + 2 + col * 18;
                int sy = startY + row * 18;

                gg.fill(sx, sy, sx + 17, sy + 17, 0xCC1E222D);
                gg.renderOutline(sx, sy, 17, 17, 0xFF3E465B);

                MultiWindowPayloads.ItemPreviewEntry entry = items.get(i);
                gg.renderItem(entry.stack(), sx + 1, sy + 1);
                String countText = formatCount(entry.count());
                gg.renderItemDecorations(font, entry.stack(), sx + 1, sy + 1, countText);
            }

            if (this.preview.occupiedSlots() > items.size()) {
                int rows = (items.size() + 8) / 9;
                String more = "... 还有 " + (this.preview.occupiedSlots() - items.size()) + " 格";
                gg.drawString(font, more, x + 2, startY + rows * 18 + 2, 0x999999, false);
            }
        }

        private void renderTradesPreview(Font font, int x, int y, GuiGraphics gg) {
            List<MultiWindowPayloads.TradeOfferPreview> trades = this.preview.trades();
            if (trades.isEmpty()) {
                gg.drawString(font, "暂无可用交易", x + 2, y + 2, 0xAAAAAA, false);
                return;
            }

            gg.drawString(font, "交易列表 (" + this.preview.totalSlots() + ")", x + 2, y + 2, 0xB8C7FF, false);
            int rowY = y + 14;
            for (MultiWindowPayloads.TradeOfferPreview t : trades) {
                gg.fill(x + 1, rowY, x + getWidth(font) - 1, rowY + 18, 0x991A1E28);

                int curX = x + 3;
                gg.renderItem(t.costA(), curX, rowY + 1);
                gg.renderItemDecorations(font, t.costA(), curX, rowY + 1);
                curX += 18;

                if (!t.costB().isEmpty()) {
                    gg.drawString(font, "+", curX, rowY + 5, 0x888888, false);
                    curX += 7;
                    gg.renderItem(t.costB(), curX, rowY + 1);
                    gg.renderItemDecorations(font, t.costB(), curX, rowY + 1);
                    curX += 18;
                }

                gg.drawString(font, t.outOfStock() ? "×" : "→", curX + 2, rowY + 5, t.outOfStock() ? 0xFF5555 : 0x55FF55, false);
                curX += 12;

                gg.renderItem(t.result(), curX, rowY + 1);
                gg.renderItemDecorations(font, t.result(), curX, rowY + 1);
                curX += 19;

                String label = font.plainSubstrByWidth(tradeResultLabel(t.result()), 96);
                gg.drawString(font, label, curX, rowY + 5, t.outOfStock() ? 0x888888 : 0xFFFFFF, false);
                rowY += 20;
            }
        }

        private static String tradeResultLabel(ItemStack result) {
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored != null && !stored.isEmpty()) {
                var first = stored.entrySet().iterator().next();
                String name = Enchantment.getFullname(first.getKey(), first.getIntValue()).getString();
                if (stored.size() > 1) name += " ...";
                return name;
            }
            return result.getHoverName().getString();
        }

        @Nullable
        private static String formatCount(int count) {
            if (count <= 1) return null;
            if (count < 1000) return String.valueOf(count);
            if (count < 10000) return String.format("%.1fk", count / 1000.0);
            return (count / 1000) + "k";
        }
    }
}
