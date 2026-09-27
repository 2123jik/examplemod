package com.example.examplemod.mixin.villna;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(Structure.class)
public class StructureBiomeMixin {

    /**
     * 只对这些结构放开群系限制。
     * 用"命名空间:路径"表示，路径就是该结构的注册名。
     *
     * 想加什么，先在游戏里用 /place structure <id> 或
     * 查数据包的 worldgen/structure/*.json 文件名确认 ID。
     */
    private static final Set<ResourceLocation> BIOME_EXPANSION_WHITELIST = Set.of(
        // ---- 原版通用结构：本来就在很多群系生成，放开后覆盖更全 ----
        ResourceLocation.withDefaultNamespace("village_plains"),
        ResourceLocation.withDefaultNamespace("village_desert"),
        ResourceLocation.withDefaultNamespace("village_savanna"),
        ResourceLocation.withDefaultNamespace("village_snowy"),
        ResourceLocation.withDefaultNamespace("village_taiga"),
        ResourceLocation.withDefaultNamespace("pillager_outpost"),
        ResourceLocation.withDefaultNamespace("mineshaft"),
        ResourceLocation.withDefaultNamespace("mineshaft_mesa"),
        ResourceLocation.withDefaultNamespace("ruined_portal"),
        ResourceLocation.withDefaultNamespace("ruined_portal_desert"),
        ResourceLocation.withDefaultNamespace("ruined_portal_jungle"),
        ResourceLocation.withDefaultNamespace("ruined_portal_mountain"),
        ResourceLocation.withDefaultNamespace("ruined_portal_ocean"),
        ResourceLocation.withDefaultNamespace("ruined_portal_swamp")

        // ---- 结构 mod 里"本意就是到处刷"的通用结构，按需加 ----
        // When Dungeons Arise 里大多是主题结构，不建议加。以下仅为格式示例：
        // ResourceLocation.fromNamespaceAndPath("when_dungeons_arise", "abandoned_temple"),
        // ResourceLocation.fromNamespaceAndPath("golem_dungeons", "small_golem_dungeon")

        // ---- 绝对不要加的：加了就毁设计 ----
        // minecraft:desert_pyramid     沙漠金字塔，就该在沙漠
        // minecraft:jungle_temple      丛林神庙
        // minecraft:witch_hut          女巫小屋
        // minecraft:ocean_monument     海底神殿
        // minecraft:igloo              雪屋
        // minecraft:nether_fortress    下界要塞
        // minecraft:bastion_remnant    堡垒遗迹
        // minecraft:end_city           末地城
        // twilightforest:*             暮色森林，别碰
        // incendium:*                  下界主题，别碰
        // betterend:* / betternether:*  别碰
    );

    /**
     * 拦截 Structure#findValidGenerationPoint 里的 Optional.filter(...)
     * 调用。如果当前结构在白名单里，则跳过群系过滤，直接返回不过滤的 Optional。
     */
    @Redirect(
        method = "findValidGenerationPoint",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;filter(Ljava/util/function/Predicate;)Ljava/util/Optional;"
        )
    )
    private Optional<Structure.GenerationStub> examplemod$expandBiomes(
            Optional<Structure.GenerationStub> original,
            Predicate<Structure.GenerationStub> originalFilter,
            Structure.GenerationContext context) {

        Structure self = (Structure)(Object)this;
        ResourceLocation id = context.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getKey(self);

        if (id != null && BIOME_EXPANSION_WHITELIST.contains(id)) {
            return original; // 不过滤 → 任何群系都允许
        }
        return original.filter(originalFilter);
    }
}