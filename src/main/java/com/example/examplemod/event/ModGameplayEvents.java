package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

import static com.example.examplemod.util.CurioMimicHelper.*;

//@EventBusSubscriber(modid = ExampleMod.MODID)
public class ModGameplayEvents {
    private static final String PEARL_MARKER = "examplemod_random_dimension_pearl";
    private static final String RECEIVED_MARKER = "examplemod_received_random_dimension_pearl";

//    @SubscribeEvent
//    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
//        if (!(event.getEntity() instanceof ServerPlayer player)) {
//            return;
//        }
//
//        CompoundTag playerData = player.getPersistentData();
//        if (playerData.getBoolean(RECEIVED_MARKER)) {
//            return;
//        }
//
//        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
//        CompoundTag pearlData = new CompoundTag();
//        pearlData.putBoolean(PEARL_MARKER, true);
//        pearl.set(DataComponents.CUSTOM_DATA, CustomData.of(pearlData));
//
//        pearl.set(
//                DataComponents.CUSTOM_NAME,
//                Component.literal("随机维度珍珠").withColor(0xB56CFF)
//        );
//
//        if (!player.getInventory().add(pearl)) {
//            player.drop(pearl, false);
//        }
//
//        playerData.putBoolean(RECEIVED_MARKER, true);
//    }
//
//    @SubscribeEvent
//    public static void onItemTooltip(ItemTooltipEvent event) {
//        if (!isRandomDimensionPearl(event.getItemStack())) {
//            return;
//        }
//
//        event.getToolTip().add(
//                Component.literal("右键使用后传送到一个随机维度").withColor(0xA970FF)
//        );
//        event.getToolTip().add(
//                Component.literal("目的地：该维度的世界出生点").withColor(0x808080)
//        );
//    }
//
//    @SubscribeEvent
//    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
//        ItemStack pearl = event.getItemStack();
//        if (!isRandomDimensionPearl(pearl)) {
//            return;
//        }
//
//        event.setCanceled(true);
//        event.setCancellationResult(InteractionResult.SUCCESS);
//
//        if (!(event.getEntity() instanceof ServerPlayer player)) {
//            return;
//        }
//
//        MinecraftServer server = player.getServer();
//        if (server == null) return;
//
//        List<ServerLevel> destinations = new ArrayList<>();
//        for (ServerLevel level : server.getAllLevels()) {
//            if (level != player.serverLevel()) {
//                destinations.add(level);
//            }
//        }
//
//        if (destinations.isEmpty()) {
//            player.displayClientMessage(
//                    Component.literal("没有可传送的其他维度！").withColor(0xFF5555),
//                    true
//            );
//            return;
//        }
//
//        RandomSource random = player.getRandom();
//        ServerLevel destination = destinations.get(random.nextInt(destinations.size()));
//
//        BlockPos spawn = destination.getSharedSpawnPos();
//        BlockPos surface = destination.getHeightmapPos(
//                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
//                spawn
//        );
//
//        player.teleportTo(
//                destination,
//                surface.getX() + 0.5D,
//                surface.getY() + 1.0D,
//                surface.getZ() + 0.5D,
//                player.getYRot(),
//                player.getXRot()
//        );
//
//        if (!player.isCreative()) {
//            pearl.shrink(1);
//        }
//
//        player.getCooldowns().addCooldown(Items.ENDER_PEARL, 20);
//        player.displayClientMessage(
//                Component.literal("已传送至维度：")
//                        .append(Component.literal(destination.dimension().location().toString()))
//                        .withColor(0x55FFFF),
//                false
//        );
//    }
//
//    private static boolean isRandomDimensionPearl(ItemStack stack) {
//        if (!stack.is(Items.ENDER_PEARL)) {
//            return false;
//        }
//
//        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
//        return customData != null && customData.copyTag().getBoolean(PEARL_MARKER);
//    }


}
