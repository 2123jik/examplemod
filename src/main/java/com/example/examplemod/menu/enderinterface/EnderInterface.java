package com.example.examplemod.menu.enderinterface;

import com.example.examplemod.ExampleMod;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Mod(ExampleMod.MODID)
public class EnderInterface {
    public static final String MODID = "examplemod"; // <-- 确认你的 modid
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public record EntitySnapshot(
            ResourceLocation entityTypeId,
            CompoundTag visualTag
    ) {
        public static final Codec<EntitySnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(EntitySnapshot::entityTypeId),
                CompoundTag.CODEC.optionalFieldOf("tag", new CompoundTag()).forGetter(EntitySnapshot::visualTag)
        ).apply(inst, EntitySnapshot::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EntitySnapshot> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, EntitySnapshot::entityTypeId,
                ByteBufCodecs.TRUSTED_COMPOUND_TAG, EntitySnapshot::visualTag,
                EntitySnapshot::new
        );
    }

    public record RemoteData(
            ResourceKey<Level> dim,
            BlockPos pos,
            ItemStack iconStack,
            Optional<UUID> entityUuid,
            Optional<EntitySnapshot> entitySnapshot,
            int pickupMode
    ) {
        public static final int PICKUP_OFF = 0;
        public static final int PICKUP_FILTER = 1;
        public static final int PICKUP_EXISTING = 2;
        public static final int PICKUP_ALL = 3;

        public RemoteData(ResourceKey<Level> dim, BlockPos pos, ItemStack iconStack) {
            this(dim, pos, iconStack, Optional.empty(), Optional.empty(), PICKUP_OFF);
        }

        public RemoteData(ResourceKey<Level> dim, BlockPos pos, ItemStack iconStack, Optional<UUID> entityUuid) {
            this(dim, pos, iconStack, entityUuid, Optional.empty(), PICKUP_OFF);
        }

        public RemoteData(ResourceKey<Level> dim, BlockPos pos, ItemStack iconStack,
                          Optional<UUID> entityUuid, Optional<EntitySnapshot> entitySnapshot) {
            this(dim, pos, iconStack, entityUuid, entitySnapshot, PICKUP_OFF);
        }

        public RemoteData withPos(BlockPos newPos) {
            return new RemoteData(this.dim, newPos.immutable(), this.iconStack, this.entityUuid, this.entitySnapshot, this.pickupMode);
        }

        public RemoteData withPickupMode(int mode) {
            return new RemoteData(this.dim, this.pos, this.iconStack, this.entityUuid, this.entitySnapshot, mode);
        }

        public String targetKey() {
            if (this.entityUuid.isPresent()) {
                return "E|" + this.dim.location() + "|" + this.entityUuid.get();
            }
            return "B|" + this.dim.location() + "|" + this.pos.asLong();
        }

        public static final Codec<RemoteData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(RemoteData::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(RemoteData::pos),
                ItemStack.OPTIONAL_CODEC.fieldOf("icon").forGetter(RemoteData::iconStack),
                net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(RemoteData::entityUuid),
                EntitySnapshot.CODEC.optionalFieldOf("entity_info").forGetter(RemoteData::entitySnapshot),
                Codec.INT.optionalFieldOf("pickup_mode", PICKUP_OFF).forGetter(RemoteData::pickupMode)
        ).apply(inst, RemoteData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RemoteData> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), RemoteData::dim,
                BlockPos.STREAM_CODEC, RemoteData::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, RemoteData::iconStack,
                ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), RemoteData::entityUuid,
                ByteBufCodecs.optional(EntitySnapshot.STREAM_CODEC), RemoteData::entitySnapshot,
                ByteBufCodecs.VAR_INT, RemoteData::pickupMode,
                RemoteData::new
        );
    }

    public record RemotePreviewTooltip(MultiWindowPayloads.PreviewData preview) implements TooltipComponent {}

    public static final Supplier<DataComponentType<RemoteData>> REMOTE_DATA = DATA_COMPONENTS.registerComponentType(
            "remote_data", builder -> builder.persistent(RemoteData.CODEC).networkSynchronized(RemoteData.STREAM_CODEC)
    );

    // 兼容保留：其他类如果通过 INTERFACE_ITEM.get() 判断物品，无需修改即可直接适配原版末影之眼
    public static final Supplier<Item> INTERFACE_ITEM = () -> Items.ENDER_EYE;

    private record PendingEntityOpen(
            UUID playerUuid,
            RemoteData data,
            UUID entityUuid,
            BlockPos ticketPos,
            int ticksLeft
    ) {}

    private record TargetKey(ResourceKey<Level> dim, BlockPos pos) {}
    private record PickupTarget(RemoteData data, IItemHandler handler) {}

    private static final List<PendingEntityOpen> PENDING_ENTITY_OPENS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Long> LAST_PREVIEW_TIME = new ConcurrentHashMap<>();
    private static int leaseTickCounter = 0;

    public EnderInterface(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    public static Component pickupModeLabel(int mode) {
        return switch (mode) {
            case RemoteData.PICKUP_FILTER -> Component.literal("§a容器过滤 (精妙存储)");
            case RemoteData.PICKUP_EXISTING -> Component.literal("§b仅已有同类");
            case RemoteData.PICKUP_ALL -> Component.literal("§6全部接收 (兜底)");
            default -> Component.literal("§7关闭");
        };
    }

    public static void handleCyclePickupMode(ServerPlayer sp) {
        if (sp.isSpectator()) return;
        ItemStack stack = sp.getMainHandItem();
        if (!stack.is(INTERFACE_ITEM.get())) return;
        RemoteData data = stack.get(REMOTE_DATA.get());
        if (data == null) return;

        if (data.entityUuid().isPresent()) {
            sp.displayClientMessage(Component.literal("§e实体绑定暂不支持自动归类"), true);
            return;
        }

        int nextMode = (data.pickupMode() + 1) % 4;
        stack.set(REMOTE_DATA.get(), data.withPickupMode(nextMode));
        sp.displayClientMessage(
                Component.literal("§f自动归类模式: ").append(pickupModeLabel(nextMode)),
                true
        );
        sp.level().playSound(
                null,
                sp.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.35F,
                0.85F + nextMode * 0.18F
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (++leaseTickCounter >= 20) {
            leaseTickCounter = 0;
            MultiMenuManager.tickAutoPickupLeases(server);
        }

        if (PENDING_ENTITY_OPENS.isEmpty()) return;
        for (PendingEntityOpen pending : PENDING_ENTITY_OPENS) {
            ServerPlayer sp = server.getPlayerList().getPlayer(pending.playerUuid());
            ServerLevel level = server.getLevel(pending.data().dim());
            if (sp == null || level == null) {
                MultiMenuManager.releaseChunk(server, pending.data().dim(), pending.ticketPos());
                PENDING_ENTITY_OPENS.remove(pending);
                continue;
            }
            Entity entity = level.getEntity(pending.entityUuid());
            long chunkLong = ChunkPos.asLong(pending.ticketPos());
            if (entity != null || level.areEntitiesLoaded(chunkLong) || pending.ticksLeft() <= 1) {
                PENDING_ENTITY_OPENS.remove(pending);
                if (entity != null) {
                    openLoadedEntity(sp, level, pending.data(), entity, pending.ticketPos());
                } else {
                    MultiMenuManager.releaseChunk(server, pending.data().dim(), pending.ticketPos());
                    sp.sendSystemMessage(Component.translatableWithFallback(
                            "message.examplemod.interface.entity_missing",
                            "§c目标实体不存在、已死亡或已消失！"));
                }
            } else {
                PENDING_ENTITY_OPENS.remove(pending);
                PENDING_ENTITY_OPENS.add(new PendingEntityOpen(
                        pending.playerUuid(), pending.data(), pending.entityUuid(),
                        pending.ticketPos(), pending.ticksLeft() - 1
                ));
            }
        }
    }

    @SubscribeEvent
    public void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (event.canPickup() == TriState.FALSE) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (sp.isSpectator()) return;

        ItemEntity itemEntity = event.getItemEntity();
        if (itemEntity.isRemoved() || itemEntity.hasPickUpDelay()) return;

        ItemStack groundStack = itemEntity.getItem();
        if (groundStack.isEmpty()) return;

        // 防止意外将已绑定的末影之眼自动吸进远程箱子
        if (groundStack.is(INTERFACE_ITEM.get()) && groundStack.has(REMOTE_DATA.get())) return;

        // 1. 快捷栏（含副手）未满组优先补满：先扣掉快捷栏还能装下的数量
        int hotbarSpace = getHotbarMatchingSpace(sp, groundStack);
        int availableToRoute = groundStack.getCount() - hotbarSpace;
        if (availableToRoute <= 0) {
            return;
        }

        // 2. 收集玩家身上及“中枢箱”内所有开启自动归类的末影之眼
        List<PickupTarget> targets = collectAutoPickupTargets(sp);
        if (targets.isEmpty()) {
            return;
        }

        // 3. 过滤/已有同类优先，兜底箱放最后（稳定排序，保持格子先后顺序）
        targets.sort(Comparator.comparingInt(t -> t.data().pickupMode() == RemoteData.PICKUP_ALL ? 1 : 0));

        ItemStack remainingToRoute = groundStack.copyWithCount(availableToRoute);
        for (PickupTarget target : targets) {
            if (remainingToRoute.isEmpty()) break;
            if (target.data().pickupMode() == RemoteData.PICKUP_EXISTING
                    && !hasExistingItem(target.handler(), remainingToRoute)) {
                continue;
            }
            remainingToRoute = ItemHandlerHelper.insertItemStacked(target.handler(), remainingToRoute, false);
        }

        int routedCount = availableToRoute - remainingToRoute.getCount();
        if (routedCount <= 0) {
            return;
        }

        // 4. 补全原版拾取动画、音效与统计（兼容任务书/成就）
        sp.take(itemEntity, routedCount);
        sp.awardStat(Stats.ITEM_PICKED_UP.get(groundStack.getItem()), routedCount);

        int leftOnGround = groundStack.getCount() - routedCount;
        if (leftOnGround <= 0) {
            itemEntity.discard();
            event.setCanPickup(TriState.FALSE);
        } else {
            itemEntity.setItem(groundStack.copyWithCount(leftOnGround));
        }
    }

    private static int getHotbarMatchingSpace(ServerPlayer sp, ItemStack target) {
        Inventory inv = sp.getInventory();
        int maxStack = Math.min(target.getMaxStackSize(), inv.getMaxStackSize());
        if (maxStack <= 1) return 0;

        int space = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getCount() < maxStack && ItemStack.isSameItemSameComponents(s, target)) {
                space += (maxStack - s.getCount());
            }
        }
        ItemStack offhand = sp.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getCount() < maxStack && ItemStack.isSameItemSameComponents(offhand, target)) {
            space += (maxStack - offhand.getCount());
        }
        return space;
    }

    private static List<PickupTarget> collectAutoPickupTargets(ServerPlayer sp) {
        List<PickupTarget> targets = new ArrayList<>();
        Set<TargetKey> visited = new HashSet<>();
        Inventory inv = sp.getInventory();

        for (int i = 0; i < 36; i++) {
            collectFromEyeStack(sp, inv.getItem(i), targets, visited);
        }
        collectFromEyeStack(sp, sp.getOffhandItem(), targets, visited);
        return targets;
    }

    private static void collectFromEyeStack(ServerPlayer sp, ItemStack stack,
                                            List<PickupTarget> targets, Set<TargetKey> visited) {
        if (stack.isEmpty() || !stack.is(INTERFACE_ITEM.get())) return;
        RemoteData data = stack.get(REMOTE_DATA.get());
        if (data == null || data.pickupMode() <= RemoteData.PICKUP_OFF || data.entityUuid().isPresent()) {
            return;
        }

        TargetKey key = new TargetKey(data.dim(), data.pos());
        if (!visited.add(key)) return;

        IItemHandler handler = resolveBlockItemHandler(sp, data);
        if (handler == null) return;

        // 若容器内部放了其他已绑定的末影之眼，则视其为“中枢箱”，按格子展开子眼睛且不把杂物塞进中枢箱本身
        if (containsBoundEye(handler)) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack inner = handler.getStackInSlot(i);
                if (inner.isEmpty() || !inner.is(INTERFACE_ITEM.get())) continue;
                RemoteData child = inner.get(REMOTE_DATA.get());
                if (child == null || child.pickupMode() <= RemoteData.PICKUP_OFF || child.entityUuid().isPresent()) {
                    continue;
                }
                TargetKey childKey = new TargetKey(child.dim(), child.pos());
                if (!visited.add(childKey)) continue;

                IItemHandler childHandler = resolveBlockItemHandler(sp, child);
                if (childHandler != null) {
                    targets.add(new PickupTarget(child, childHandler));
                }
            }
            return;
        }

        targets.add(new PickupTarget(data, handler));
    }

    @Nullable
    private static IItemHandler resolveBlockItemHandler(ServerPlayer sp, RemoteData data) {
        if (data.entityUuid().isPresent()) return null;
        ServerLevel targetLevel = sp.server.getLevel(data.dim());
        if (targetLevel == null) return null;

        MultiMenuManager.touchAutoPickupChunk(targetLevel, data.pos());
        BlockState state = targetLevel.getBlockState(data.pos());
        if (state.isAir()) return null;
        if (state.is(Blocks.ENDER_CHEST)) {
            return new InvWrapper(sp.getEnderChestInventory());
        }

        IItemHandler handler = targetLevel.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), Direction.UP);
        if (handler == null) {
            handler = targetLevel.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), null);
        }
        if (handler == null && targetLevel.getBlockEntity(data.pos()) instanceof Container container) {
            handler = new InvWrapper(container);
        }
        return handler;
    }

    private static boolean containsBoundEye(IItemHandler handler) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && s.is(INTERFACE_ITEM.get()) && s.has(REMOTE_DATA.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExistingItem(IItemHandler handler, ItemStack target) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (s.is(INTERFACE_ITEM.get()) && s.has(REMOTE_DATA.get())) continue;
            if (ItemStack.isSameItemSameComponents(s, target)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack sanitizeIconStack(ItemStack src) {
        ItemStack copy = src.copyWithCount(1);
        copy.remove(DataComponents.CONTAINER);
        copy.remove(DataComponents.BLOCK_ENTITY_DATA);
        copy.remove(DataComponents.BUNDLE_CONTENTS);
        copy.remove(DataComponents.CUSTOM_DATA);
        copy.remove(DataComponents.CONTAINER_LOOT);
        copy.remove(DataComponents.ENTITY_DATA);
        return copy;
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            LAST_PREVIEW_TIME.remove(sp.getUUID());
            MultiMenuManager.closeAll(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MultiMenuManager.closeAll(sp);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PENDING_ENTITY_OPENS.clear();
        LAST_PREVIEW_TIME.clear();
        leaseTickCounter = 0;
        MultiMenuManager.clearAllOnServerStop();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.ENDER_EYE)) return;

        BlockPos pos = event.getPos();
        if (player.isShiftKeyDown()) {
            BlockState state = level.getBlockState(pos);
            Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
            ItemStack iconStack = state.getCloneItemStack(
                    new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false),
                    level, pos, player
            );
            if (iconStack.isEmpty()) {
                Item asItem = state.getBlock().asItem();
                iconStack = (asItem != Items.AIR) ? new ItemStack(asItem) : new ItemStack(Items.ENDER_EYE);
            }
            iconStack = sanitizeIconStack(iconStack);

            RemoteData data = new RemoteData(level.dimension(), pos.immutable(), iconStack);
            applyBoundEye(player, stack, data);

            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "message.examplemod.interface.bound",
                        "§a已绑定至: §e%s §7[%s]",
                        iconStack.getHoverName().getString(),
                        pos.toShortString()
                ));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        // 已绑定的末影之眼对着方块右键时直接打开远程界面，防止触发方块交互或被末地传送门框架消耗
        if (stack.has(REMOTE_DATA.get())) {
            if (!level.isClientSide) {
                openRemote(player, stack);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.ENDER_EYE)) return;

        // 未绑定的末影之眼不拦截，保留原版丢出寻路功能
        if (!stack.has(REMOTE_DATA.get())) return;

        // 已绑定的末影之眼即使在潜行状态下右键空气也不能丢出消耗
        if (player.isShiftKeyDown()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }

        if (!level.isClientSide) {
            openRemote(player, stack);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleEntityInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleEntityInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
    }

    private static boolean handleEntityInteract(Player player, Level level, InteractionHand hand, Entity target) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.ENDER_EYE)) return false;

        if (player.isShiftKeyDown()) {
            return handleEntityBind(player, level, hand, target);
        }

        // 拿着已绑定的末影之眼对着实体右键时，直接打开远程界面，避免误触实体的右键动作（如回收傀儡）
        if (stack.has(REMOTE_DATA.get())) {
            if (!level.isClientSide) {
                openRemote(player, stack);
            }
            return true;
        }
        return false;
    }

    private static boolean handleEntityBind(Player player, Level level, InteractionHand hand, Entity target) {
        if (!player.isShiftKeyDown()) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.ENDER_EYE)) return false;

        ItemStack iconStack = target.getPickResult();
        if (iconStack == null || iconStack.isEmpty()) {
            SpawnEggItem egg = SpawnEggItem.byId(target.getType());
            iconStack = (egg != null) ? new ItemStack(egg) : new ItemStack(Items.EMERALD);
        }
        iconStack = sanitizeIconStack(iconStack);

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        EntitySnapshot snapshot = new EntitySnapshot(typeId, captureEntityVisualTag(target));

        RemoteData data = new RemoteData(
                level.dimension(),
                target.blockPosition(),
                iconStack,
                Optional.of(target.getUUID()),
                Optional.of(snapshot),
                RemoteData.PICKUP_OFF
        );
        applyBoundEye(player, stack, data);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            EntityTrackerData.get(serverLevel).update(target);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.bound_entity",
                    "§a已绑定实体: §e%s",
                    target.getDisplayName().getString()
            ));
        }
        return true;
    }

    private static void applyBoundEye(Player player, ItemStack handStack, RemoteData data) {
        if (handStack.getCount() == 1) {
            configureBoundEye(handStack, data);
            return;
        }
        // 手里拿了多个末影之眼时，只消耗 1 个生成绑定物品，避免整组同时绑定
        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                handStack.shrink(1);
            }
            ItemStack bound = new ItemStack(Items.ENDER_EYE, 1);
            configureBoundEye(bound, data);
            if (!player.addItem(bound)) {
                player.drop(bound, false);
            }
        }
    }

    private static void configureBoundEye(ItemStack stack, RemoteData data) {
        stack.set(REMOTE_DATA.get(), data);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        if (!data.iconStack().isEmpty()) {
            stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(
                    "item.examplemod.interface.bound_name",
                    "末影之眼 (%s)",
                    data.iconStack().getHoverName()
            ));
        }
    }

    private static CompoundTag captureEntityVisualTag(Entity target) {
        CompoundTag tag = new CompoundTag();
        try {
            target.saveWithoutId(tag);
        } catch (Throwable ignored) {
            return new CompoundTag();
        }
        String[] stripKeys = {
                "UUID", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
                "OnGround", "PortalCooldown", "Passengers", "Leash", "leash",
                "Offers", "Brain", "Gossips", "Inventory", "Items", "ActiveEffects",
                "active_effects", "Attributes", "attributes", "Memories", "LastRestock",
                "RestocksToday", "Xp", "DespawnDelay", "WanderingTarget", "ForgeData",
                "neoforge:attachments", "KubeJSPersistentData", "DeathTime", "HurtTime",
                "HurtByTimestamp", "Health"
        };
        for (String key : stripKeys) {
            tag.remove(key);
        }
        return tag;
    }

    public static void handlePreviewRequest(ServerPlayer sp, RemoteData data) {
        if (sp.isSpectator()) return;
        long now = System.currentTimeMillis();
        Long prev = LAST_PREVIEW_TIME.get(sp.getUUID());
        if (prev != null && now - prev < 80L) return;
        LAST_PREVIEW_TIME.put(sp.getUUID(), now);

        ServerLevel level = sp.server.getLevel(data.dim());
        if (level == null) {
            PacketDistributor.sendToPlayer(sp, new MultiWindowPayloads.PreviewResponsePayload(data.targetKey(), MultiWindowPayloads.PreviewData.EMPTY));
            return;
        }

        MultiWindowPayloads.PreviewData preview = MultiWindowPayloads.PreviewData.EMPTY;
        if (data.entityUuid().isPresent()) {
            UUID uuid = data.entityUuid().get();
            BlockPos loadPos = EntityTrackerData.get(level).getPos(uuid, data.pos());
            boolean acquired = false;
            Entity target = level.getEntity(uuid);
            if (target == null) {
                MultiMenuManager.acquireChunk(level, loadPos);
                acquired = true;
                target = level.getEntity(uuid);
            }
            try {
                if (target != null && target.isAlive()) {
                    EntityTrackerData.get(level).update(target);
                    if (target instanceof Merchant merchant) {
                        preview = buildTradePreview(merchant);
                    } else {
                        IItemHandler handler = target.getCapability(Capabilities.ItemHandler.ENTITY, null);
                        if (handler != null) {
                            preview = buildHandlerPreview(handler);
                        } else if (target instanceof Container container) {
                            preview = buildContainerPreview(container);
                        }
                    }
                }
            } finally {
                if (acquired) {
                    MultiMenuManager.releaseChunk(sp.server, data.dim(), loadPos);
                }
            }
        } else {
            boolean acquired = !level.isLoaded(data.pos());
            if (acquired) {
                MultiMenuManager.acquireChunk(level, data.pos());
            }
            try {
                BlockState state = level.getBlockState(data.pos());
                if (state.is(Blocks.ENDER_CHEST)) {
                    preview = buildContainerPreview(sp.getEnderChestInventory());
                } else {
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, data.pos(), null);
                    if (handler != null) {
                        preview = buildHandlerPreview(handler);
                    } else {
                        BlockEntity be = level.getBlockEntity(data.pos());
                        if (be instanceof Container container) {
                            preview = buildContainerPreview(container);
                        }
                    }
                }
            } finally {
                if (acquired) {
                    MultiMenuManager.releaseChunk(sp.server, data.dim(), data.pos());
                }
            }
        }

        PacketDistributor.sendToPlayer(sp, new MultiWindowPayloads.PreviewResponsePayload(data.targetKey(), preview));
    }

    private static ItemStack sanitizePreviewStack(ItemStack src) {
        ItemStack copy = src.copyWithCount(1);
        copy.remove(DataComponents.CONTAINER);
        copy.remove(DataComponents.BLOCK_ENTITY_DATA);
        copy.remove(DataComponents.BUNDLE_CONTENTS);
        copy.remove(DataComponents.CUSTOM_DATA);
        return copy;
    }

    private static MultiWindowPayloads.PreviewData buildHandlerPreview(IItemHandler handler) {
        int slots = handler.getSlots();
        int occupied = 0;
        List<MultiWindowPayloads.ItemPreviewEntry> list = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) {
                occupied++;
                if (list.size() < 45) {
                    list.add(new MultiWindowPayloads.ItemPreviewEntry(sanitizePreviewStack(s), s.getCount()));
                }
            }
        }
        return new MultiWindowPayloads.PreviewData(MultiWindowPayloads.PREVIEW_ITEMS, slots, occupied, list, List.of());
    }

    private static MultiWindowPayloads.PreviewData buildContainerPreview(Container container) {
        int slots = container.getContainerSize();
        int occupied = 0;
        List<MultiWindowPayloads.ItemPreviewEntry> list = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) {
                occupied++;
                if (list.size() < 45) {
                    list.add(new MultiWindowPayloads.ItemPreviewEntry(sanitizePreviewStack(s), s.getCount()));
                }
            }
        }
        return new MultiWindowPayloads.PreviewData(MultiWindowPayloads.PREVIEW_ITEMS, slots, occupied, list, List.of());
    }

    private static MultiWindowPayloads.PreviewData buildTradePreview(Merchant merchant) {
        List<MultiWindowPayloads.TradeOfferPreview> list = new ArrayList<>();
        var offers = merchant.getOffers();
        int max = Math.min(offers.size(), 10);
        for (int i = 0; i < max; i++) {
            MerchantOffer offer = offers.get(i);
            ItemStack costA = offer.getCostA().copy();
            ItemStack costB = offer.getCostB().copy();
            ItemStack result = offer.getResult().copy();
            list.add(new MultiWindowPayloads.TradeOfferPreview(costA, costB, result, offer.isOutOfStock()));
        }
        return new MultiWindowPayloads.PreviewData(MultiWindowPayloads.PREVIEW_TRADES, offers.size(), offers.size(), List.of(), list);
    }

    public static void openRemote(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        RemoteData data = stack.get(REMOTE_DATA.get());
        if (data == null) {
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.unbound", "§c该末影之眼尚未绑定任何目标！"));
            return;
        }

        ServerLevel level = serverPlayer.server.getLevel(data.dim());
        if (level == null) {
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.unloaded", "§c目标维度不存在！"));
            return;
        }

        if (data.entityUuid().isPresent()) {
            openRemoteEntity(serverPlayer, level, data, data.entityUuid().get());
            return;
        }

        MultiMenuManager.RemoteSession existing = MultiMenuManager.findByTarget(serverPlayer, data.dim(), data.pos());
        if (existing != null && MultiMenuManager.isSessionTargetValid(serverPlayer, existing)) {
            serverPlayer.containerMenu = existing.menu();
            PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.FocusRemoteClientPayload(existing.menu().containerId));
            return;
        }

        MultiMenuManager.acquireChunk(level, data.pos());

        BlockState state = level.getBlockState(data.pos());
        MenuProvider provider = state.is(Blocks.ENDER_CHEST)
                ? new SimpleMenuProvider(
                (id, inv, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inv, p.getEnderChestInventory()),
                Component.translatable("container.enderchest"))
                : state.getMenuProvider(level, data.pos());

        AbstractContainerMenu parkedPrev = null;
        if (MultiMenuManager.isTracked(serverPlayer, serverPlayer.containerMenu)) {
            parkedPrev = serverPlayer.containerMenu;
            serverPlayer.containerMenu = serverPlayer.inventoryMenu;
        }
        AbstractContainerMenu menuBefore = serverPlayer.containerMenu;

        var snapshot = RemoteBlockSnapshot.capture(level, data.pos());
        PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.PrepareRemotePayload(data, snapshot));

        boolean openedOk = false;
        if (provider != null) {
            L2MenuStackerCompat.clearServerTrackerIfWid(serverPlayer, -1);
            MultiMenuManager.beginInterfaceOpen();
            try {
                OptionalInt opened = serverPlayer.openMenu(provider, data.pos());
                openedOk = opened.isPresent();
            } finally {
                MultiMenuManager.endInterfaceOpen();
            }
        } else if (level.getBlockEntity(data.pos()) != null) {
            MultiMenuManager.beginVirtualUse(serverPlayer, data.pos());
            try {
                var hit = new BlockHitResult(
                        Vec3.atCenterOf(data.pos()),
                        Direction.UP,
                        data.pos(), false
                );
                state.useWithoutItem(level, serverPlayer, hit);
                openedOk = serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            } finally {
                MultiMenuManager.endVirtualUse(serverPlayer);
            }
        }

        if (openedOk) {
            MultiMenuManager.register(serverPlayer, data.dim(), data.pos(), state.getBlock(), serverPlayer.containerMenu, data.iconStack());
        } else {
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), data.pos());
            if (parkedPrev != null) {
                serverPlayer.containerMenu = parkedPrev;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.ClearPendingPayload());
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.no_menu", "§c目标方块不存在或没有可打开的界面！"));
        }
    }

    private static void openRemoteEntity(ServerPlayer serverPlayer, ServerLevel level, RemoteData data, UUID entityUuid) {
        MultiMenuManager.RemoteSession existing = MultiMenuManager.findByEntity(serverPlayer, entityUuid);
        if (existing != null && MultiMenuManager.isSessionTargetValid(serverPlayer, existing)) {
            serverPlayer.containerMenu = existing.menu();
            PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.FocusRemoteClientPayload(existing.menu().containerId));
            return;
        }

        BlockPos loadPos = EntityTrackerData.get(level).getPos(entityUuid, data.pos());
        MultiMenuManager.acquireChunk(level, loadPos);

        Entity target = level.getEntity(entityUuid);
        if (target == null && !loadPos.equals(data.pos())) {
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), loadPos);
            loadPos = data.pos();
            MultiMenuManager.acquireChunk(level, loadPos);
            target = level.getEntity(entityUuid);
        }

        if (target == null) {
            long chunkLong = ChunkPos.asLong(loadPos);
            if (!level.areEntitiesLoaded(chunkLong)) {
                PENDING_ENTITY_OPENS.removeIf(p -> p.playerUuid().equals(serverPlayer.getUUID()) && p.entityUuid().equals(entityUuid));
                PENDING_ENTITY_OPENS.add(new PendingEntityOpen(serverPlayer.getUUID(), data, entityUuid, loadPos.immutable(), 20));
                return;
            }
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), loadPos);
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.entity_missing",
                    "§c目标实体不存在、已死亡或已消失！"));
            return;
        }

        openLoadedEntity(serverPlayer, level, data, target, loadPos);
    }

    private static void openLoadedEntity(ServerPlayer serverPlayer, ServerLevel level, RemoteData data,
                                         Entity target, BlockPos ticketPos) {
        if (!target.isAlive() || target.isRemoved()) {
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), ticketPos);
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.entity_missing",
                    "§c目标实体不存在、已死亡或已消失！"));
            return;
        }

        BlockPos actualPos = target.blockPosition();
        if (!new ChunkPos(ticketPos).equals(new ChunkPos(actualPos))) {
            MultiMenuManager.acquireChunk(level, actualPos);
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), ticketPos);
            ticketPos = actualPos;
        }
        EntityTrackerData.get(level).update(target);

        if (target instanceof Merchant merchant) {
            Player tradingPlayer = merchant.getTradingPlayer();
            if (tradingPlayer != null && tradingPlayer != serverPlayer) {
                MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), ticketPos);
                serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                        "message.examplemod.interface.entity_busy",
                        "§c目标商人正在与其他玩家交易！"));
                return;
            }
            if (merchant.getOffers().isEmpty()) {
                MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), ticketPos);
                serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                        "message.examplemod.interface.entity_no_menu",
                        "§c该实体当前没有可打开的交易或界面！"));
                return;
            }
        }

        AbstractContainerMenu parkedPrev = null;
        if (MultiMenuManager.isTracked(serverPlayer, serverPlayer.containerMenu)) {
            parkedPrev = serverPlayer.containerMenu;
            serverPlayer.containerMenu = serverPlayer.inventoryMenu;
        }
        AbstractContainerMenu menuBefore = serverPlayer.containerMenu;

        RemoteData actualData = data.withPos(actualPos);
        PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.PrepareRemotePayload(actualData, Optional.empty()));

        boolean openedOk = false;
        L2MenuStackerCompat.clearServerTrackerIfWid(serverPlayer, -1);
        MultiMenuManager.beginInterfaceOpen();
        MultiMenuManager.beginVirtualEntityUse(serverPlayer, target.getUUID());
        ItemStack savedMainHand = serverPlayer.getMainHandItem();
        try {
            if (isModularGolem(target)) {
                openedOk = openModularGolem(serverPlayer, target, menuBefore);
            } else if (target instanceof MenuProvider mp) {
                openedOk = serverPlayer.openMenu(mp).isPresent();
            } else if (target instanceof HasCustomInventoryScreen customInv) {
                customInv.openCustomInventoryScreen(serverPlayer);
                openedOk = serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            } else if (target instanceof Villager) {
                serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                target.interact(serverPlayer, InteractionHand.MAIN_HAND);
                openedOk = serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            } else if (target instanceof Merchant merchant) {
                merchant.setTradingPlayer(serverPlayer);
                merchant.openTradingScreen(serverPlayer, target.getDisplayName(), 1);
                openedOk = serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            } else {
                serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                target.interact(serverPlayer, InteractionHand.MAIN_HAND);
                openedOk = serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            }
        } finally {
            serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, savedMainHand);
            MultiMenuManager.endVirtualEntityUse(serverPlayer);
            MultiMenuManager.endInterfaceOpen();
        }

        if (openedOk) {
            MultiMenuManager.registerEntity(serverPlayer, data.dim(), actualPos, target.getUUID(),
                    serverPlayer.containerMenu, data.iconStack());
        } else {
            MultiMenuManager.releaseChunk(serverPlayer.server, data.dim(), ticketPos);
            if (parkedPrev != null) {
                serverPlayer.containerMenu = parkedPrev;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new MultiWindowPayloads.ClearPendingPayload());
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.entity_no_menu",
                    "§c目标实体没有可打开的界面！"));
        }
    }

    public record OpenPacket(int slotId, ItemStack clientStack) implements CustomPacketPayload {
        public static final Type<OpenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_remote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenPacket::slotId,
                ItemStack.OPTIONAL_STREAM_CODEC, OpenPacket::clientStack,
                OpenPacket::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean isModularGolem(Entity target) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if ("modulargolems".equals(typeId.getNamespace())) {
            return true;
        }
        Class<?> cls = target.getClass();
        while (cls != null && cls != Object.class) {
            if ("dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity".equals(cls.getName())) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static boolean openModularGolem(ServerPlayer serverPlayer, Entity target, AbstractContainerMenu menuBefore) {
        Item commandWand = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("modulargolems", "command_wand"))
                .orElse(null);
        if (commandWand == null || commandWand == Items.AIR) {
            return false;
        }

        boolean wasSneaking = serverPlayer.isShiftKeyDown();
        try {
            serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(commandWand));

            serverPlayer.setShiftKeyDown(false);
            if (tryInteractForMenu(serverPlayer, target, menuBefore)) {
                return true;
            }

            serverPlayer.setShiftKeyDown(true);
            return tryInteractForMenu(serverPlayer, target, menuBefore);
        } finally {
            serverPlayer.setShiftKeyDown(wasSneaking);
        }
    }

    private static boolean tryInteractForMenu(ServerPlayer serverPlayer, Entity target, AbstractContainerMenu menuBefore) {
        var specificEvent = new PlayerInteractEvent.EntityInteractSpecific(
                serverPlayer, InteractionHand.MAIN_HAND, target, Vec3.ZERO
        );
        NeoForge.EVENT_BUS.post(specificEvent);
        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu && serverPlayer.containerMenu != menuBefore) {
            return true;
        }
        if (!specificEvent.isCanceled()) {
            var interactEvent = new PlayerInteractEvent.EntityInteract(
                    serverPlayer, InteractionHand.MAIN_HAND, target
            );
            NeoForge.EVENT_BUS.post(interactEvent);
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu && serverPlayer.containerMenu != menuBefore) {
                return true;
            }
            if (!interactEvent.isCanceled()) {
                target.interact(serverPlayer, InteractionHand.MAIN_HAND);
                return serverPlayer.containerMenu != serverPlayer.inventoryMenu
                        && serverPlayer.containerMenu != menuBefore;
            }
        }
        return false;
    }

    @EventBusSubscriber(modid = MODID)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerNetwork(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            MultiWindowPayloads.register(registrar);
            registrar.playToServer(
                    OpenPacket.TYPE, OpenPacket.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> {
                        Player p = ctx.player();
                        if (p.isSpectator()) return;
                        ItemStack targetStack = ItemStack.EMPTY;

                        if (p.isCreative() && p.containerMenu == p.inventoryMenu) {
                            if (payload.clientStack().is(Items.ENDER_EYE)) {
                                targetStack = payload.clientStack();
                            }
                        } else if (payload.slotId() >= 0 && payload.slotId() < p.containerMenu.slots.size()) {
                            ItemStack menuStack = p.containerMenu.getSlot(payload.slotId()).getItem();
                            if (menuStack.is(Items.ENDER_EYE)) {
                                targetStack = menuStack;
                            }
                        }

                        // 仅处理已绑定的末影之眼，普通末影之眼右键拆分堆叠时直接忽略
                        if (!targetStack.isEmpty() && targetStack.has(REMOTE_DATA.get())) {
                            openRemote(p, targetStack);
                        }
                    })
            );
        }
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (!stack.is(Items.ENDER_EYE)) return;

            RemoteData data = stack.get(REMOTE_DATA.get());
            List<Component> tooltip = event.getToolTip();
            if (data != null) {
                tooltip.add(Component.translatableWithFallback(
                        "tooltip.examplemod.interface.connected",
                        "§7已连接: §e%s",
                        data.iconStack().getHoverName().getString()
                ));
                tooltip.add(Component.translatableWithFallback(
                        "tooltip.examplemod.interface.pos",
                        "§7坐标: §8%s",
                        data.pos().toShortString()
                ));
                if (data.entityUuid().isEmpty()) {
                    tooltip.add(Component.literal("§7自动归类: ").append(pickupModeLabel(data.pickupMode())));
                    tooltip.add(Component.literal("§8Shift+左键切换自动归类"));
                }
                tooltip.add(Component.translatableWithFallback(
                        "tooltip.examplemod.interface.gui_open",
                        "§b▶ 在GUI内右键打开"
                ));
            } else {
                tooltip.add(Component.translatableWithFallback(
                        "tooltip.examplemod.interface.bind_hint",
                        "§8Shift+右键方块或实体以绑定"
                ));
            }
        }
    }
}