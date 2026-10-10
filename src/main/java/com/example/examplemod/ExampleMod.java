package com.example.examplemod;

import com.example.examplemod.client.XpFlightClient;
import com.example.examplemod.compat.apothic.CurioSlots;
import com.example.examplemod.entity.EquipmentCabinetBlock;
import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import com.example.examplemod.network.*;
import com.example.examplemod.registry.*;
import com.example.examplemod.registry.loot.EntityLootModifier;
import com.example.examplemod.registry.loot.WeaponReplacementModifier;
import com.example.examplemod.registry.modulargolems.ModRegistry;
import com.example.examplemod.skill.SkillRegistry;
import com.example.examplemod.skill.SyncSkillDataPayload;
import com.example.examplemod.skill.UpgradeSkillPayload;
import com.example.examplemod.spell.ModSpells;
import com.example.examplemod.spell.SummonedCaveSpider;
import com.example.examplemod.spell.SummonedSupportDragon;
import com.example.examplemod.trim.RunewordManager;
import com.mojang.serialization.MapCodec;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.examplemod.registry.ModItems.ITEMS;
import static com.example.examplemod.registry.ModMenus.MENUS;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.class);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    public static final DeferredHolder<Block, EquipmentCabinetBlock> CABINET =

            BLOCKS.register("equipment_cabinet", () ->
                    new EquipmentCabinetBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.5F)
                    ));
    public static final DeferredHolder<Item, BlockItem> CABINET_ITEM =
            ITEMS.register("equipment_cabinet", () ->
                    new BlockItem(CABINET.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EquipmentCabinetBlockEntity>>
            CABINET_ENTITY = BLOCK_ENTITIES.register("equipment_cabinet", () ->
            BlockEntityType.Builder.of(
                    EquipmentCabinetBlockEntity::new, CABINET.get()
            ).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentCabinetMenu>>
            CABINET_MENU = MENUS.register("equipment_cabinet", () ->
            IMenuTypeExtension.create(EquipmentCabinetMenu::new));
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS =
            LOOT_MODIFIERS.register("replace_weapons", () -> WeaponReplacementModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<EntityLootModifier>> ENTITY_LOOT =
            LOOT_MODIFIERS.register("entity_loot", () -> EntityLootModifier.CODEC);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        // 建议仅在客户端环境注册客户端配置，避免服务端读取无用配置
        if (FMLEnvironment.dist.isClient()) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        ModSpells.register(modEventBus);
        ModEntities.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModAttributes.ATTRIBUTES.register(modEventBus);
        ModRegistry.MODIFIERS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        ModRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(CurioSlots::onRegister);

    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_CAVE_SPIDER.get(), SummonedCaveSpider.createAttributes().build());
        event.put(ModEntities.SUMMONED_SUPPORT_DRAGON.get(), SummonedSupportDragon.createAttributes().build());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
//        event.enqueueWork(WeaponReplacer::initWeaponPools);
        event.enqueueWork(SkillRegistry::init);
    }

    public void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");


        registrar.playToClient(
                XpFlightPayload.TYPE,
                XpFlightPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> XpFlightClient.receive(payload))
        );
        registrar.playToClient(
                SyncSkillDataPayload.TYPE,
                SyncSkillDataPayload.STREAM_CODEC,
                SyncSkillDataPayload::handle
        );

        // ================= 服务端接收 (C2S) =================

        registrar.playToServer(
                OpenShopSelectorPayload.TYPE,
                OpenShopSelectorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> { // 切换到主线程
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        ViScriptShopServerUtil.serverOpenShopSelector(serverPlayer);
                    }
                })
        );
        registrar.playToServer(
                AutoUpgradeArtifactsPayload.TYPE,
                AutoUpgradeArtifactsPayload.STREAM_CODEC,
                AutoUpgradeArtifactsPayload::handle
        );
        registrar.playToServer(
                UpgradeSkillPayload.TYPE,
                UpgradeSkillPayload.STREAM_CODEC,
                UpgradeSkillPayload::handle // 请确保其内部也使用了 context.enqueueWork
        );
        registrar.playToServer(
                SortToBackpackPayload.TYPE,
                SortToBackpackPayload.STREAM_CODEC,
                SortToBackpackPayload::handle
        );
        registrar.playToServer(
                RequestFetchRecipeIngredientsPayload.TYPE,
                RequestFetchRecipeIngredientsPayload.STREAM_CODEC,
                RequestFetchRecipeIngredientsPayload::handle
        );
        registrar.playToServer(
                SwapEquipmentPayload.TYPE,
                SwapEquipmentPayload.STREAM_CODEC,
                ServerPayloadHandler::handle
        );
        registrar.playToServer(
                SleepButtonPayload.TYPE,
                SleepButtonPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> { // 切换到主线程
                    if (context.player() instanceof ServerPlayer player) {
                        handleSleepButton(player);
                    }
                })
        );
    }

    private static void handleSleepButton(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimensionType().bedWorks()) {
            player.sendSystemMessage(Component.translatable("examplemod.message.bed_explodes"));
            return;
        }

        long timeOfDay = level.getDayTime() % 24000L;

        if (timeOfDay < 12542L || timeOfDay >= 23460L) {
            player.sendSystemMessage(Component.translatable("examplemod.message.cannot_sleep_now"));
            return;
        }

        BlockPos respawnPos = player.blockPosition();

        player.setRespawnPosition(
                level.dimension(),
                respawnPos,
                player.getYRot(),
                false,
                true
        );

        // 跳到第二天早上 6:00 (1000 tick)
        long nextMorning = ((level.getDayTime() / 24000L) + 1L) * 24000L + 1000L;
        level.setDayTime(nextMorning);

        // 重置天气（如果是雨天/雷雨天）
        if (level.isRaining()) {
            level.resetWeatherCycle();
        }

        // 重置玩家休息时间（避免生成幻翼）
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));

        player.closeContainer();
        player.sendSystemMessage(Component.translatable("examplemod.message.woke_up"));
    }
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        // 挂载数据包重新加载器
        event.addListener(new RunewordManager());
    }
}