package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.EquipmentCabinetBlock;
import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import com.example.examplemod.item.*;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import com.example.examplemod.registry.loot.EntityLootModifier;
import com.example.examplemod.registry.loot.WeaponReplacementModifier;
import com.example.examplemod.skill.PlayerSkillData;
import com.example.examplemod.trim.RunewordSmithingRecipe;
import com.example.examplemod.util.TimedModifierTracker;
import com.mojang.serialization.MapCodec;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static com.example.examplemod.registry.ModAttributes.ATTRIBUTES;
import static com.example.examplemod.registry.ModDataComponents.COMPONENTS;

public final class ModRegistry {
    // 1. 各类 DeferredRegister
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ExampleMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExampleMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExampleMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ExampleMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ExampleMod.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ExampleMod.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ExampleMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MOD_ID);
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ExampleMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ExampleMod.MOD_ID);

    // ==================== 注册项定义 (注意顺序：先 Block 后 Item) ====================
    // 方块 & 方块实体
    public static final DeferredHolder<Block, EquipmentCabinetBlock> CABINET =
            BLOCKS.register("equipment_cabinet", () -> new EquipmentCabinetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EquipmentCabinetBlockEntity>> CABINET_ENTITY =
            BLOCK_ENTITIES.register("equipment_cabinet", () -> BlockEntityType.Builder.of(EquipmentCabinetBlockEntity::new, CABINET.get()).build(null));

    // 物品 (引用 CABINET.get() 必须放在 CABINET 后面)
    public static final DeferredHolder<Item, BlockItem> CABINET_ITEM = ITEMS.register("equipment_cabinet", () -> new BlockItem(CABINET.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> PORTABLE_STONECUTTER = ITEMS.register("portable_stonecutter", () -> new PortableStonecutterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_ANVIL = ITEMS.register("portable_anvil", () -> new PortableAnvilItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_FURNACE = ITEMS.register("portable_furnace", () -> new PortableFurnaceItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PORTABLE_BREWING_STAND = ITEMS.register("portable_brewing_stand", () -> new PortableBrewingStandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<TurtleShieldItem> TURTLE_SHIELD = ITEMS.register("turtle_shield", () -> new TurtleShieldItem(new Item.Properties().durability(336)));
    public static final DeferredHolder<Item, Item> GLASS_OVERLAY_ITEM = ITEMS.register("glass_overlay_item", () -> new Item(new Item.Properties()));

    // 菜单
    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentCabinetMenu>> CABINET_MENU =
            MENUS.register("equipment_cabinet", () -> IMenuTypeExtension.create(EquipmentCabinetMenu::new));

    // 实体

    // 粒子
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DAMAGE_SPARK =
            PARTICLES.register("damage_spark", () -> new SimpleParticleType(false));

    // 配方
    public static final Supplier<RecipeSerializer<RunewordSmithingRecipe>> RUNEWORD_SMITHING_SERIALIZER =
            RECIPES.register("runeword_smithing", RunewordSmithingRecipe.Serializer::new);

    // 法术

    // Attachments
    public static final Supplier<AttachmentType<PlayerSkillData>> SKILL_DATA = ATTACHMENTS.register("skill_data", () -> AttachmentType.builder(PlayerSkillData::new).serialize(PlayerSkillData.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<TimedModifierTracker>> TIMED_MODIFIERS = ATTACHMENTS.register("timed_modifiers", () -> AttachmentType.builder(TimedModifierTracker::new).build());

    // 战利品修饰器
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS = LOOT_MODIFIERS.register("replace_weapons", () -> WeaponReplacementModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<EntityLootModifier>> ENTITY_LOOT = LOOT_MODIFIERS.register("entity_loot", () -> EntityLootModifier.CODEC);

    // ==================== 统一挂载入口 ====================
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        ATTRIBUTES.register(modBus);
        ATTACHMENTS.register(modBus);
        RECIPES.register(modBus);
        SPELLS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        COMPONENTS.register(modBus);
    }
}