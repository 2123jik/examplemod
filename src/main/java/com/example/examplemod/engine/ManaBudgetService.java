package com.example.examplemod.engine;

import com.example.examplemod.entity.attachments.AuraAttachments;
import com.example.examplemod.entity.attachments.AuraRegistry;
import com.example.examplemod.entity.attachments.SyncReservedManaPayload;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

@EventBusSubscriber(modid = "examplemod")
public class ManaBudgetService {

    // 绝对防白嫖底线：任何一次主动施法或子触发至少消耗 5 点蓝
    public static final float MIN_CAST_COST = 1.0F;

    /**
     * 总预留蓝量 = 全局唯一光环锁蓝 + 法术书/法杖常驻被动锁蓝
     */
    public static float getTotalReservedMana(ServerPlayer player, float wandFlatReserve, float wandPercentReserve) {
        float maxMana = getMaxMana(player);

        float auraReserved = 0.0F;
        Map<ResourceLocation, Integer> auras = player.getData(AuraAttachments.ACTIVE_AURAS);
        for (var entry : auras.entrySet()) {
            AbstractSpell spell = SpellRegistry.getSpell(entry.getKey());
            if (spell != null && AuraRegistry.isAuraSpell(entry.getKey())) {
                int effLvl = spell.getLevelFor(entry.getValue(), player);
                auraReserved += spell.getManaCost(effLvl) * AuraRegistry.RESERVE_MULTIPLIER;
            }
        }

        float wandReserved = wandFlatReserve + (maxMana * wandPercentReserve);
        return Math.min(maxMana, auraReserved + wandReserved);
    }

    public static float getMaxMana(ServerPlayer player) {
        return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
    }

    public static float getUsableMaxMana(ServerPlayer player, float totalReserved) {
        return Math.max(0.0F, getMaxMana(player) - totalReserved);
    }

    /**
     * 核心原子扣蓝闸门：所有施法与触发必须通过此校验
     */
    public static boolean tryConsumeMana(ServerPlayer player, float rawCost) {
        float finalCost = Math.max(MIN_CAST_COST, rawCost);
        MagicData magicData = MagicData.getPlayerMagicData(player);

        if (magicData.getMana() < finalCost) {
            return false;
        }

        magicData.setMana(magicData.getMana() - finalCost);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        return true;
    }

    public static void clampAndSync(ServerPlayer player, float totalReserved) {
        float usableMax = getUsableMaxMana(player, totalReserved);
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getMana() > usableMax) {
            magicData.setMana(usableMax);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
        PacketDistributor.sendToPlayer(player, new SyncReservedManaPayload(totalReserved));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChangeMana(ChangeManaEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var heldWand = WandPassiveTickHandler.getHeldWand(player);
        float wandFlat = (heldWand != null) ? heldWand.flatReserve() : 0.0F;
        float wandPct = (heldWand != null) ? heldWand.percentReserve() : 0.0F;

        float totalReserved = getTotalReservedMana(player, wandFlat, wandPct);
        if (totalReserved <= 0.0F) return;

        float usableMax = getUsableMaxMana(player, totalReserved);
        if (event.getNewMana() > usableMax) {
            if (event.getOldMana() >= usableMax && event.getNewMana() > event.getOldMana()) {
                event.setCanceled(true);
            } else {
                event.setNewMana(usableMax);
            }
        }
    }
}