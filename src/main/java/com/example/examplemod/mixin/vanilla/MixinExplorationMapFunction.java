package com.example.examplemod.mixin.vanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExplorationMapFunction.class)
public class MixinExplorationMapFunction {

    @Shadow @Final private TagKey<Structure> destination;
    @Shadow @Final private Holder<MapDecorationType> mapDecoration;
    @Shadow @Final private byte zoom;
    @Shadow @Final private int searchRadius;
    @Shadow @Final private boolean skipKnownStructures;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void deferStructureSearch(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.is(Items.MAP)) {
            cir.setReturnValue(stack);
            return;
        }

        Vec3 vec3 = context.getParamOrNull(LootContextParams.ORIGIN);
        BlockPos originPos = vec3 != null ? BlockPos.containing(vec3) : BlockPos.ZERO;

        // 复制一份当前空地图
        ItemStack lazyMap = stack.copy();

        // 将定位所必需的参数全部写入 1.21 的 CUSTOM_DATA 组件中
        lazyMap.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putBoolean("is_lazy_exploration_map", true);
            tag.putString("dest_tag", this.destination.location().toString());
            tag.putString("deco_type", this.mapDecoration.unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("minecraft:target_x"));
            tag.putByte("zoom", this.zoom);
            tag.putInt("search_radius", this.searchRadius);
            tag.putBoolean("skip_known", this.skipKnownStructures);
            // 记录生成箱子的坐标，右键寻路时以箱子为中心找（和原版逻辑严格一致）
            tag.putInt("origin_x", originPos.getX());
            tag.putInt("origin_y", originPos.getY());
            tag.putInt("origin_z", originPos.getZ());
        }));

        // 贴心地改个名，提示玩家需要手动右键
        lazyMap.set(DataComponents.ITEM_NAME, Component.literal("§e未解密的探险地图 §7(右键展开)"));

        // 提前返回，彻底跳过 findNearestMapStructure
        cir.setReturnValue(lazyMap);
    }
}