package com.example.examplemod.engine;

import com.example.examplemod.entity.attachments.SyncReservedManaPayload;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public class WandPassiveTickHandler {

    private static final ResourceLocation WAND_CD_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "wand_cooldown_mult");

    private static final Map<UUID, WandCompiler.CompiledWand> HELD_WAND_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> POST_SLOT_PROGRESS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_SYNCED_RESERVED = new ConcurrentHashMap<>();

    private static final ThreadLocal<Boolean> IN_BARRIER_REFLECT = ThreadLocal.withInitial(() -> false);

    public static void updateHeldWand(ServerPlayer player, @Nullable WandCompiler.CompiledWand compiled) {
        if (compiled == null) {
            HELD_WAND_CACHE.remove(player.getUUID());
            applyCooldownModifier(player, 1.0F);
            float onlyAura = ManaBudgetService.getTotalReservedMana(player, 0.0F, 0.0F);
            syncReservedIfChanged(player, onlyAura);
        } else {
            HELD_WAND_CACHE.put(player.getUUID(), compiled);
            POST_SLOT_PROGRESS.putIfAbsent(player.getUUID(), new float[16]);
            applyCooldownModifier(player, compiled.cooldownMult());
            float total = ManaBudgetService.getTotalReservedMana(
                    player, compiled.flatReserve(), compiled.percentReserve());
            ManaBudgetService.clampAndSync(player, total);
            LAST_SYNCED_RESERVED.put(player.getUUID(), total);
        }
    }

    /**
     * 跨版本零兼容风险的冷却缩放：直接映射为 COOLDOWN_REDUCTION 属性修正
     * cooldownMult = 0.60F 表示冷却缩放到 60%，即额外增加 (1 - 0.60) = +0.40 冷却缩减
     */
    private static void applyCooldownModifier(ServerPlayer player, float cooldownMult) {
        AttributeInstance inst = player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION);
        if (inst == null) return;
        inst.removeModifier(WAND_CD_MODIFIER_ID);

        float cdBonus = 1.0F - cooldownMult;
        if (Math.abs(cdBonus) > 0.001F) {
            inst.addTransientModifier(new AttributeModifier(
                    WAND_CD_MODIFIER_ID,
                    cdBonus,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Nullable
    public static WandCompiler.CompiledWand getHeldWand(ServerPlayer player) {
        return HELD_WAND_CACHE.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (IN_BARRIER_REFLECT.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WandCompiler.CompiledWand wand = HELD_WAND_CACHE.get(player.getUUID());
        if (wand == null || !wand.hasArcaneBarrier()) return;

        MagicData data = MagicData.getPlayerMagicData(player);
        float maxBlockable = data.getMana() / 30.0F;
        if (maxBlockable < 0.2F) return;

        float blocked = Math.min(event.getAmount(), maxBlockable);
        float manaSpent = blocked * 30.0F;
        if (ManaBudgetService.tryConsumeMana(player, manaSpent)) {
            event.setAmount(Math.max(0.0F, event.getAmount() - blocked));
            if (player.level() instanceof ServerLevel sl) {
                IN_BARRIER_REFLECT.set(true);
                try {
                    for (LivingEntity mob : sl.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(4.0),
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
                        mob.hurt(sl.damageSources().magic(), manaSpent * 0.1F);
                    }
                } finally {
                    IN_BARRIER_REFLECT.set(false);
                }
            }
        }
    }

    public static void notifyWandCast(ServerPlayer player) {
        WandCompiler.CompiledWand wand = HELD_WAND_CACHE.get(player.getUUID());
        if (wand == null || wand.postSlotTriggers().isEmpty()) return;

        float[] prog = POST_SLOT_PROGRESS.computeIfAbsent(player.getUUID(), k -> new float[16]);
        float effMult = 1.0F + wand.postChargeEfficiencyBonus();
        for (int i = 0; i < Math.min(16, wand.postSlotTriggers().size()); i++) {
            var post = wand.postSlotTriggers().get(i);
            if ("CAST".equals(post.chargeType())) {
                prog[i] = Math.min(100.0F, prog[i] + post.chargeRate() * effMult);
                if (prog[i] >= 100.0F) {
                    if (WandExecutionEngine.executeSingleNode(player, post.targetNode(), null, null)) {
                        prog[i] -= 100.0F;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onManaRegenBlock(ChangeManaEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WandCompiler.CompiledWand wand = HELD_WAND_CACHE.get(player.getUUID());
        if (wand != null && wand.stopNaturalRegen() && event.getNewMana() > event.getOldMana()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WandCompiler.CompiledWand wand = HELD_WAND_CACHE.get(player.getUUID());
        if (wand == null) return;

        Vec3 prev = LAST_POS.getOrDefault(player.getUUID(), player.position());
        double movedDist = player.position().distanceTo(prev);
        LAST_POS.put(player.getUUID(), player.position());

        if (!wand.postSlotTriggers().isEmpty()) {
            float[] prog = POST_SLOT_PROGRESS.computeIfAbsent(player.getUUID(), k -> new float[16]);
            float effMult = 1.0F + wand.postChargeEfficiencyBonus();
            for (int i = 0; i < Math.min(16, wand.postSlotTriggers().size()); i++) {
                var post = wand.postSlotTriggers().get(i);
                float gain = switch (post.chargeType()) {
                    case "MOVE" -> (float) movedDist * post.chargeRate();
                    case "STAND" -> (movedDist < 0.01) ? (post.chargeRate() / 20.0F) : 0.0F;
                    case "TIME" -> post.chargeRate() / 20.0F;
                    default -> 0.0F;
                };
                prog[i] = Math.min(100.0F, prog[i] + gain * effMult);
                if (prog[i] >= 100.0F) {
                    if (WandExecutionEngine.executeSingleNode(player, post.targetNode(), null, null)) {
                        prog[i] -= 100.0F;
                    }
                }
            }
        }

        if (player.tickCount % 20 == 0) {
            float totalReserved = ManaBudgetService.getTotalReservedMana(
                    player, wand.flatReserve(), wand.percentReserve());
            float usableMax = ManaBudgetService.getUsableMaxMana(player, totalReserved);
            MagicData data = MagicData.getPlayerMagicData(player);
            boolean manaChanged = false;

            if (!wand.stopNaturalRegen() && wand.bonusManaRegen() > 0.0F && data.getMana() < usableMax) {
                float next = Math.min(usableMax, data.getMana() + wand.bonusManaRegen());
                if (next != data.getMana()) {
                    data.setMana(next);
                    manaChanged = true;
                }
            }

            if (data.getMana() > usableMax) {
                data.setMana(usableMax);
                manaChanged = true;
            }

            if (manaChanged) {
                PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
            }
            syncReservedIfChanged(player, totalReserved);

            if (wand.hasGuardianSpirit() && player.level() instanceof ServerLevel sl) {
                boolean enemyNear = !sl.getEntitiesOfClass(LivingEntity.class,
                        new AABB(player.blockPosition()).inflate(14.0),
                        e -> e != player && e.isAlive() && !e.isAlliedTo(player)).isEmpty();
                if (enemyNear) {
                    WandExecutionEngine.castConnected(player, true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            applyCooldownModifier(sp, 1.0F);
        }
        UUID id = event.getEntity().getUUID();
        HELD_WAND_CACHE.remove(id);
        POST_SLOT_PROGRESS.remove(id);
        LAST_POS.remove(id);
        LAST_SYNCED_RESERVED.remove(id);
    }

    private static void syncReservedIfChanged(ServerPlayer player, float totalReserved) {
        Float last = LAST_SYNCED_RESERVED.get(player.getUUID());
        if (last == null || Math.abs(last - totalReserved) > 0.01F) {
            LAST_SYNCED_RESERVED.put(player.getUUID(), totalReserved);
            PacketDistributor.sendToPlayer(player, new SyncReservedManaPayload(totalReserved));
        }
    }
}