package com.example.examplemod.event;

import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class HostilityLuckHandler {

    // 1.21.1 不再使用 UUID，使用 ResourceLocation 作为修饰符唯一 ID
    private static final ResourceLocation HOSTILITY_LUCK_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "hostility_luck");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // 仅在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // 每秒（20 ticks）节流检查一次
            if (player.tickCount % 20 != 0) {
                return;
            }

            // 1.21.1 Attributes.LUCK 是 Holder<Attribute>，直接传入即可
            AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
            if (luckAttr == null) {
                return;
            }

            // 1. 计算目标幸运加成
            int hostilityLevel = DifficultyLevel.ofAny(player);
            double targetBonus = hostilityLevel * 0.1D; // 每 50 级恶意 +1.0 幸运

            // 2. 检查现存修饰符
            AttributeModifier current = luckAttr.getModifier(HOSTILITY_LUCK_ID);

            if (current != null) {
                // 1.21.1 AttributeModifier 变成了 record 风格，取值为 current.amount()
                if (Math.abs(current.amount() - targetBonus) < 1.0E-4) {
                    return; // 数值未变，直接返回
                }
                luckAttr.removeModifier(HOSTILITY_LUCK_ID);
            }

            // 3. 挂载新的基础加成
            if (targetBonus > 0) {
                luckAttr.addTransientModifier(new AttributeModifier(
                        HOSTILITY_LUCK_ID,
                        targetBonus,
                        AttributeModifier.Operation.ADD_VALUE // 1.21.1 中 ADDITION 变成了 ADD_VALUE
                ));
            }
        }
    }
}