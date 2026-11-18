package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.XpFlightClient;
import com.example.examplemod.client.layer.VillagerTradeClientCache;
import com.example.examplemod.deathrecap.DamageEventSnapshot;
import com.example.examplemod.deathrecap.client.ClientPayloadHandler;
import com.example.examplemod.skill.PlayerSkillData;
import com.example.examplemod.skill.SkillManager;
import com.example.examplemod.util.ArtifactAutoUpgradeHandler;
import com.example.examplemod.util.BackpackSortHelper;
import com.example.examplemod.util.DeepBackpackFetcher;
import com.mojang.datafixers.util.Pair;
import com.viscriptshop.util.ViScriptShopServerUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.example.examplemod.registry.ModRegistry.SKILL_DATA;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class ModPayloads {
    private ModPayloads() {}

    // ==========================================
    // 方向标记注解
    // ==========================================
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ServerBound {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClientBound {}

    public static <T extends CustomPacketPayload> Type<T> createType(String name) {
        return new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, name));
    }

    // =========================================================================
    // 客户端 -> 服务端 (C2S)
    // =========================================================================

    /** 1. 自动升级神器 (空包) */
    @ServerBound
    public record AutoUpgradeArtifactsPayload() implements CustomPacketPayload {
        public static final Type<AutoUpgradeArtifactsPayload> TYPE = createType("auto_upgrade_artifacts");
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sp) ArtifactAutoUpgradeHandler.processAutoUpgrade(sp);
        }
    }

    /** 2. 打开商店选择器 (空包) */
    @ServerBound
    public record OpenShopSelectorPayload() implements CustomPacketPayload {
        public static final Type<OpenShopSelectorPayload> TYPE = createType("open_shop_selector");
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sp) ViScriptShopServerUtil.serverOpenShopSelector(sp);
        }
    }

    /** 3. 睡觉按钮 (空包) */
    @ServerBound
    public record SleepButtonPayload() implements CustomPacketPayload {
        public static final Type<SleepButtonPayload> TYPE = createType("sleep_button");
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer player) handleSleepAction(player);
        }
    }

    /** 4. 整理到背包 */
    @ServerBound
    public record SortToBackpackPayload(boolean fromContainerOnly) implements CustomPacketPayload {
        public static final Type<SortToBackpackPayload> TYPE = createType("sort_to_backpack");
        public static final StreamCodec<ByteBuf, SortToBackpackPayload> STREAM_CODEC =
                ByteBufCodecs.BOOL.map(SortToBackpackPayload::new, SortToBackpackPayload::fromContainerOnly);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sp) BackpackSortHelper.sortInventoryToBackpack(sp, fromContainerOnly);
        }
    }

    /** 5. 交换装备（将原 ServerPayloadHandler 逻辑直接收拢到此处，彻底自给自足） */
    @ServerBound
    public record SwapEquipmentPayload(EquipmentSlot slot) implements CustomPacketPayload {
        public static final Type<SwapEquipmentPayload> TYPE = createType("swap_equipment");
        public static final StreamCodec<RegistryFriendlyByteBuf, SwapEquipmentPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.map(i -> EquipmentSlot.values()[i], Enum::ordinal),
                SwapEquipmentPayload::slot,
                SwapEquipmentPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            AbstractContainerMenu menu = player.containerMenu;
            ItemStack carried = menu.getCarried();
            ItemStack equipped = player.getItemBySlot(slot);
            boolean changed = false;

            if (carried.isEmpty()) {
                if (!equipped.isEmpty()) {
                    menu.setCarried(equipped.copy());
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                }
            } else if (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.MAINHAND || player.getEquipmentSlotForItem(carried) == slot) {
                if (equipped.isEmpty()) {
                    ItemStack toEquip = carried.split(1);
                    player.setItemSlot(slot, toEquip);
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                } else if (carried.getCount() == 1) {
                    player.setItemSlot(slot, carried.copy());
                    menu.setCarried(equipped.copy());
                    player.playNotifySound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    changed = true;
                }
            }

            if (changed) {
                menu.incrementStateId();
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        ClientboundContainerSetSlotPacket.CARRIED_ITEM, menu.getStateId(), -1, menu.getCarried()
                ));
                player.connection.send(new ClientboundSetEquipmentPacket(
                        player.getId(), List.of(Pair.of(slot, player.getItemBySlot(slot)))
                ));
                menu.broadcastChanges();
                menu.sendAllDataToRemote();
            }
        }
    }

    /** 6. 技能加点 */
    @ServerBound
    public record UpgradeSkillPayload(String nodeId) implements CustomPacketPayload {
        public static final Type<UpgradeSkillPayload> TYPE = createType("upgrade_skill");
        public static final StreamCodec<ByteBuf, UpgradeSkillPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, UpgradeSkillPayload::nodeId, UpgradeSkillPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sp) SkillManager.tryUpgradeSkill(sp, nodeId);
        }
    }

    /** 7. EMI 配方合成 */
    @ServerBound
    public record RequestEmiCraftingPayload(ResourceLocation recipeId, int requestedSets) implements CustomPacketPayload {
        public static final Type<RequestEmiCraftingPayload> TYPE = createType("emi_craft");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestEmiCraftingPayload> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, RequestEmiCraftingPayload::recipeId,
                ByteBufCodecs.VAR_INT, RequestEmiCraftingPayload::requestedSets,
                RequestEmiCraftingPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CraftingMenu menu) || !menu.stillValid(player)) return;
            int sets = Math.clamp(requestedSets, 1, 64);
            DeepBackpackFetcher.fillRecipe(player, recipeId, sets);
        }
    }

    /** 8. 抓取配方材料 */
    @ServerBound
    public record RequestFetchRecipeIngredientsPayload(List<SlotReq> items, boolean maxTransfer) implements CustomPacketPayload {
        public record SlotReq(int slot, ItemStack stack) {
            public static final StreamCodec<RegistryFriendlyByteBuf, SlotReq> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SlotReq::slot,
                    ItemStack.STREAM_CODEC, SlotReq::stack,
                    SlotReq::new
            );
        }

        public static final Type<RequestFetchRecipeIngredientsPayload> TYPE = createType("fetch_recipe_ingredients");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestFetchRecipeIngredientsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(ArrayList::new, SlotReq.STREAM_CODEC, 9), RequestFetchRecipeIngredientsPayload::items,
                ByteBufCodecs.BOOL, RequestFetchRecipeIngredientsPayload::maxTransfer,
                RequestFetchRecipeIngredientsPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CraftingMenu menu) || !menu.stillValid(player)) return;
            if (items.isEmpty() || items.size() > 9) return;

            boolean[] visited = new boolean[10];
            for (SlotReq req : items) {
                int slot = req.slot();
                ItemStack stack = req.stack();
                if (slot < 1 || slot > 9 || visited[slot]) return;
                visited[slot] = true;
                if (stack.isEmpty() || stack.getCount() != 1) return;
                if (!menu.getSlot(slot).mayPlace(stack) || menu.getSlot(slot).getMaxStackSize(stack) < 1) return;
            }
            // 内部直接将请求数据转换给通用 Fetcher
            DeepBackpackFetcher.fillCraftingGrid(player, items, 1);
        }
    }

    // =========================================================================
    // 服务端 -> 客户端 (S2C)
    // =========================================================================

    /** 9. 死亡统计数据 */
    @ClientBound
    public record DeathRecapPayload(List<DamageEventSnapshot> records) implements CustomPacketPayload {
        public static final Type<DeathRecapPayload> TYPE = createType("death_recap");
        public static final StreamCodec<RegistryFriendlyByteBuf, DeathRecapPayload> STREAM_CODEC =
                DamageEventSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DeathRecapPayload::new, DeathRecapPayload::records);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            // 直接传递内部实例自身
            ClientPayloadHandler.handleDeathRecap(this, ctx);
        }
    }

    /** 10. 经验飞行特效 */
    @ClientBound
    public record XpFlightPayload(int orbId, int playerId, Vec3 pos, long startTick, int duration) implements CustomPacketPayload {
        public static final Type<XpFlightPayload> TYPE = createType("xp_flight");
        public static final StreamCodec<ByteBuf, XpFlightPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, XpFlightPayload::orbId,
                ByteBufCodecs.VAR_INT, XpFlightPayload::playerId,
                StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new), XpFlightPayload::pos,
                ByteBufCodecs.VAR_LONG, XpFlightPayload::startTick,
                ByteBufCodecs.VAR_INT, XpFlightPayload::duration,
                XpFlightPayload::new
        );

        public static XpFlightPayload stop(int orbId) {
            return new XpFlightPayload(orbId, 0, Vec3.ZERO, 0, 0);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            // 直接传递内部实例自身
            XpFlightClient.receive(this);
        }
    }

    /** 11. 同步玩家技能数据 */
    @ClientBound
    public record SyncSkillDataPayload(PlayerSkillData data) implements CustomPacketPayload {
        public static final Type<SyncSkillDataPayload> TYPE = createType("sync_skill_data");
        public static final StreamCodec<ByteBuf, SyncSkillDataPayload> STREAM_CODEC = StreamCodec.composite(
                PlayerSkillData.STREAM_CODEC, SyncSkillDataPayload::data, SyncSkillDataPayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            ctx.player().setData(SKILL_DATA.get(), data);
        }
    }

    /** 12. 村民交易缓存 */
    @ClientBound
    public record ClientboundVillagerTradesPacket(int entityId, MerchantOffers offers) implements CustomPacketPayload {
        public static final Type<ClientboundVillagerTradesPacket> TYPE = createType("villager_trades");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundVillagerTradesPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ClientboundVillagerTradesPacket::entityId,
                MerchantOffers.STREAM_CODEC, ClientboundVillagerTradesPacket::offers,
                ClientboundVillagerTradesPacket::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public void handle(IPayloadContext ctx) {
            if (offers.isEmpty()) {
                VillagerTradeClientCache.remove(entityId);
            } else {
                VillagerTradeClientCache.put(entityId, offers);
            }
        }
    }

    // =========================================================================
    // 自动扫描引擎（无任何外部依赖）
    // =========================================================================

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ExampleMod.MODID).versioned("1.0.0");

        for (Class<?> clazz : ModPayloads.class.getDeclaredClasses()) {
            boolean isServer = clazz.isAnnotationPresent(ServerBound.class);
            boolean isClient = clazz.isAnnotationPresent(ClientBound.class);
            if (!isServer && !isClient) continue;
            if (!CustomPacketPayload.class.isAssignableFrom(clazz)) continue;

            try {
                Type type = (Type) clazz.getField("TYPE").get(null);

                StreamCodec codec;
                if (clazz.isRecord() && clazz.getRecordComponents().length == 0) {
                    Object emptyInstance = clazz.getConstructor().newInstance();
                    codec = StreamCodec.unit(emptyInstance);
                } else {
                    codec = (StreamCodec) clazz.getField("STREAM_CODEC").get(null);
                }

                Method handleMethod = findHandleMethod(clazz);

                if (isServer) {
                    registrar.playToServer(type, codec, (payload, ctx) -> ctx.enqueueWork(() -> invokeHandler(handleMethod, payload, ctx)));
                } else {
                    registrar.playToClient(type, codec, (payload, ctx) -> ctx.enqueueWork(() -> invokeHandler(handleMethod, payload, ctx)));
                }

            } catch (Exception e) {
                throw new RuntimeException("自动注册网络包异常: " + clazz.getSimpleName(), e);
            }
        }
    }

    private static Method findHandleMethod(Class<?> clazz) throws NoSuchMethodException {
        try {
            return clazz.getMethod("handle", IPayloadContext.class);
        } catch (NoSuchMethodException e) {
            return clazz.getMethod("handle", clazz, IPayloadContext.class);
        }
    }

    private static void invokeHandler(Method method, Object payload, IPayloadContext ctx) {
        try {
            if (Modifier.isStatic(method.getModifiers())) {
                method.invoke(null, payload, ctx);
            } else {
                method.invoke(payload, ctx);
            }
        } catch (Exception e) {
            throw new RuntimeException("执行网络包逻辑异常", e);
        }
    }

    private static void handleSleepAction(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!level.dimensionType().bedWorks()) {
            player.sendSystemMessage(Component.translatable("examplemod.message.bed_explodes"));
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay < 12542L || timeOfDay >= 23460L) {
            player.sendSystemMessage(Component.translatable("examplemod.message.cannot_sleep_now"));
            return;
        }

        BlockPos respawnPos = player.blockPosition();
        player.setRespawnPosition(level.dimension(), respawnPos, player.getYRot(), false, true);

        long nextMorning = ((level.getDayTime() / 24000L) + 1L) * 24000L + 1000L;
        level.setDayTime(nextMorning);
        if (level.isRaining()) level.resetWeatherCycle();
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));

        player.closeContainer();
        player.sendSystemMessage(Component.translatable("examplemod.message.woke_up"));
    }
}