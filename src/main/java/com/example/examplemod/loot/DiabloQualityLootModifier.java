package com.example.examplemod.loot;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.loot.DiabloQualityRoller.EquipmentQuality;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.xkmc.l2hostility.init.data.LHConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
@EventBusSubscriber(modid = ExampleMod.MODID)
public class DiabloQualityLootModifier extends LootModifier {

    public static final MapCodec<DiabloQualityLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, DiabloQualityLootModifier::new)
    );

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper ->
                helper.register(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "quality_roll"), CODEC)
        );
    }

    public DiabloQualityLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    // ================= 3. 核心流水线 =================
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx) {
        // 判断是否为生物被击杀掉落
        boolean isMobKill = ctx.hasParam(LootContextParams.DAMAGE_SOURCE)
                && ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity;

        int extraWeaponAttempts = 0;
        // 提取环境与玩家信息（完全防 NPE）
        Entity entity = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
        LivingEntity mob = isMobKill ? (LivingEntity) entity : null;
        ServerPlayer player = resolvePlayer(ctx, mob);
        if (!isMobKill || player == null||!(mob instanceof net.minecraft.world.entity.monster.Enemy)) {
            return generatedLoot;
        }
        ServerLevel level = ctx.getLevel();
        RandomSource random = ctx.getRandom();
        DiabloQualityRoller.DropContext dropContext = DropContextBuilder.build(level, mob, player, ctx.getLuck());

        for (int i = 0; i <
                -1+Math.floor(Math.pow((1 + LHConfig.SERVER.healthFactor.getAsDouble()), (dropContext.mobLevel() - dropContext.playerDifficulty())))
                ; i++) {
            extraWeaponAttempts += 1;
        }

        if (extraWeaponAttempts <= 0) {
            return generatedLoot;
        }


        // 根据触发次数，逐一尝试掉落额外武器
        for (int i = 0; i < extraWeaponAttempts; i++) {
            // ① 掷骰暗黑品质
            EquipmentQuality quality = DiabloQualityRoller.rollQuality(dropContext, random);

            // ② 从 Tag 抽选底子武器
            ItemStack bonusWeapon = pickWeaponWithFallback(quality, random);
            if (bonusWeapon.isEmpty()) continue;
            var gc=GenContext.forPlayer(player);
            LootRarity rarity = LootRarity.random(gc);

            bonusWeapon = LootController.createLootItem(bonusWeapon, rarity, gc);

            // ④ 追加进最终战利品
            generatedLoot.add(bonusWeapon);
        }

        return generatedLoot;
    }


    private static ServerPlayer resolvePlayer(LootContext ctx, LivingEntity mob) {
        if (ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer sp) return sp;
        if (ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer sp) return sp;
        if (ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer sp) return sp;
        if (mob != null && mob.getKillCredit() instanceof ServerPlayer sp) return sp;
        return null;
    }

    private static ItemStack pickWeaponWithFallback(EquipmentQuality target, RandomSource random) {
        List<EquipmentQuality> cascade = switch (target) {
            case UNIQUE -> List.of(EquipmentQuality.UNIQUE, EquipmentQuality.RARE, EquipmentQuality.MAGIC, EquipmentQuality.WHITE);
            case SET    -> List.of(EquipmentQuality.SET, EquipmentQuality.RARE, EquipmentQuality.MAGIC, EquipmentQuality.WHITE);
            case RARE   -> List.of(EquipmentQuality.RARE, EquipmentQuality.MAGIC, EquipmentQuality.WHITE);
            case MAGIC  -> List.of(EquipmentQuality.MAGIC, EquipmentQuality.WHITE);
            case WHITE  -> List.of(EquipmentQuality.WHITE);
        };
        for (EquipmentQuality q : cascade) {
            Optional<Item> item = BuiltInRegistries.ITEM.getTag(q.getTag())
                    .flatMap(set -> set.getRandomElement(random))
                    .map(Holder::value);
            if (item.isPresent()) return new ItemStack(item.get());
        }
        return new ItemStack(Items.IRON_SWORD);
    }

}