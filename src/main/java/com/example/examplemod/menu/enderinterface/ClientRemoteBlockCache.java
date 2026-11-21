package com.example.examplemod.menu.enderinterface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public final class ClientRemoteBlockCache {
    public static final class ShadowEntry {
        public final ResourceKey<Level> dim;
        public final BlockPos pos;
        public final BlockState state;
        @Nullable public final BlockEntity blockEntity;

        public ShadowEntry(ResourceKey<Level> dim, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
            this.dim = dim;
            this.pos = pos;
            this.state = state;
            this.blockEntity = blockEntity;
        }
    }

    private static volatile boolean hasAny = false;
    private static Map<BlockPos, ShadowEntry> pendingShadows = Collections.emptyMap();
    private static final Map<Integer, Map<BlockPos, ShadowEntry>> WINDOW_SHADOWS = new LinkedHashMap<>();
    private static final Deque<Integer> CONTEXT_STACK = new ArrayDeque<>();
    private static boolean initializingShadows = false;

    private ClientRemoteBlockCache() {}

    public static boolean hasAny() {
        return hasAny;
    }

    private static void refreshFlag() {
        hasAny = !pendingShadows.isEmpty() || !WINDOW_SHADOWS.isEmpty();
    }

    public static void setPending(@Nullable RemoteBlockSnapshot snapshot) {
        if (snapshot == null || snapshot.entries().isEmpty()) {
            pendingShadows = Collections.emptyMap();
            refreshFlag();
            return;
        }
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            pendingShadows = Collections.emptyMap();
            refreshFlag();
            return;
        }

        Map<BlockPos, ShadowEntry> built = new HashMap<>();
        List<Runnable> initTasks = new ArrayList<>();

        for (RemoteBlockSnapshot.BlockEntry entry : snapshot.entries()) {
            BlockPos pos = entry.pos().immutable();
            BlockEntity be = null;
            if (entry.beTag().isPresent() && entry.state().getBlock() instanceof EntityBlock eb) {
                try {
                    be = eb.newBlockEntity(pos, entry.state());
                    if (be != null) {
                        be.setLevel(clientLevel);
                        BlockEntity finalBe = be;
                        CompoundTag tag = entry.beTag().get();
                        initTasks.add(() -> {
                            try {
                                if (!tag.isEmpty()) {
                                    finalBe.handleUpdateTag(tag, clientLevel.registryAccess());
                                }
                            } catch (Throwable t) {
                                EnderInterface.LOGGER.warn("Failed to apply shadow update tag at {}", pos, t);
                            }
                        });
                    }
                } catch (Throwable t) {
                    EnderInterface.LOGGER.warn("Failed to construct shadow BlockEntity at {}", pos, t);
                    be = null;
                }
            }
            built.put(pos, new ShadowEntry(snapshot.dim(), pos, entry.state(), be));
        }

        pendingShadows = built;
        refreshFlag();

        // 先放入 pendingShadows 再执行 handleUpdateTag，防止双箱在读数据时互查邻居方块失败
        initializingShadows = true;
        try {
            for (Runnable task : initTasks) {
                task.run();
            }
        } finally {
            initializingShadows = false;
        }
    }

    public static void bindToWindow(int containerId, int replaceOldId) {
        Map<BlockPos, ShadowEntry> oldMap = (replaceOldId != -1) ? WINDOW_SHADOWS.remove(replaceOldId) : null;
        if (!pendingShadows.isEmpty()) {
            WINDOW_SHADOWS.put(containerId, pendingShadows);
            pendingShadows = Collections.emptyMap();
        } else if (oldMap != null) {
            WINDOW_SHADOWS.put(containerId, oldMap);
        }
        refreshFlag();
    }

    public static void clearPending() {
        pendingShadows = Collections.emptyMap();
        refreshFlag();
    }

    public static void removeWindow(int containerId) {
        WINDOW_SHADOWS.remove(containerId);
        refreshFlag();
    }

    public static void clearAll() {
        pendingShadows = Collections.emptyMap();
        WINDOW_SHADOWS.clear();
        CONTEXT_STACK.clear();
        hasAny = false;
    }

    public static void pushContext(int containerId) {
        CONTEXT_STACK.push(containerId);
    }

    public static void popContext() {
        if (!CONTEXT_STACK.isEmpty()) {
            CONTEXT_STACK.pop();
        }
    }

    @Nullable
    private static ShadowEntry resolve(Level level, BlockPos pos) {
        if (!hasAny) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level) return null;

        boolean realChunkLoaded = level.getChunkSource().hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );

        // 1. 若当前正在执行某个远程窗口的 UI/初始化逻辑
        if (!CONTEXT_STACK.isEmpty()) {
            Map<BlockPos, ShadowEntry> winMap = WINDOW_SHADOWS.get(CONTEXT_STACK.peek());
            if (winMap != null) {
                ShadowEntry e = winMap.get(pos);
                if (e != null) {
                    if (level.dimension().equals(e.dim) && realChunkLoaded) return null;
                    return e;
                }
            }
        }

        // 2. 正在打开界面（MenuType.create / MenuScreens.create / handleUpdateTag 阶段）
        if (!pendingShadows.isEmpty()) {
            ShadowEntry e = pendingShadows.get(pos);
            if (e != null) {
                if (initializingShadows || !(level.dimension().equals(e.dim) && realChunkLoaded)) {
                    return e;
                }
            }
        }

        // 3. 非显式上下文（如外部模组发包更新等），仅在客户端该坐标区块确实未加载时才回退，绝不干扰本地已加载世界
        if (!realChunkLoaded) {
            for (Map<BlockPos, ShadowEntry> winMap : WINDOW_SHADOWS.values()) {
                ShadowEntry e = winMap.get(pos);
                if (e != null) {
                    return e;
                }
            }
        }
        return null;
    }

    public static boolean hasShadow(Level level, BlockPos pos) {
        return resolve(level, pos) != null;
    }

    @Nullable
    public static BlockState getShadowState(Level level, BlockPos pos) {
        ShadowEntry e = resolve(level, pos);
        return e != null ? e.state : null;
    }

    @Nullable
    public static BlockEntity getShadowBlockEntity(Level level, BlockPos pos, @Nullable BlockEntity realBe) {
        ShadowEntry e = resolve(level, pos);
        if (e == null || e.blockEntity == null) return null;
        if (realBe != null && level.dimension().equals(e.dim)) {
            return null;
        }
        if (e.blockEntity.getLevel() != level) {
            e.blockEntity.setLevel(level);
        }
        return e.blockEntity;
    }
}