package com.example.examplemod.registry;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class DataRegistryHandler {

    private static final Consumer<DataComponentPatch.Builder> SET_MAX_STACK_64 =
            builder -> builder.set(DataComponents.MAX_STACK_SIZE, 64);

    private static final List<Class<? extends Item>> STACKABLE_TYPES = List.of(
            SaddleItem.class, MinecartItem.class, BoatItem.class, SignItem.class, HangingSignItem.class,
            BucketItem.class, SnowballItem.class, SolidBucketItem.class, MilkBucketItem.class, MobBucketItem.class,
            EggItem.class, BundleItem.class, SpyglassItem.class, BedItem.class, EnderpearlItem.class,
            PotionItem.class, SplashPotionItem.class, LingeringPotionItem.class, EnchantedBookItem.class,
            ArmorStandItem.class, BannerItem.class, BannerPatternItem.class, InstrumentItem.class, HoneyBottleItem.class
    );

    private static final Set<Item> SPECIFIC_ITEMS = Set.of(
            Items.WRITABLE_BOOK, Items.MUSHROOM_STEW, Items.CAKE,
            Items.RABBIT_STEW, Items.BEETROOT_SOUP, Items.KNOWLEDGE_BOOK
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {

        // 1. 指定的单个物品
        for (Item item : SPECIFIC_ITEMS) {
            // 同样确保没有被其他奇葩模组魔改出耐久
            if (!item.components().has(DataComponents.MAX_DAMAGE)) {
                event.modify(item, SET_MAX_STACK_64);
            }
        }

        // 2. 批量匹配
        event.modifyMatching(item -> {
            var components = item.components();

            // 核心熔断：原版底层铁律——凡是带耐久度的物品，绝不能堆叠，直接跳过！
            if (components.has(DataComponents.MAX_DAMAGE)) {
                return false;
            }

            // A. 潜影盒
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                return true;
            }

            // B. 类型树匹配
            for (Class<? extends Item> type : STACKABLE_TYPES) {
                if (type.isInstance(item)) {
                    return true;
                }
            }

            // C. 唱片检测（避开单例污染）
            if (components.has(DataComponents.JUKEBOX_PLAYABLE)
                    && components.getOrDefault(DataComponents.RARITY, Rarity.COMMON) == Rarity.RARE) {
                return true;
            }

            return false;
        }, SET_MAX_STACK_64);
    }
}