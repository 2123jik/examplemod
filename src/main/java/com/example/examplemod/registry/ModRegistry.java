package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.particle.SpaceSlashParticle;
import com.example.examplemod.entity.EquipmentCabinetBlock;
import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import com.example.examplemod.item.*;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import com.example.examplemod.registry.loot.EntityLootModifier;
import com.example.examplemod.registry.loot.WeaponReplacementModifier;
import com.example.examplemod.runeword.RuneUpgradeRecipe;
import com.example.examplemod.skill.PlayerSkillData;
import com.example.examplemod.util.TimedModifierTracker;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.util.entry.*;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.simple.Val;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@SuppressWarnings("unused")
public final class ModRegistry {

    private static final L2Registrate REG = ExampleMod.REGISTRATE;

    // 1. 方块
    public static final BlockEntry<EquipmentCabinetBlock> CABINET = REG
            .block("equipment_cabinet", EquipmentCabinetBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL).strength(3.5F))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .simpleItem()
            .register();

    // 1. 方块实体
    public static final BlockEntityEntry<EquipmentCabinetBlockEntity> CABINET_ENTITY = REG
            .<EquipmentCabinetBlockEntity>blockEntity("equipment_cabinet", EquipmentCabinetBlockEntity::new)
            .validBlocks(CABINET)
            .register();

    public static final RegistryEntry<MenuType<?>, MenuType<EquipmentCabinetMenu>> CABINET_MENU = REG
            .simple("equipment_cabinet", Registries.MENU, () -> IMenuTypeExtension.create(EquipmentCabinetMenu::new));


    // ==================== 普通物品 ====================
    public static final ItemEntry<PortableStonecutterItem> PORTABLE_STONECUTTER = REG
            .item("portable_stonecutter", PortableStonecutterItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<PortableAnvilItem> PORTABLE_ANVIL = REG
            .item("portable_anvil", PortableAnvilItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<PortableFurnaceItem> PORTABLE_FURNACE = REG
            .item("portable_furnace", PortableFurnaceItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<PortableBrewingStandItem> PORTABLE_BREWING_STAND = REG
            .item("portable_brewing_stand", PortableBrewingStandItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<TurtleShieldItem> TURTLE_SHIELD = REG
            .item("turtle_shield", TurtleShieldItem::new)
            .properties(p -> p.durability(336))
            .register();

    public static final ItemEntry<Item> GLASS_OVERLAY_ITEM = REG
            .item("glass_overlay_item", Item::new)
            .register();


    // 在你的 ModRegistry.java 里：
    public static final Val<SimpleParticleType> DAMAGE_SPARK = REG
            .particle("damage_spark",
                    () -> new SimpleParticleType(false),
                    () -> L2Registrate.ParticleSupplier.provider(SpaceSlashParticle.Provider::new)
            );

    // ==================== 配方序列化器 ====================
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RuneUpgradeRecipe>> RUNE_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("rune_upgrade", () -> new SimpleCraftingRecipeSerializer<>(RuneUpgradeRecipe::new));

    // ==================== 附件 (Attachments) ====================
    public static final RegistryEntry<AttachmentType<?>, AttachmentType<PlayerSkillData>> SKILL_DATA = REG
            .simple("skill_data", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(PlayerSkillData::new)
                    .serialize(PlayerSkillData.CODEC)
                    .copyOnDeath()
                    .build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<TimedModifierTracker>> TIMED_MODIFIERS = REG
            .simple("timed_modifiers", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(TimedModifierTracker::new).build());

    // ==================== 战利品修饰器 ====================
    public static final RegistryEntry<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS = REG
            .simple("replace_weapons", NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, () -> WeaponReplacementModifier.CODEC);

    public static final RegistryEntry<MapCodec<? extends IGlobalLootModifier>, MapCodec<EntityLootModifier>> ENTITY_LOOT = REG
            .simple("entity_loot", NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, () -> EntityLootModifier.CODEC);


    public static void init() {

    }
}
