package com.example.examplemod.client.tooltip;

import com.example.examplemod.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.LockCode;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BuiltinTooltipProviders {
    private BuiltinTooltipProviders() {}
    private static final List<String> ENTITY_KEYS =
            List.of("entity", "Entity", "CapturedEntity", "mob", "captured_entity");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final class AdventureAccess {
        private static final Field FIELD = findField();
        private static volatile boolean readFailed;

        private static Field findField() {
            try {
                for (Field field : AdventureModePredicate.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())
                            || !List.class.isAssignableFrom(field.getType())) continue;

                    if (field.getGenericType() instanceof ParameterizedType type
                            && type.getActualTypeArguments().length == 1
                            && type.getActualTypeArguments()[0] == BlockPredicate.class
                            && field.trySetAccessible()) {
                        return field;
                    }
                }
                LOGGER.warn("无法访问冒险模式方块谓词，将使用文字提示");
            } catch (RuntimeException exception) {
                LOGGER.warn("初始化冒险模式方块谓词访问失败", exception);
            }
            return null;
        }
    }

    public static final List<TooltipContributors.Provider> ALL = List.of(
            BuiltinTooltipProviders::appendBees,
            BuiltinTooltipProviders::appendEntities,
            BuiltinTooltipProviders::appendAdventureRestrictions,
            BuiltinTooltipProviders::appendDurability,
            BuiltinTooltipProviders::appendFood,
            BuiltinTooltipProviders::appendJukebox,
            BuiltinTooltipProviders::appendEnchantments,
            BuiltinTooltipProviders::appendSuspiciousStew,
            BuiltinTooltipProviders::appendChargedProjectiles,
            BuiltinTooltipProviders::appendContainers,
            BuiltinTooltipProviders::appendPotions,
            BuiltinTooltipProviders::appendFuel,
            BuiltinTooltipProviders::appendToolSpeed,
            BuiltinTooltipProviders::appendCooldown,
            BuiltinTooltipProviders::appendDecorations,
            BuiltinTooltipProviders::appendOminousBottle,
            BuiltinTooltipProviders::appendCompost,
            BuiltinTooltipProviders::appendDyeColor,
            BuiltinTooltipProviders::appendLodestone,
            BuiltinTooltipProviders::appendBookContent,
            BuiltinTooltipProviders::appendInstrument,
            BuiltinTooltipProviders::appendProfile,
            BuiltinTooltipProviders::appendSpecialAttributes,
            BuiltinTooltipProviders::appendMisc
    );



    private static void appendBees(TooltipContext ctx, List<TooltipContribution> sink) {
        List<Occupant> bees = ctx.stack().get(DataComponents.BEES);
        if (bees != null && !bees.isEmpty()) {
            List<VisualComponents.EntityEntry> entries = new ArrayList<>(bees.size());
            for (Occupant occupant : bees) {
                CompoundTag tag = occupant.entityData().copyTag();
                String entityId = tag.contains("id") ? tag.getString("id") : "minecraft:bee";
                entries.add(new VisualComponents.EntityEntry(entityId, occupant.ticksInHive(), occupant.minTicksInHive()));
            }
            sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "bees_dynamic",
                    new VisualComponents.DynamicEntityPreview(entries, "🐝 居住蜜蜂", new ItemStack(Items.HONEYCOMB))));
        }
    }

    private static void appendEntities(TooltipContext ctx, List<TooltipContribution> sink) {
        ItemStack stack = ctx.stack();

        CustomData bucketData = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (bucketData != null) {
            String id = extractEntityId(bucketData.copyTag());
            if (id != null) {
                EntityType<?> type = resolveLivingEntity(id);
                if (type != null) {
                    sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "bucket_entity",
                            new VisualComponents.DynamicEntityPreview(
                                    List.of(new VisualComponents.EntityEntry(id, 0, 0)),
                                    "🐟 桶装生物",
                                    new ItemStack(Items.WATER_BUCKET)
                            )));
                    return;
                }
            }
        }

        if (!isEntityPlacerItem(stack)) {
            String capturedId = null;

            CustomData entityData = stack.get(DataComponents.ENTITY_DATA);
            if (entityData != null) {
                capturedId = extractEntityId(entityData.copyTag());
            }

            if (capturedId == null) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    capturedId = extractEntityIdFromCustomData(customData.copyTag());
                }
            }

            if (capturedId != null) {
                EntityType<?> type = resolveLivingEntity(capturedId);
                if (type != null) {
                    sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "captured_entity",
                            new VisualComponents.DynamicEntityPreview(
                                    List.of(new VisualComponents.EntityEntry(capturedId, 0, 0)),
                                    "📦 捕获生物",
                                    new ItemStack(Items.LEAD)
                            )));
                    return;
                }
            }
        }

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag tag = blockEntityData.copyTag();
            if (tag.contains("SpawnData", CompoundTag.TAG_COMPOUND)) {
                CompoundTag spawnData = tag.getCompound("SpawnData");
                if (spawnData.contains("entity", CompoundTag.TAG_COMPOUND)) {
                    String spawnerMob = spawnData.getCompound("entity").getString("id");
                    if (!spawnerMob.isEmpty() && isRegisteredEntity(spawnerMob)) {
                        sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "spawner_entity",
                                new VisualComponents.DynamicEntityPreview(
                                        List.of(new VisualComponents.EntityEntry(spawnerMob, 0, 0)),
                                        "🔥 刷怪笼核心生物",
                                        new ItemStack(Items.SPAWNER)
                                )));
                    }
                }
            }
        }
    }

    private static boolean isEntityPlacerItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SpawnEggItem) return true;
        if (item instanceof HangingEntityItem) return true;
        if (item instanceof ArmorStandItem) return true;
        if (item instanceof BoatItem || item instanceof MinecartItem) return true;
        if (item instanceof EndCrystalItem) return true;
        return stack.is(Items.PAINTING) || stack.is(Items.ITEM_FRAME) || stack.is(Items.GLOW_ITEM_FRAME) || stack.is(Items.ARMOR_STAND);
    }

    private static EntityType<?> resolveLivingEntity(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) return null;

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) return null;

        if (!LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
            return null;
        }
        if (type == EntityType.ARMOR_STAND) {
            return null;
        }

        return type;
    }

    private static boolean isRegisteredEntity(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

    private static String extractEntityId(CompoundTag tag) {
        if (tag == null || !tag.contains("id", CompoundTag.TAG_STRING)) return null;
        String id = tag.getString("id");
        return id.isBlank() ? null : id;
    }


    private static String extractEntityIdFromCustomData(CompoundTag tag) {
        if (tag == null) return null;

        String direct = extractEntityId(tag);
        if (direct != null) return direct;

        for (String key : ENTITY_KEYS) {
            String id = null;
            if (tag.contains(key, CompoundTag.TAG_COMPOUND)) {
                id = extractEntityId(tag.getCompound(key));
            } else if (tag.contains(key, CompoundTag.TAG_STRING)) {
                id = tag.getString(key);
            }

            if (id != null && !id.isBlank()) return id;
        }

        String fallback = tag.getString("entity_id");
        return fallback.isBlank() ? null : fallback;
    }

    private static void appendAdventureRestrictions(
            TooltipContext ctx, List<TooltipContribution> sink
    ) {
        appendAdventureRestriction(
                ctx.stack().get(DataComponents.CAN_BREAK),
                sink, 10, "can_break",
                "⚒ 冒险模式可破坏:",
                "⚒ 包含冒险模式破坏限制",
                ChatFormatting.RED, 0xFFFF5555
        );
        appendAdventureRestriction(
                ctx.stack().get(DataComponents.CAN_PLACE_ON),
                sink, 11, "can_place",
                "🧱 冒险模式可放置于:",
                "🧱 包含冒险模式放置限制",
                ChatFormatting.GREEN, 0xFF55FF55
        );
    }

    private static void appendAdventureRestriction(
            AdventureModePredicate predicate,
            List<TooltipContribution> sink,
            int priority,
            String key,
            String title,
            String fallback,
            ChatFormatting formatting,
            int color
    ) {
        if (predicate == null || !predicate.showInTooltip()) return;

        List<ItemStack> blocks = resolveAdventureBlocks(predicate);
        sink.add(blocks.isEmpty()
                ? TooltipContribution.text(
                TooltipCategory.TOOL, priority, key,
                Component.literal(fallback).withStyle(formatting)
        )
                : TooltipContribution.visual(
                TooltipCategory.TOOL, priority, key + "_grid",
                new VisualComponents.BlockGridPreview(blocks, title, color)
        ));
    }

    private static List<ItemStack> resolveAdventureBlocks(
            AdventureModePredicate predicate
    ) {
        if (AdventureAccess.FIELD == null || AdventureAccess.readFailed) return List.of();

        try {
            Object value = AdventureAccess.FIELD.get(predicate);
            if (!(value instanceof List<?> predicates)) return List.of();

            Set<Item> items = new LinkedHashSet<>();
            for (Object candidate : predicates) {
                if (!(candidate instanceof BlockPredicate blockPredicate)) continue;

                blockPredicate.blocks().ifPresent(blocks -> {
                    for (Holder<Block> holder : blocks) {
                        Item item = holder.value().asItem();
                        if (item != Items.AIR) items.add(item);
                    }
                });
            }

            return items.stream().map(ItemStack::new).toList();
        } catch (IllegalAccessException | RuntimeException exception) {
            AdventureAccess.readFailed = true;
            LOGGER.warn("读取冒险模式方块谓词失败，将使用文字提示", exception);
            return List.of();
        }
    }



    private static void appendDurability(TooltipContext ctx, List<TooltipContribution> sink) {
        ItemStack stack = ctx.stack();
        if (Config.SHOW_DURABILITY.get() && stack.isDamageableItem()) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            sink.add(TooltipContribution.visual(TooltipCategory.DURABILITY, 0, "durability_bar",
                    new VisualComponents.DurabilityBar(remaining, stack.getMaxDamage())));
        }
    }

    private static void appendFood(TooltipContext ctx, List<TooltipContribution> sink) {
        if (!Config.SHOW_FOOD.get()) return;
        FoodProperties food = ctx.stack().get(DataComponents.FOOD);
        if (food != null) {
            sink.add(TooltipContribution.visual(TooltipCategory.FOOD, 0, "food_visual", new VisualComponents.FoodComponent(food)));
        }
    }

    private static void appendJukebox(TooltipContext ctx, List<TooltipContribution> sink) {
        JukeboxPlayable jukebox = ctx.stack().get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukebox != null) {
            HolderLookup.Provider registries = ctx.vanillaContext() != null ? ctx.vanillaContext().registries() : null;
            if (registries != null) {
                jukebox.song().unwrap(registries).ifPresent(songHolder -> {
                    var song = songHolder.value();
                    sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "jukebox_card",
                            new VisualComponents.JukeboxCard(song.description(), (int) song.lengthInSeconds(), song.comparatorOutput())));
                });
            }
        }
    }

    private static void appendEnchantments(TooltipContext ctx, List<TooltipContribution> sink) {
        if (!ctx.detailMode()) return;
        ItemEnchantments enchants = ctx.stack().get(DataComponents.ENCHANTMENTS);
        ItemEnchantments storedEnchants = ctx.stack().get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments targetEnchants = enchants != null && !enchants.isEmpty() ? enchants : storedEnchants;
        if (targetEnchants != null) {
            for (var entry : targetEnchants.entrySet()) {
                int lvl = entry.getIntValue();
                int max = entry.getKey().value().getMaxLevel();
                boolean isMax = lvl >= max;
                MutableComponent line = Component.literal(isMax ? "★ " : "✦ ")
                        .append(Enchantment.getFullname(entry.getKey(), lvl));
                sink.add(TooltipContribution.text(TooltipCategory.ENCHANTMENTS, isMax ? -1 : 1,
                        "ench:" + entry.getKey().getRegisteredName(),
                        line.withStyle(isMax ? ChatFormatting.GOLD : ChatFormatting.AQUA)));
            }
        }
    }

    private static void appendSuspiciousStew(
            TooltipContext ctx, List<TooltipContribution> sink
    ) {
        if (!Config.SHOW_EFFECTS.get()) return;

        SuspiciousStewEffects stew =
                ctx.stack().get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stew == null || stew.effects().isEmpty()) return;

        List<MobEffectInstance> effects = stew.effects().stream()
                .map(effect -> new MobEffectInstance(effect.effect(), effect.duration()))
                .toList();

        sink.add(TooltipContribution.visual(
                TooltipCategory.EFFECTS, 0, "stew_effects",
                new VisualComponents.PotionEffects(effects, TooltipUtil.tickRate(ctx))
        ));
    }

    private static void appendChargedProjectiles(TooltipContext ctx, List<TooltipContribution> sink) {
        ChargedProjectiles charged = ctx.stack().get(DataComponents.CHARGED_PROJECTILES);
        if (charged != null && !charged.isEmpty()) {
            sink.add(TooltipContribution.visual(TooltipCategory.TOOL, 0, "charged_grid",
                    new VisualComponents.ContainerGrid(charged.getItems())));
        }
    }

    private static void appendContainers(TooltipContext ctx, List<TooltipContribution> sink) {
        if (!Config.SHOW_CONTAINER_PREVIEW.get()) return;
        List<ItemStack> previewItems = new ArrayList<>();
        ItemContainerContents contents = ctx.stack().get(DataComponents.CONTAINER);
        if (contents != null) contents.nonEmptyItems().forEach(previewItems::add);
        BundleContents bundle = ctx.stack().get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) bundle.itemsCopy().forEach(previewItems::add);
        if (!previewItems.isEmpty()) {
            sink.add(TooltipContribution.visual(TooltipCategory.CONTAINER_PREVIEW, 0, "container_grid",
                    new VisualComponents.ContainerGrid(previewItems)));
        }

        SeededContainerLoot loot = ctx.stack().get(DataComponents.CONTAINER_LOOT);
        if (loot != null) {
            sink.add(TooltipContribution.text(TooltipCategory.CONTAINER_PREVIEW, -5, "container_loot",
                    Component.literal("🎁 随机战利品容器 (" + loot.lootTable().location().getPath() + ")").withStyle(ChatFormatting.GOLD)));
        }
    }

    private static void appendPotions(TooltipContext ctx, List<TooltipContribution> sink) {
        if (!Config.SHOW_EFFECTS.get()) return;
        PotionContents potion = ctx.stack().get(DataComponents.POTION_CONTENTS);
        if (potion != null && potion.hasEffects()) {
            List<MobEffectInstance> effects = new ArrayList<>();
            potion.getAllEffects().forEach(effects::add);
            float tickRate = TooltipUtil.tickRate(ctx);
            sink.add(TooltipContribution.visual(TooltipCategory.EFFECTS, 0, "potion_visual",
                    new VisualComponents.PotionEffects(effects, tickRate)));
        }
    }

    private static void appendFuel(
            TooltipContext ctx, List<TooltipContribution> sink
    ) {
        if (!Config.SHOW_FUEL.get()) return;

        int burn = ctx.stack().getBurnTime(null);
        if (burn <= 0) return;

        sink.add(TooltipContribution.visual(
                TooltipCategory.FUEL, 0, "fuel_visual",
                new VisualComponents.FuelGauge(burn)
        ));
    }


    private static void appendToolSpeed(
            TooltipContext ctx, List<TooltipContribution> sink
    ) {
        if (!Config.SHOW_TOOL_SPEED.get()) return;

        Tool tool = ctx.stack().get(DataComponents.TOOL);
        if (tool == null) return;

        float maximum = tool.defaultMiningSpeed();
        for (var rule : tool.rules()) {
            maximum = Math.max(maximum, rule.speed().orElse(tool.defaultMiningSpeed()));
        }

        sink.add(TooltipContribution.text(
                TooltipCategory.TOOL, 0, "tool",
                Component.literal(String.format(
                        java.util.Locale.ROOT, "⛏ 挖掘倍率: %.1fx", maximum
                )).withStyle(ChatFormatting.DARK_AQUA)
        ));
    }


    private static void appendCooldown(TooltipContext ctx, List<TooltipContribution> sink) {
        if (Config.SHOW_COOLDOWN.get() && ctx.player() != null && ctx.player().getCooldowns().isOnCooldown(ctx.stack().getItem())) {
            float percent = ctx.player().getCooldowns().getCooldownPercent(ctx.stack().getItem(), 0.0F);
            sink.add(TooltipContribution.visual(TooltipCategory.COOLDOWN, 0, "cooldown_gauge",
                    new VisualComponents.CooldownGauge(percent)));
        }
    }

    private static void appendDecorations(TooltipContext ctx, List<TooltipContribution> sink) {
        PotDecorations pot = ctx.stack().get(DataComponents.POT_DECORATIONS);
        if (pot != null) {
            List<ItemStack> sherds = new ArrayList<>();
            pot.back().ifPresent(i -> sherds.add(new ItemStack(i)));
            pot.left().ifPresent(i -> sherds.add(new ItemStack(i)));
            pot.right().ifPresent(i -> sherds.add(new ItemStack(i)));
            pot.front().ifPresent(i -> sherds.add(new ItemStack(i)));
            if (!sherds.isEmpty()) {
                sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "pot_grid", new VisualComponents.ContainerGrid(sherds)));
            }
        }

        BannerPatternLayers banner = ctx.stack().get(DataComponents.BANNER_PATTERNS);
        if (banner != null && !banner.layers().isEmpty()) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 0, "banner_layers",
                    Component.literal("🚩 旗帜图案层数: " + banner.layers().size()).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        DyeColor baseColor = ctx.stack().get(DataComponents.BASE_COLOR);
        if (baseColor != null) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 1, "base_color",
                    Component.literal("🛡 基础底色: " + baseColor.getName()).withStyle(ChatFormatting.GRAY)));
        }
    }

    private static void appendOminousBottle(TooltipContext ctx, List<TooltipContribution> sink) {
        Integer amp = ctx.stack().get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        if (amp != null) {
            sink.add(TooltipContribution.visual(TooltipCategory.EFFECTS, -5, "ominous_badge", new VisualComponents.OminousBottleBadge(amp)));
        }
    }

    private static void appendCompost(TooltipContext ctx, List<TooltipContribution> sink) {
        if (!Config.SHOW_COMPOST.get()) return;
        Compostable compostable = ctx.stack().getItemHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
        float chance = compostable != null ? compostable.chance() : ComposterBlock.getValue(ctx.stack());
        if (chance > 0.0F) {
            sink.add(TooltipContribution.text(TooltipCategory.COMPOST, 0, "compost",
                    Component.literal("🌱 堆肥概率: " + Math.round(chance * 100.0F) + "%").withStyle(ChatFormatting.GREEN)));
        }
    }

    private static void appendDyeColor(TooltipContext ctx, List<TooltipContribution> sink) {
        DyedItemColor dyedColor = ctx.stack().get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 20, "dyed_color", new VisualComponents.ColorSwatch(dyedColor.rgb())));
        }
    }

    private static void appendLodestone(TooltipContext ctx, List<TooltipContribution> sink) {
        LodestoneTracker lodestone = ctx.stack().get(DataComponents.LODESTONE_TRACKER);
        if (lodestone != null && lodestone.target().isPresent()) {
            var target = lodestone.target().get();
            BlockPos bpos = target.pos();
            double dist = -1.0;
            if (ctx.player() != null && ctx.player().level().dimension().equals(target.dimension())) {
                dist = Math.sqrt(ctx.player().distanceToSqr(bpos.getX(), bpos.getY(), bpos.getZ()));
            }
            sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "lodestone_tracker",
                    new VisualComponents.LodestoneCard(target.dimension().location(), bpos, dist)));
        }
    }

    private static void appendBookContent(TooltipContext ctx, List<TooltipContribution> sink) {
        WrittenBookContent written = ctx.stack().get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            String snippet = written.pages().isEmpty() ? "空" : written.pages().get(0).raw().getString().replace("\n", " ");
            sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "written_book",
                    new VisualComponents.BookSummary(written.author(), written.pages().size(), snippet)));
            return;
        }

        WritableBookContent writable = ctx.stack().get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writable != null) {
            String snippet = writable.pages().isEmpty() ? "空" : writable.pages().get(0).raw().replace("\n", " ");
            sink.add(TooltipContribution.visual(TooltipCategory.CUSTOM, 0, "writable_book",
                    new VisualComponents.BookSummary("未署名 (草稿)", writable.pages().size(), snippet)));
        }
    }

    private static void appendInstrument(TooltipContext ctx, List<TooltipContribution> sink) {
        Holder<Instrument> inst = ctx.stack().get(DataComponents.INSTRUMENT);
        if (inst != null) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 0, "instrument",
                    Component.literal("📯 乐器音效: " + inst.getRegisteredName()).withStyle(ChatFormatting.GOLD)));
        }
    }

    private static void appendProfile(TooltipContext ctx, List<TooltipContribution> sink) {
        ResolvableProfile profile = ctx.stack().get(DataComponents.PROFILE);
        if (profile != null && profile.name().isPresent()) {
            sink.add(TooltipContribution.text(TooltipCategory.IDENTITY, 5, "profile_owner",
                    Component.literal("👤 皮肤所有者: " + profile.name().get()).withStyle(ChatFormatting.YELLOW)));
        }
    }

    private static void appendSpecialAttributes(TooltipContext ctx, List<TooltipContribution> sink) {
        ItemStack stack = ctx.stack();

        Integer maxStack = stack.get(DataComponents.MAX_STACK_SIZE);

        ResourceLocation noteSound = stack.get(DataComponents.NOTE_BLOCK_SOUND);
        if (noteSound != null) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 12, "note_sound",
                    Component.literal("🎵 音符盒发声: " + noteSound.toString()).withStyle(ChatFormatting.AQUA)));
        }

        List<ResourceLocation> recipes = stack.get(DataComponents.RECIPES);
        if (recipes != null && !recipes.isEmpty()) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 15, "recipes_unlock",
                    Component.literal("📖 包含配方解锁: " + recipes.size() + " 个").withStyle(ChatFormatting.GREEN)));
        }

        DebugStickState debugStick = stack.get(DataComponents.DEBUG_STICK_STATE);
        if (debugStick != null && !debugStick.properties().isEmpty()) {
            sink.add(TooltipContribution.text(TooltipCategory.DEBUG, 0, "debug_stick",
                    Component.literal("🔧 调试棒已记录状态 (" + debugStick.properties().size() + " 项)").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    private static void appendMisc(TooltipContext ctx, List<TooltipContribution> sink) {
        ItemStack stack = ctx.stack();
        Integer repairCost = stack.get(DataComponents.REPAIR_COST);
        if (repairCost != null && repairCost > 0) {
            boolean tooExpensive = repairCost >= 40;
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 30, "repair_cost",
                    Component.literal("🔨 铁砧累加惩罚: +" + repairCost + (tooExpensive ? " (过于昂贵!)" : " 级"))
                            .withStyle(tooExpensive ? ChatFormatting.RED : ChatFormatting.GRAY)));
        }


        LockCode lock = stack.get(DataComponents.LOCK);
        if (lock != null && !lock.key().isEmpty()) {
            sink.add(TooltipContribution.text(TooltipCategory.CUSTOM, 0, "lock",
                    Component.literal("🔒 容器已加锁: 需要名为 \"" + lock.key() + "\" 的物品").withStyle(ChatFormatting.DARK_RED)));
        }
    }
}