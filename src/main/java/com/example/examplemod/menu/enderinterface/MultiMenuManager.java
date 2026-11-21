package com.example.examplemod.menu.enderinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiMenuManager {
    public static boolean isOpeningByInterface() {
        return Boolean.TRUE.equals(OPENING_BY_INTERFACE.get());
    }

    public static final class RemoteSession {
        private final ResourceKey<Level> dim;
        private BlockPos pos;
        @Nullable private final Block expectedBlock;
        @Nullable private final UUID entityUuid;
        private final AbstractContainerMenu menu;
        private final ItemStack iconStack;

        public RemoteSession(ResourceKey<Level> dim, BlockPos pos, @Nullable Block expectedBlock,
                             @Nullable UUID entityUuid, AbstractContainerMenu menu, ItemStack iconStack) {
            this.dim = dim;
            this.pos = pos.immutable();
            this.expectedBlock = expectedBlock;
            this.entityUuid = entityUuid;
            this.menu = menu;
            this.iconStack = iconStack;
        }

        public ResourceKey<Level> dim() { return dim; }
        public BlockPos pos() { return pos; }
        @Nullable public Block expectedBlock() { return expectedBlock; }
        @Nullable public UUID entityUuid() { return entityUuid; }
        public AbstractContainerMenu menu() { return menu; }
        public ItemStack iconStack() { return iconStack; }
    }

    private static final Map<UUID, UUID> VIRTUAL_ENTITY_USE = new ConcurrentHashMap<>();

    public static void beginVirtualEntityUse(ServerPlayer player, UUID entityUuid) {
        VIRTUAL_ENTITY_USE.put(player.getUUID(), entityUuid);
    }

    public static void endVirtualEntityUse(ServerPlayer player) {
        VIRTUAL_ENTITY_USE.remove(player.getUUID());
    }

    public static void register(ServerPlayer player, ResourceKey<Level> dim, BlockPos pos,
                                Block expectedBlock, AbstractContainerMenu menu, ItemStack iconStack) {
        SESSIONS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .put(menu.containerId, new RemoteSession(dim, pos, expectedBlock, null, menu, iconStack));
    }

    public static void registerEntity(ServerPlayer player, ResourceKey<Level> dim, BlockPos pos,
                                      UUID entityUuid, AbstractContainerMenu menu, ItemStack iconStack) {
        SESSIONS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .put(menu.containerId, new RemoteSession(dim, pos, null, entityUuid, menu, iconStack));
    }

    @Nullable
    public static RemoteSession findByEntity(ServerPlayer player, UUID entityUuid) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null) return null;
        for (RemoteSession s : map.values()) {
            if (entityUuid.equals(s.entityUuid())) {
                return s;
            }
        }
        return null;
    }

    private record ChunkKey(ResourceKey<Level> dim, ChunkPos chunkPos) {}

    private static final TicketType<ChunkPos> REMOTE_TICKET =
            TicketType.create(EnderInterface.MODID + ":remote_container", Comparator.comparingLong(ChunkPos::toLong));

    private static final Map<UUID, Map<Integer, RemoteSession>> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> VIRTUAL_USE = new ConcurrentHashMap<>();
    private static final Map<ChunkKey, Integer> CHUNK_TICKET_COUNTS = new ConcurrentHashMap<>();
    private static final Map<ChunkKey, Long> AUTO_PICKUP_LEASES = new ConcurrentHashMap<>();
    private static final long AUTO_PICKUP_LEASE_TICKS = 600L; // 30秒保活，兼顾性能与精妙存储熔炼/Tick升级

    private MultiMenuManager() {}

    // ===== 自动归类区块短租保活 =====
    public static void touchAutoPickupChunk(ServerLevel level, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkKey key = new ChunkKey(level.dimension(), cp);
        long expireTick = level.getServer().getTickCount() + AUTO_PICKUP_LEASE_TICKS;
        boolean[] newlyAdded = {false};
        AUTO_PICKUP_LEASES.compute(key, (k, prev) -> {
            if (prev == null) {
                newlyAdded[0] = true;
            }
            return expireTick;
        });
        if (newlyAdded[0]) {
            acquireChunk(level, pos);
        }
    }

    public static void tickAutoPickupLeases(MinecraftServer server) {
        if (AUTO_PICKUP_LEASES.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<Map.Entry<ChunkKey, Long>> it = AUTO_PICKUP_LEASES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkKey, Long> entry = it.next();
            if (now >= entry.getValue()) {
                it.remove();
                ChunkKey key = entry.getKey();
                releaseChunk(server, key.dim(), key.chunkPos().getWorldPosition());
            }
        }
    }

    // ===== 临时区块加载（引用计数，支持跨维度、双箱跨区块边界、多玩家共用） =====
    public static void acquireChunk(ServerLevel level, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkKey key = new ChunkKey(level.dimension(), cp);
        CHUNK_TICKET_COUNTS.compute(key, (k, count) -> {
            if (count == null || count <= 0) {
                // radius=2：当前区块处于实体Tick级，周围1圈也处于加载级（兼容跨区块大箱子）
                level.getChunkSource().addRegionTicket(REMOTE_TICKET, cp, 2, cp);
                return 1;
            }
            return count + 1;
        });
        // 同步确保目标区块加载进内存，紧接着读方块/开菜单才能拿到真实数据
        level.getChunk(cp.x, cp.z);
    }

    private static final ThreadLocal<Boolean> OPENING_BY_INTERFACE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<RemoteSession> REPLACING_SESSION = new ThreadLocal<>();

    public static void beginInterfaceOpen() { OPENING_BY_INTERFACE.set(true); }
    public static void endInterfaceOpen() { OPENING_BY_INTERFACE.set(false); }

    public static void onBeforeSubMenuOpen(ServerPlayer player) {
        if (Boolean.TRUE.equals(OPENING_BY_INTERFACE.get())) return;
        if (!isTracked(player, player.containerMenu)) return;

        RemoteSession old = getSession(player, player.containerMenu.containerId);
        if (old == null) return;

        REPLACING_SESSION.set(old);
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map != null) {
            map.remove(old.menu().containerId);
        }
        // 若鼠标抓着东西，先退回背包防止丢失
        ItemStack carried = old.menu().getCarried();
        if (!carried.isEmpty()) {
            old.menu().setCarried(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(carried);
        }
        try {
            old.menu().removed(player);
        } catch (Throwable ignored) {}
        // 暂置为 inventoryMenu，防止 ServerPlayer.openMenu 发原版关窗包把客户端窗口销毁
        player.containerMenu = player.inventoryMenu;
        ServerLevel targetLevel = player.server.getLevel(old.dim());
        java.util.Optional<RemoteBlockSnapshot> snapshot = (old.entityUuid() == null && targetLevel != null)
                ? RemoteBlockSnapshot.capture(targetLevel, old.pos())
                : java.util.Optional.empty();

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new MultiWindowPayloads.ReplaceRemotePayload(
                        old.menu().containerId,
                        new EnderInterface.RemoteData(
                                old.dim(), old.pos(), old.iconStack(),
                                java.util.Optional.ofNullable(old.entityUuid())
                        ),
                        snapshot
                )
        );
    }

    public static void onAfterSubMenuOpen(ServerPlayer player, boolean opened) {
        RemoteSession old = REPLACING_SESSION.get();
        REPLACING_SESSION.remove();
        if (old == null) return;

        if (opened && player.containerMenu != player.inventoryMenu) {
            if (old.entityUuid() != null) {
                registerEntity(player, old.dim(), old.pos(), old.entityUuid(), player.containerMenu, old.iconStack());
            } else {
                register(player, old.dim(), old.pos(), old.expectedBlock(), player.containerMenu, old.iconStack());
            }
        } else {
            releaseChunk(player.server, old.dim(), old.pos());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new MultiWindowPayloads.ClearPendingPayload());
        }
    }

    public static void releaseChunk(MinecraftServer server, ResourceKey<Level> dim, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkKey key = new ChunkKey(dim, cp);
        CHUNK_TICKET_COUNTS.computeIfPresent(key, (k, count) -> {
            if (count <= 1) {
                ServerLevel level = server.getLevel(dim);
                if (level != null) {
                    level.getChunkSource().removeRegionTicket(REMOTE_TICKET, cp, 2, cp);
                }
                return null;
            }
            return count - 1;
        });
    }

    public static boolean hasRemoteSessionAt(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer sp)) return false;
        BlockPos virtualPos = VIRTUAL_USE.get(player.getUUID());
        if (virtualPos != null && virtualPos.equals(pos)) {
            return true;
        }
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null || map.isEmpty()) return false;
        for (RemoteSession s : map.values()) {
            if (s.entityUuid() == null && s.pos().equals(pos) && isSessionTargetValid(sp, s)) {
                return true;
            }
        }
        return false;
    }

    // ===== 虚拟右键标记 =====
    public static void beginVirtualUse(ServerPlayer player, BlockPos pos) {
        VIRTUAL_USE.put(player.getUUID(), pos.immutable());
    }

    public static void endVirtualUse(ServerPlayer player) {
        VIRTUAL_USE.remove(player.getUUID());
    }

    @Nullable
    public static RemoteSession findByTarget(ServerPlayer player, ResourceKey<Level> dim, BlockPos pos) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null) return null;
        for (RemoteSession s : map.values()) {
            if (s.entityUuid() == null && s.dim().equals(dim) && s.pos().equals(pos)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static Map<Integer, RemoteSession> getSessions(Player player) {
        return SESSIONS.get(player.getUUID());
    }

    @Nullable
    public static RemoteSession getSession(Player player, int containerId) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        return map != null ? map.get(containerId) : null;
    }

    @Nullable
    public static AbstractContainerMenu getMenu(Player player, int containerId) {
        RemoteSession s = getSession(player, containerId);
        return s != null ? s.menu() : null;
    }

    public static boolean isTracked(Player player, @Nullable AbstractContainerMenu menu) {
        if (menu == null) return false;
        RemoteSession s = getSession(player, menu.containerId);
        return s != null && s.menu() == menu;
    }

    public static boolean hasAny(Player player) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        return map != null && !map.isEmpty();
    }

    public static boolean isRemoteMenuValid(AbstractContainerMenu menu, Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        RemoteSession s = getSession(player, menu.containerId);
        return s != null && s.menu() == menu && isSessionTargetValid(sp, s);
    }

    public static boolean isSessionTargetValid(ServerPlayer player, RemoteSession session) {
        ServerLevel level = player.server.getLevel(session.dim());
        if (level == null) return false;

        if (session.entityUuid() != null) {
            net.minecraft.world.entity.Entity entity = level.getEntity(session.entityUuid());
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                return false;
            }
            BlockPos newPos = entity.blockPosition();
            ChunkPos oldChunk = new ChunkPos(session.pos());
            ChunkPos newChunk = new ChunkPos(newPos);
            if (!oldChunk.equals(newChunk)) {
                acquireChunk(level, newPos);
                releaseChunk(player.server, session.dim(), session.pos());
            }
            session.pos = newPos;
            EntityTrackerData.get(level).update(entity);
            return true;
        }

        if (!level.isLoaded(session.pos())) return false;
        return session.expectedBlock() != null && level.getBlockState(session.pos()).is(session.expectedBlock());
    }

    public static boolean hasRemoteEntityNear(Player player, net.minecraft.world.phys.AABB aabb) {
        if (!(player instanceof ServerPlayer sp)) return false;
        UUID virtualUuid = VIRTUAL_ENTITY_USE.get(player.getUUID());
        if (virtualUuid != null) {
            for (ServerLevel lvl : sp.server.getAllLevels()) {
                var e = lvl.getEntity(virtualUuid);
                if (e != null && e.getBoundingBox().intersects(aabb.inflate(0.5))) {
                    return true;
                }
            }
        }
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null || map.isEmpty()) return false;
        for (RemoteSession s : map.values()) {
            if (s.entityUuid() != null) {
                ServerLevel lvl = sp.server.getLevel(s.dim());
                var e = (lvl != null) ? lvl.getEntity(s.entityUuid()) : null;
                if (e != null && e.isAlive() && e.getBoundingBox().intersects(aabb.inflate(0.5))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSelfPointingInterface(ItemStack stack, @Nullable RemoteSession session) {
        if (session == null || stack.isEmpty() || !stack.is(EnderInterface.INTERFACE_ITEM.get())) {
            return false;
        }
        EnderInterface.RemoteData data = stack.get(EnderInterface.REMOTE_DATA.get());
        if (data == null) return false;
        if (session.entityUuid() != null) {
            return data.entityUuid().isPresent() && data.entityUuid().get().equals(session.entityUuid());
        }
        return data.entityUuid().isEmpty() && data.dim().equals(session.dim()) && data.pos().equals(session.pos());
    }

    // ===== Tick 同步 & 失效自动关闭 =====
    public static void tickPlayer(ServerPlayer player) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null || map.isEmpty()) return;

        Iterator<Map.Entry<Integer, RemoteSession>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            RemoteSession s = it.next().getValue();
            AbstractContainerMenu m = s.menu();
            if (!isSessionTargetValid(player, s) || !m.stillValid(player)) {
                it.remove();
                boolean lastOne = map.isEmpty();
                if (!lastOne) {
                    m.setCarried(ItemStack.EMPTY);
                } else {
                    player.inventoryMenu.setCarried(ItemStack.EMPTY);
                }
                try {
                    m.removed(player);
                } catch (Throwable ignored) {}
                if (player.containerMenu == m) {
                    player.containerMenu = lastOne ? player.inventoryMenu : map.values().iterator().next().menu();
                }
                releaseChunk(player.server, s.dim(), s.pos());
                L2MenuStackerCompat.clearServerTrackerIfWid(player, lastOne ? -1 : m.containerId);
                player.connection.send(new ClientboundContainerClosePacket(m.containerId));
                continue;
            }
            if (m != player.containerMenu) {
                m.broadcastChanges();
            }
        }
        if (map.isEmpty()) {
            SESSIONS.remove(player.getUUID());
        }
    }

    // ===== 单窗关闭 (×按钮) =====
    public static void closeOne(ServerPlayer player, int containerId) {
        Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
        if (map == null) return;
        RemoteSession s = map.remove(containerId);
        if (s != null) {
            boolean lastOne = map.isEmpty();
            if (!lastOne) {
                s.menu().setCarried(ItemStack.EMPTY);
            } else {
                player.inventoryMenu.setCarried(ItemStack.EMPTY);
            }
            try {
                s.menu().removed(player);
            } catch (Throwable ignored) {}
            if (player.containerMenu == s.menu()) {
                player.containerMenu = lastOne
                        ? player.inventoryMenu
                        : map.values().iterator().next().menu();
            }
            L2MenuStackerCompat.clearServerTrackerIfWid(player, lastOne ? -1 : containerId);
            releaseChunk(player.server, s.dim(), s.pos());
        }
        if (map.isEmpty()) {
            SESSIONS.remove(player.getUUID());
        }
    }

    // ===== 按 E 最小化收起 =====
    public static void parkActive(ServerPlayer player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            ItemStack toReturn = carried.copy();
            Map<Integer, RemoteSession> map = SESSIONS.get(player.getUUID());
            if (map != null) {
                for (RemoteSession s : map.values()) {
                    s.menu().setCarried(ItemStack.EMPTY);
                }
            }
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(toReturn);
            player.connection.send(new ClientboundContainerSetSlotPacket(-1, player.inventoryMenu.incrementStateId(), -1, ItemStack.EMPTY));
        }
        if (isTracked(player, player.containerMenu)) {
            player.containerMenu = player.inventoryMenu;
        }
        L2MenuStackerCompat.clearServerTrackerIfWid(player, -1);
    }

    public static void focus(ServerPlayer player, int containerId) {
        AbstractContainerMenu m = getMenu(player, containerId);
        if (m != null) {
            player.containerMenu = m;
        }
    }

    // ===== 全关 / 死亡 / 掉线 =====
    public static void closeAll(ServerPlayer player) {
        L2MenuStackerCompat.clearServerTrackerIfWid(player, -1);
        VIRTUAL_ENTITY_USE.remove(player.getUUID());
        VIRTUAL_USE.remove(player.getUUID());
        Map<Integer, RemoteSession> map = SESSIONS.remove(player.getUUID());
        if (map == null || map.isEmpty()) return;

        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        boolean carriedHandled = false;
        for (RemoteSession s : map.values()) {
            AbstractContainerMenu m = s.menu();
            if (carriedHandled) {
                m.setCarried(ItemStack.EMPTY);
            } else if (!m.getCarried().isEmpty()) {
                carriedHandled = true;
            }
            try {
                m.removed(player);
            } catch (Throwable ignored) {}
            if (player.containerMenu == m) {
                player.containerMenu = player.inventoryMenu;
            }
            releaseChunk(player.server, s.dim(), s.pos());
        }

        player.connection.send(new ClientboundContainerSetSlotPacket(-1, player.inventoryMenu.incrementStateId(), -1, ItemStack.EMPTY));
    }

    public static void clearAllOnServerStop() {
        VIRTUAL_ENTITY_USE.clear();
        SESSIONS.clear();
        VIRTUAL_USE.clear();
        CHUNK_TICKET_COUNTS.clear();
        AUTO_PICKUP_LEASES.clear();
    }
}
