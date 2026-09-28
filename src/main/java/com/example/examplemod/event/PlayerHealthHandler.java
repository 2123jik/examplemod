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

    // 1 + 4.0 = 5.0，即 5 倍血量独立乘区
    private static final double MULTIPLIER_AMOUNT = 4.0D;

    private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
            HEALTH_MULTIPLIER_ID,
            MULTIPLIER_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    /**
     * 边界情况 1 & 2：玩家进入世界时（首次进入、重连、切换维度）应用属性并修正血量
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 边界情况：必须仅在服务端处理
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            applyHealthModifier(player, false);
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

            if (isDeathRespawn) {
                // 确保真实死亡重生后回满 5 倍后的最大血量（如 100/100）
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * 核心应用逻辑与边界修正
     */
    public static void applyHealthModifier(ServerPlayer player, boolean forceFullHealth) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        AttributeModifier existing = maxHealthAttr.getModifier(HEALTH_MULTIPLIER_ID);

        // 如果修饰符已经存在且数值匹配，则跳过
        if (existing != null && existing.amount() == MULTIPLIER_AMOUNT) {
            return;
        }

        float oldMaxHealth = player.getMaxHealth();
        float currentHealth = player.getHealth();

        // 1.21.1 推荐使用 addOrReplacePermanentModifier 防止重复添加抛出 IllegalArgumentException
        maxHealthAttr.addOrReplacePermanentModifier(HEALTH_MODIFIER);

        float newMaxHealth = player.getMaxHealth();

        // 边界情况 5：避免在实体死亡或濒死时操作血量
        if (!player.isDeadOrDying() && currentHealth > 0.0F && oldMaxHealth > 0.0F) {
            if (forceFullHealth) {
                player.setHealth(newMaxHealth);
            } else if (oldMaxHealth != newMaxHealth) {
                // 首次添加时按旧血量比例等比缩放当前血量（避免从 20/20 变成 20/100）
                float ratio = currentHealth / oldMaxHealth;
                float adjustedHealth = Mth.clamp(ratio * newMaxHealth, 1.0F, newMaxHealth);
                player.setHealth(adjustedHealth);
            }
        }
    }
}