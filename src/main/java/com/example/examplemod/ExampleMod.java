package com.example.examplemod;

import com.example.examplemod.client.ClientPayloadHandler;
import com.example.examplemod.compat.ModLootCategories;
import com.example.examplemod.content.spell.ModSpells;
import com.example.examplemod.content.spell.SummonedCaveSpider;
import com.example.examplemod.content.spell.SummonedSupportDragon;
import com.example.examplemod.event.MobEquipmentHandler;
import com.example.examplemod.network.ArrowCritPayload;
import com.example.examplemod.network.OpenShopSelectorPayload;
import com.example.examplemod.network.SleepButtonPayload;
import com.example.examplemod.registry.*;
import com.example.examplemod.skill.SkillRegistry;
import com.example.examplemod.skill.SyncSkillDataPayload;
import com.example.examplemod.skill.UpgradeSkillPayload;
import com.example.examplemod.trim.RunewordManager;
import com.example.examplemod.util.WeaponReplacer;
import com.mojang.serialization.MapCodec;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
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

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.class);

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS =
            LOOT_MODIFIERS.register("replace_weapons", () -> WeaponReplacementModifier.CODEC);

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
        ModMenus.MENUS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModAttributes.ATTRIBUTES.register(modEventBus);

        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(ModLootCategories::onRegister);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        ModRecipes.SERIALIZERS.register(modEventBus);
        NeoForge.EVENT_BUS.register(MobEquipmentHandler.class);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_CAVE_SPIDER.get(), SummonedCaveSpider.createAttributes().build());
        event.put(ModEntities.SUMMONED_SUPPORT_DRAGON.get(), SummonedSupportDragon.createAttributes().build());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WeaponReplacer::initWeaponPools);
        event.enqueueWork(SkillRegistry::init);
    }

    public void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // ================= 客户端接收 (S2C) =================
        // 使用独立类避免在 Dedicated Server 上触发 ClassNotFoundException
        registrar.playToClient(
                ArrowCritPayload.TYPE,
                ArrowCritPayload.STREAM_CODEC,
                ClientPayloadHandler::handleArrowCrit
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
                UpgradeSkillPayload.TYPE,
                UpgradeSkillPayload.STREAM_CODEC,
                UpgradeSkillPayload::handle // 请确保其内部也使用了 context.enqueueWork
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