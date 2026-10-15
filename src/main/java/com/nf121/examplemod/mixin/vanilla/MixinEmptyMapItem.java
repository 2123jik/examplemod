package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EmptyMapItem.class)
public class MixinEmptyMapItem {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onLazyMapUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack heldStack = player.getItemInHand(hand);
        CustomData customData = heldStack.get(DataComponents.CUSTOM_DATA);

        // 如果不是我们延迟生成的地图，放行走原版的空地图逻辑
        if (customData == null || !customData.contains("is_lazy_exploration_map")) {
            return;
        }

        if (level.isClientSide) {
            cir.setReturnValue(InteractionResultHolder.success(heldStack));
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            CompoundTag tag = customData.copyTag();

            // 1. 读取参数
            TagKey<Structure> destTag = TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(tag.getString("dest_tag")));
            ResourceLocation decoLoc = ResourceLocation.parse(tag.getString("deco_type"));
            Holder<MapDecorationType> decoHolder = serverLevel.registryAccess()
                    .lookupOrThrow(Registries.MAP_DECORATION_TYPE)
                    .getOrThrow(ResourceKey.create(Registries.MAP_DECORATION_TYPE, decoLoc));

            byte zoom = tag.getByte("zoom");
            int searchRadius = tag.getInt("search_radius");
            boolean skipKnown = tag.getBoolean("skip_known");
            
            // 优先以箱子坐标为中心寻路；如果箱子坐标是 (0,0,0)，则以玩家位置为中心
            BlockPos searchOrigin = new BlockPos(tag.getInt("origin_x"), tag.getInt("origin_y"), tag.getInt("origin_z"));
            if (searchOrigin.equals(BlockPos.ZERO)) {
                searchOrigin = player.blockPosition();
            }

            // 给玩家动作栏提示，玩家自己点的右键，有明确心理预期
            player.displayClientMessage(Component.literal("§7正在勘测附近结构..."), true);

            // 2. 执行定位（此时玩家在主动探索，即使耗时零点几秒也不会觉得突兀）
            BlockPos targetPos = serverLevel.findNearestMapStructure(destTag, searchOrigin, searchRadius, skipKnown);

            if (targetPos != null) {
                // 生成正式的已填充地图 (filled_map)
                ItemStack filledMap = MapItem.create(serverLevel, targetPos.getX(), targetPos.getZ(), zoom, true, true);
                
                // 跳过耗时的 MapItem.renderBiomePreviewMap(serverLevel, filledMap); （省去 128*128 群系扫描）
                MapItemSavedData.addTargetDecoration(filledMap, targetPos, "+", decoHolder);

                // 播放音效与统计
                level.playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, player.getSoundSource(), 1.0F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get((EmptyMapItem)(Object)this));

                // 消耗 1 个手持物品，给予生成后的地图（严格按照 EmptyMapItem 的原生逻辑处理多堆叠）
                heldStack.consume(1, player);
                if (heldStack.isEmpty()) {
                    cir.setReturnValue(InteractionResultHolder.consume(filledMap));
                } else {
                    if (!player.getInventory().add(filledMap.copy())) {
                        player.drop(filledMap, false);
                    }
                    cir.setReturnValue(InteractionResultHolder.consume(heldStack));
                }
            } else {
                player.displayClientMessage(Component.literal("§c在附近未能找到该结构！"), true);
                cir.setReturnValue(InteractionResultHolder.fail(heldStack));
            }
        }
    }
}