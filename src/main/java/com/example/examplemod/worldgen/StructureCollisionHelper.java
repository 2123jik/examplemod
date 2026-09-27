package com.example.examplemod.worldgen;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.concurrent.TimeUnit;

public class StructureCollisionHelper {

    // 整合包黑名单 Tag：可在 data/examplemod/tags/worldgen/structure/blacklist.json 里配置排除结构
    public static final TagKey<Structure> BLACKLIST = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("examplemod", "blacklist")
    );

    // 缓存近期的结构包围盒，防止巨型建筑跨区块碰撞（防内存溢出，10分钟过期）
    private static final Cache<BlockPos, BoundingBox> RECENT_BOUNDS = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 解决“坑 2”：垂直高度平移，将整个结构无论是地下（Y<0）还是天空，强制对齐到地表 Y 坐标
     */
    public static void alignToSurface(StructureStart start, int targetSurfaceY) {
        if (!start.isValid()) return;

        BoundingBox box = start.getBoundingBox();
        int currentMinY = box.minY();
        int deltaY = targetSurfaceY - currentMinY;

        // 如果高度不在地表，整体升降所有零件
        if (deltaY != 0) {
            for (StructurePiece piece : start.getPieces()) {
                piece.move(0, deltaY, 0); // 官方映射中的零件平移方法
            }
        }
    }

    /**
     * 解决“坑 1”：包围盒碰撞排斥校验，防止超大结构与周边已有结构穿模
     * @param padding 建筑之间的安全间距（如 16 格）
     */
    public static boolean checkAndRegister(StructureStart start, int padding) {
        if (!start.isValid()) return false;

        BoundingBox box = start.getBoundingBox();
        // 加上安全缓冲区（外扩）
        BoundingBox paddedBox = new BoundingBox(
                box.minX() - padding, box.minY(), box.minZ() - padding,
                box.maxX() + padding, box.maxY(), box.maxZ() + padding
        );

        // 检查与已有结构是否相交
        for (BoundingBox existing : RECENT_BOUNDS.asMap().values()) {
            if (existing.intersects(paddedBox)) {
                return false; // 重叠，废弃此生成！
            }
        }

        BlockPos key = new BlockPos(box.minX(), box.minY(), box.minZ());
        RECENT_BOUNDS.put(key, paddedBox);
        return true;
    }
}