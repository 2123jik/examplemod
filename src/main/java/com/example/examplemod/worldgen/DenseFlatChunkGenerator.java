package com.example.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

public class DenseFlatChunkGenerator extends FlatLevelSource {

    public static final MapCodec<DenseFlatChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(DenseFlatChunkGenerator::settings),
                    Codec.INT.optionalFieldOf("grid_size_chunks", 8).forGetter(g -> g.gridSizeChunks),
                    Codec.INT.optionalFieldOf("surface_y", 64).forGetter(g -> g.surfaceY),
                    Codec.INT.optionalFieldOf("padding_blocks", 16).forGetter(g -> g.paddingBlocks)
            ).apply(instance, DenseFlatChunkGenerator::new)
    );

    private final int gridSizeChunks;
    private final int surfaceY;
    private final int paddingBlocks;

    public DenseFlatChunkGenerator(FlatLevelGeneratorSettings settings,
                                   int gridSizeChunks,
                                   int surfaceY,
                                   int paddingBlocks) {
        super(settings);
        this.gridSizeChunks = gridSizeChunks;
        this.surfaceY = surfaceY;
        this.paddingBlocks = paddingBlocks;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void createStructures(RegistryAccess registryAccess,
                                 ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager,
                                 ChunkAccess chunk,
                                 StructureTemplateManager templateManager) {
        ChunkPos chunkPos = chunk.getPos();

        // 1. 出生点保护区：以原点为中心 5x5 区块不生成任何结构
        if (Math.abs(chunkPos.x) <= 2 && Math.abs(chunkPos.z) <= 2) {
            return;
        }

        // 2. 只在网格锚点生成（gridSizeChunks 的整数倍区块）
        if (Math.floorMod(chunkPos.x, this.gridSizeChunks) != 0
                || Math.floorMod(chunkPos.z, this.gridSizeChunks) != 0) {
            return;
        }

        // 3. 收集所有可用结构（黑名单过滤）
        Registry<Structure> structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
        List<Holder.Reference<Structure>> available = structureRegistry.holders()
                .filter(holder -> !holder.is(StructureCollisionHelper.BLACKLIST))
                .toList();
        if (available.isEmpty()) {
            return;
        }

        // 4. 确定性随机：同种子 + 同区块 → 永远选中同一结构
        RandomSource random = RandomSource.create(chunkPos.toLong() ^ structureState.getLevelSeed());
        Holder.Reference<Structure> chosenHolder = available.get(random.nextInt(available.size()));
        Structure structure = chosenHolder.value();

        // 5. 生成结构（第三个参数是 biome 允许性谓词，这里恒 true，即不受群系限制）
        RandomState randomState = structureState.randomState();
        StructureStart start = structure.generate(
                registryAccess,
                this,
                this.biomeSource,
                randomState,
                templateManager,
                structureState.getLevelSeed(),
                chunkPos,
                0,
                chunk,
                holder -> true
        );

        if (!start.isValid()) {
            return;
        }

        // 6. 对齐地表 Y，做包围盒碰撞检测
        StructureCollisionHelper.alignToSurface(start, this.surfaceY);
        if (!StructureCollisionHelper.checkAndRegister(start, this.paddingBlocks)) {
            return;
        }

        // 7. 登记 StructureStart —— 这是唯一必要的写入操作。
        //    用 chunk.setStartForStructure 而不是 structureManager 的版本，
        //    因为我们手里就是当前区块，避免多绕一层。
        chunk.setStartForStructure(structure, start);

        // 8. 为本区块写一条“引用自己”的记录。
        //    这是给 StructureManager.startsForStructure(chunkPos, ...) 用的，
        //    确保之后 getStructureAt / getStructureWithPieceAt 能查到它。
        chunk.addReferenceForStructure(structure, chunkPos.toLong());

        // 注意：不要再遍历包围盒覆盖的其它区块去“手动添加引用”。
        //      1) createStructures 里只能拿到当前 ChunkAccess，强取其它区块会触发递归/死锁。
        //      2) 原版 ChunkStatus.STRUCTURE_REFERENCES 阶段会自动扫描相邻区块的
        //         STRUCTURE_STARTS，把结构包围盒覆盖到的区块全部补上引用。
        //         条件只有一个：StructureStart 已经用 setStartForStructure 登记好了——上面已做。
    }
}