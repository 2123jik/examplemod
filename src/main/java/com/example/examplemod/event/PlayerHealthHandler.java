package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class PlayerHealthHandler {

    // 1.21 中 ResourceLocation 构造器已私有，需使用 fromNamespaceAndPath
    public static final ResourceLocation HEALTH_MULTIPLIER_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "player_max_health_mult");

    public static final ResourceLocation INITIAL_LUCK_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "player_initial_luck");

    // 1 + 4.0 = 5.0，即 5 倍血量独立乘区
    private static final double MULTIPLIER_AMOUNT = 4.0D;

    // 初始幸运值 +4.0
    private static final double INITIAL_LUCK_AMOUNT = 4.0D;

    private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
            HEALTH_MULTIPLIER_ID,
            MULTIPLIER_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static final AttributeModifier LUCK_MODIFIER = new AttributeModifier(
            INITIAL_LUCK_ID,
            INITIAL_LUCK_AMOUNT,
            AttributeModifier.Operation.ADD_VALUE
    );

    /**
     * 边界情况 1 & 2：玩家进入世界时（首次进入、重连、切换维度）应用属性并修正血量
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            applyHealthModifier(player, false);
            applyLuckModifier(player);
        }
    }

    /**
     * 边界情况 3 & 4：处理玩家重生（死亡重生 vs 末地通关回主世界）
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            boolean isDeathRespawn = !event.isEndConquered();
            applyHealthModifier(player, isDeathRespawn);
            applyLuckModifier(player);

            if (isDeathRespawn) {
                // 确保真实死亡重生后回满 5 倍后的最大血量（如 100/100）
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * 应用初始幸运值 (+4.0)
     */
    public static void applyLuckModifier(ServerPlayer player) {
        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr == null) {
            return;
        }

        AttributeModifier existing = luckAttr.getModifier(INITIAL_LUCK_ID);
        if (existing != null
                && existing.amount() == INITIAL_LUCK_AMOUNT
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE) {
            return;
        }

        luckAttr.addOrReplacePermanentModifier(LUCK_MODIFIER);
    }

    /**
     * 核心血量应用逻辑与边界修正
     */
    public static void applyHealthModifier(ServerPlayer player, boolean forceFullHealth) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        AttributeModifier existing = maxHealthAttr.getModifier(HEALTH_MULTIPLIER_ID);
        if (existing != null
                && existing.amount() == MULTIPLIER_AMOUNT
                && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }

        float oldMaxHealth = player.getMaxHealth();
        float currentHealth = player.getHealth();

        maxHealthAttr.addOrReplacePermanentModifier(HEALTH_MODIFIER);

        float newMaxHealth = player.getMaxHealth();

        if (!player.isDeadOrDying() && currentHealth > 0.0F && oldMaxHealth > 0.0F) {
            if (forceFullHealth) {
                player.setHealth(newMaxHealth);
            } else if (oldMaxHealth != newMaxHealth) {
                float ratio = currentHealth / oldMaxHealth;
                float adjustedHealth = Mth.clamp(ratio * newMaxHealth, 1.0F, newMaxHealth);
                player.setHealth(adjustedHealth);
            }
        }
    }
}