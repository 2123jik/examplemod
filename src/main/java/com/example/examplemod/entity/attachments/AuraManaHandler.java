package com.example.examplemod.entity.attachments;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@EventBusSubscriber(modid = "examplemod")
public class AuraManaHandler {

    // 续杯持续时间：45 tick (2.25秒)，每20 tick刷新一次，关闭后最多2秒自然消散
    private static final int REFRESH_DURATION_TICKS = 45;

    // 临时记录：服务端本次施法在 ModifySpellLevelEvent 修饰前的原始基础等级
    private static final Map<UUID, Integer> PENDING_BASE_LEVEL = new ConcurrentHashMap<>();

    /**
     * 步骤 1：在饰品/共鸣戒指修改法术等级之前，记录真实的 baseLevel
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_BASE_LEVEL.put(player.getUUID(), event.getLevel());
        }
    }

    /**
     * 步骤 2：拦截施法，实现光环开关（Toggle）
     */
    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 注：若你的 ISS 版本返回的是 String，请用 ResourceLocation.parse(event.getSpellId())
        ResourceLocation spellId = ResourceLocation.parse(event.getSpellId());
        AuraRegistry.AuraSpec spec = AuraRegistry.get(spellId);
        if (spec == null) {
            PENDING_BASE_LEVEL.remove(player.getUUID());
            return;
        }

        // 防止用一次性卷轴零消耗白嫖开关光环
        if (event.getCastSource() == CastSource.SCROLL) {
            PENDING_BASE_LEVEL.remove(player.getUUID());
            player.displayClientMessage(
                    Component.literal("一次性卷轴无法作为常驻光环开启").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return; // 不取消，让卷轴走原版一次性施法
        }

        // 接管施法：取消原吟唱、原长CD和原单次扣蓝
        event.setCanceled(true);

        int effectiveLevel = event.getSpellLevel();
        Integer capturedBase = PENDING_BASE_LEVEL.remove(player.getUUID());
        int baseLevel = (capturedBase != null && capturedBase > 0) ? capturedBase : effectiveLevel;

        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) return;

        LinkedHashMap<ResourceLocation, Integer> activeAuras = player.getData(AuraAttachments.ACTIVE_AURAS);

        if (activeAuras.containsKey(spellId)) {
            // ===== 关闭光环 =====
            activeAuras.remove(spellId);
            player.removeEffect(spec.effect());
            spec.onRemoveCleanup().accept(player);

            player.displayClientMessage(
                    Component.literal("已关闭光环: ").append(spell.getDisplayName(player))
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
        } else {
            // ===== 开启光环 =====
            float maxMana = getMaxMana(player);
            float alreadyReserved = calculateTotalReserved(player, activeAuras);
            float newCost = getReserveCost(spell, effectiveLevel, player);

            if (alreadyReserved + newCost > maxMana) {
                player.displayClientMessage(
                        Component.literal(String.format("最大法力不足！需要预留 %.0f，当前剩余可预留 %.0f",
                                newCost, Math.max(0, maxMana - alreadyReserved)))
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // 存入基础等级（非加成后等级）
            activeAuras.put(spellId, baseLevel);
            applyAuraEffect(player, spell, spec, effectiveLevel);

            // 立刻把当前超出 usableMax 的多余蓝量压下去
            clampPlayerMana(player, maxMana - (alreadyReserved + newCost));

            player.displayClientMessage(
                    Component.literal("已开启光环: ").append(spell.getDisplayName(player))
                            .append(String.format(" (预留 %.0f 蓝量)", newCost))
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        }
    }

    /**
     * 步骤 3：每秒动态重算（响应 +1 法术等级饰品穿脱） + 心跳续杯 + 超限自动关闭
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }

        LinkedHashMap<ResourceLocation, Integer> activeAuras = player.getData(AuraAttachments.ACTIVE_AURAS);
        if (activeAuras.isEmpty()) {
            return;
        }

        float maxMana = getMaxMana(player);
        float totalReserved = 0.0F;
        List<ResourceLocation> overflowToRemove = new ArrayList<>();

        // 遍历开启顺序（LinkedHashMap），动态重算每个光环的最新等级与蓝耗
        for (Map.Entry<ResourceLocation, Integer> entry : activeAuras.entrySet()) {
            ResourceLocation spellId = entry.getKey();
            int baseLevel = entry.getValue();

            AuraRegistry.AuraSpec spec = AuraRegistry.get(spellId);
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spec == null || spell == null) {
                overflowToRemove.add(spellId);
                continue;
            }

            // 实时计算穿戴 +1 等级饰品/装备后的当前生效等级
            int effectiveLevel = getEffectiveLevel(spell, baseLevel, player);
            float cost = getReserveCost(spell, effectiveLevel, player);

            if (totalReserved + cost > maxMana) {
                // 换装降了上限，或者戴了 +1 戒指导致总锁蓝突破 MAX_MANA，标记移除
                overflowToRemove.add(spellId);
            } else {
                totalReserved += cost;
                applyAuraEffect(player, spell, spec, effectiveLevel);
            }
        }

        // 清理超限的光环
        for (ResourceLocation id : overflowToRemove) {
            activeAuras.remove(id);
            AuraRegistry.AuraSpec spec = AuraRegistry.get(id);
            if (spec != null) {
                player.removeEffect(spec.effect());
                spec.onRemoveCleanup().accept(player);
            }
            AbstractSpell spell = SpellRegistry.getSpell(id);
            Component name = spell != null ? spell.getDisplayName(player) : Component.literal(id.toString());
            player.displayClientMessage(
                    Component.literal("法力上限不足以维持，已自动关闭光环: ").append(name)
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }

        // 兜底钳制当前蓝量
        clampPlayerMana(player, Math.max(0.0F, maxMana - totalReserved));
    }

    /**
     * 步骤 4：拦截所有超过“可用蓝量上限(usableMax)”的回蓝/加蓝
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChangeMana(ChangeManaEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LinkedHashMap<ResourceLocation, Integer> activeAuras = player.getData(AuraAttachments.ACTIVE_AURAS);
        if (activeAuras.isEmpty()) {
            return;
        }

        float maxMana = getMaxMana(player);
        float totalReserved = calculateTotalReserved(player, activeAuras);
        float usableMax = Math.max(0.0F, maxMana - totalReserved);

        if (event.getNewMana() > usableMax) {
            // 如果当前已经到达或超过可用上限，且本次是试图往上涨（例如自然回蓝），直接取消，省去无意义发包
            if (event.getOldMana() >= usableMax && event.getNewMana() > event.getOldMana()) {
                event.setCanceled(true);
            } else {
                event.setNewMana(usableMax);
            }
        }
    }

    /**
     * 死亡 & 登出清理
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_BASE_LEVEL.remove(player.getUUID());
            LinkedHashMap<ResourceLocation, Integer> activeAuras = player.getData(AuraAttachments.ACTIVE_AURAS);
            if (!activeAuras.isEmpty()) {
                activeAuras.forEach((id, lvl) -> {
                    AuraRegistry.AuraSpec spec = AuraRegistry.get(id);
                    if (spec != null) spec.onRemoveCleanup().accept(player);
                });
                activeAuras.clear();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_BASE_LEVEL.remove(event.getEntity().getUUID());
    }
    // 加进 AuraManaHandler.java
    private static void syncReservedToClient(ServerPlayer player, float totalReserved) {
        PacketDistributor.sendToPlayer(player, new SyncReservedManaPayload(totalReserved));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float reserved = calculateTotalReserved(player, player.getData(AuraAttachments.ACTIVE_AURAS));
            syncReservedToClient(player, reserved);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float reserved = calculateTotalReserved(player, player.getData(AuraAttachments.ACTIVE_AURAS));
            syncReservedToClient(player, reserved);
        }
    }

    // ==================== 辅助工具方法 ====================

    private static void applyAuraEffect(ServerPlayer player, AbstractSpell spell, AuraRegistry.AuraSpec spec, int effectiveLevel) {
        int amplifier = spec.amplifierCalculator().calculate(spell, effectiveLevel, player);
        // ambient = true, visible = false：隐藏烦人的全身粒子，仅保留右上角图标
        player.addEffect(new MobEffectInstance(spec.effect(), REFRESH_DURATION_TICKS, amplifier, true, false, true));
    }

    private static float calculateTotalReserved(ServerPlayer player, Map<ResourceLocation, Integer> activeAuras) {
        float total = 0.0F;
        for (Map.Entry<ResourceLocation, Integer> entry : activeAuras.entrySet()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getKey());
            if (spell != null && AuraRegistry.isAuraSpell(entry.getKey())) {
                int effectiveLevel = getEffectiveLevel(spell, entry.getValue(), player);
                total += getReserveCost(spell, effectiveLevel, player);
            }
        }
        return total;
    }

    private static float getMaxMana(ServerPlayer player) {
        return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
    }

    private static void clampPlayerMana(ServerPlayer player, float usableMax) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getMana() > usableMax) {
            magicData.setMana(usableMax);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }

    private static int getEffectiveLevel(AbstractSpell spell, int baseLevel, ServerPlayer player) {
        // 注：ISS 3.15+ 为 spell.getLevelFor(baseLevel, player) 或 spell.getLevel(baseLevel, player)
        return spell.getLevelFor(baseLevel, player);
    }

    private static float getReserveCost(AbstractSpell spell, int effectiveLevel, ServerPlayer player) {
        // 注：ISS 3.15+ 若移除了 getManaCost(int)，请改为 spell.getManaCost(effectiveLevel) / getMana(effectiveLevel, player)
        return spell.getManaCost(effectiveLevel) * AuraRegistry.RESERVE_MULTIPLIER;
    }
}