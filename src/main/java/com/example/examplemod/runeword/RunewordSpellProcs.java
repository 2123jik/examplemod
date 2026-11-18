package com.example.examplemod.runeword;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class RunewordSpellProcs {

    public enum Trigger {
        ON_STRIKE("击中时"),
        ON_STRUCK("被击中时"),
        ON_CAST("施法时"),
        ON_KILL("击杀时");

        public final String label;

        Trigger(String label) {
            this.label = label;
        }
    }

    public record SpellProc(Trigger trigger, double chancePercent, ResourceLocation spellId, int baseLevel) {}

    private static final int INTERNAL_COOLDOWN_TICKS = 20; // 每个触发槽位 1 秒内置冷却防刷屏卡顿
    private static final Set<UUID> PROC_GUARD = ConcurrentHashMap.newKeySet();
    private static final Map<String, List<SpellProc>> PROCS = new HashMap<>();

    static {
        // ==================== 头盔 ====================
        reg("delirium",
                proc(Trigger.ON_STRIKE, 11, "irons_spellbooks:sonic_boom", 2),
                proc(Trigger.ON_STRUCK, 14, "irons_spellbooks:shockwave", 3),
                proc(Trigger.ON_STRUCK, 6,  "irons_spellbooks:eldritch_blast", 2));
        reg("dream_helm",
                proc(Trigger.ON_STRUCK, 10, "irons_spellbooks:shockwave", 3));

        // ==================== 胸甲 ====================
        reg("myth",
                proc(Trigger.ON_STRUCK, 10, "irons_spellbooks:gust", 2));
        reg("treachery",
                proc(Trigger.ON_STRUCK, 15, "irons_spellbooks:eldritch_blast", 3));
        reg("enlightenment",
                proc(Trigger.ON_CAST,   25, "irons_spellbooks:fireball", 4),
                proc(Trigger.ON_STRUCK, 35, "irons_spellbooks:heat_surge", 3));
        reg("bone",
                proc(Trigger.ON_STRIKE, 30, "iss_magicfromtheeast:bone_hands", 4),
                proc(Trigger.ON_STRUCK, 10, "irons_spellbooks:fortify", 3));
        reg("rain",
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:gust", 4),
                proc(Trigger.ON_STRUCK, 8,  "irons_spellbooks:oakskin", 3));
        reg("principle",
                proc(Trigger.ON_STRIKE, 40, "irons_spellbooks:guiding_bolt", 5));
        reg("dragon_armor",
                proc(Trigger.ON_STRIKE, 12, "irons_spellbooks:blaze_storm", 5),
                proc(Trigger.ON_STRUCK, 20, "irons_spellbooks:acid_orb", 3));

        // ==================== 盾牌 ====================
        reg("epiphany",
                proc(Trigger.ON_STRUCK, 25, "irons_spellbooks:shockwave", 5));
        reg("dragon_shield",
                proc(Trigger.ON_STRIKE, 12, "irons_spellbooks:blaze_storm", 5),
                proc(Trigger.ON_STRUCK, 20, "irons_spellbooks:acid_orb", 3));
        reg("dream_shield",
                proc(Trigger.ON_STRUCK, 10, "irons_spellbooks:shockwave", 3));
        reg("phoenix_shield",
                proc(Trigger.ON_STRIKE, 33, "irons_spellbooks:flaming_barrage", 6));

        // ==================== 武器 ====================
        reg("zephyr",
                proc(Trigger.ON_STRUCK, 10, "irons_spellbooks:gust", 2));
        reg("obedience",
                proc(Trigger.ON_KILL,   30, "irons_spellbooks:heat_surge", 4));
        reg("voice_of_reason",
                proc(Trigger.ON_STRIKE, 15, "irons_spellbooks:frostwave", 4),
                proc(Trigger.ON_STRIKE, 18, "irons_spellbooks:icicle", 5));
        reg("crescent_moon",
                proc(Trigger.ON_STRIKE, 15, "irons_spellbooks:lightning_lance", 4),
                proc(Trigger.ON_STRIKE, 16, "irons_spellbooks:shockwave", 4));
        reg("venom",
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:poison_splash", 5));
        reg("oath",
                proc(Trigger.ON_STRIKE, 30, "iss_magicfromtheeast:soul_burst", 4));
        reg("rift",
                proc(Trigger.ON_STRIKE, 26, "irons_spellbooks:gust", 5),
                proc(Trigger.ON_STRIKE, 24, "irons_spellbooks:frostwave", 5));
        reg("death",
                proc(Trigger.ON_STRIKE, 25, "irons_spellbooks:icicle", 5));
        reg("chaos",
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:frostwave", 5),
                proc(Trigger.ON_STRIKE, 18, "irons_spellbooks:ball_lightning", 5));
        reg("wind",
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:gust", 5),
                proc(Trigger.ON_CAST,   20, "irons_spellbooks:gust", 5),
                proc(Trigger.ON_STRUCK, 8,  "irons_spellbooks:oakskin", 3));
        reg("infinity",
                proc(Trigger.ON_STRIKE, 25, "irons_spellbooks:ball_lightning", 5),
                proc(Trigger.ON_CAST,   15, "irons_spellbooks:ball_lightning", 5));
        reg("ice",
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:frostwave", 6));
        reg("brand",
                proc(Trigger.ON_STRIKE, 50, "irons_spellbooks:magic_arrow", 6));
        reg("phoenix_weapon",
                proc(Trigger.ON_STRIKE, 33, "irons_spellbooks:flaming_barrage", 6),
                proc(Trigger.ON_CAST,   33, "irons_spellbooks:flaming_barrage", 6));
        reg("destruction",
                proc(Trigger.ON_STRIKE, 15, "irons_spellbooks:starfall", 5),
                proc(Trigger.ON_STRIKE, 20, "irons_spellbooks:frostwave", 5),
                proc(Trigger.ON_CAST,   15, "irons_spellbooks:starfall", 5));
        reg("rapture",
                proc(Trigger.ON_STRIKE, 18, "irons_spellbooks:magma_bomb", 6));
        reg("plague",
                proc(Trigger.ON_STRIKE, 25, "irons_spellbooks:poison_splash", 6));
        reg("doom",
                proc(Trigger.ON_STRIKE, 15, "irons_spellbooks:magma_bomb", 5),
                proc(Trigger.ON_CAST,   15, "irons_spellbooks:magma_bomb", 5));
        reg("pride",
                proc(Trigger.ON_STRUCK, 25, "irons_spellbooks:wall_of_fire", 5));
        reg("asylum",
                proc(Trigger.ON_STRIKE, 28, "irons_spellbooks:divine_smite", 6),
                proc(Trigger.ON_CAST,   28, "irons_spellbooks:divine_smite", 6));
        reg("breath_of_the_dying",
                proc(Trigger.ON_STRIKE, 35, "irons_spellbooks:poison_splash", 7));
    }

    private static void reg(String rwId, SpellProc... list) {
        PROCS.put(rwId, List.of(list));
    }

    private static SpellProc proc(Trigger trigger, double chance, String spellId, int level) {
        return new SpellProc(trigger, chance, ResourceLocation.parse(spellId), level);
    }

    public static List<SpellProc> getProcs(String runewordId) {
        if (runewordId == null) return List.of();
        return PROCS.getOrDefault(runewordId, List.of());
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (event.getNewDamage() <= 0) return;

        // 击中敌人 (仅限近战/箭矢，避免触发法术造成伤害后套娃)
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() != attacker
                && attacker.isAlive()) {
            var direct = event.getSource().getDirectEntity();
            if (direct == attacker || direct instanceof AbstractArrow) {
                handleTrigger(attacker, level, Trigger.ON_STRIKE);
            }
        }

        // 玩家受击
        if (event.getEntity() instanceof ServerPlayer victim && victim.isAlive()) {
            handleTrigger(victim, level, Trigger.ON_STRUCK);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer.isAlive()) {
            handleTrigger(killer, level, Trigger.ON_KILL);
        }
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (PROC_GUARD.contains(player.getUUID())) return;

        // 延后到下一个服务器 Tick 触发，避免干扰玩家当前正在执行的 MagicData
        level.getServer().execute(() -> {
            if (player.isAlive() && player.level() instanceof ServerLevel sl) {
                handleTrigger(player, sl, Trigger.ON_CAST);
            }
        });
    }

    private static void handleTrigger(ServerPlayer player, ServerLevel level, Trigger trigger) {
        UUID uuid = player.getUUID();
        if (!PROC_GUARD.add(uuid)) return;

        try {
            List<ItemStack> equipped = new ArrayList<>(6);
            equipped.add(player.getMainHandItem());
            equipped.add(player.getOffhandItem());
            for (ItemStack armor : player.getArmorSlots()) {
                equipped.add(armor);
            }

            long now = level.getGameTime();
            for (ItemStack stack : equipped) {
                if (stack.isEmpty()) continue;
                ModDataComponents.ActiveRuneword active = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
                if (active == null || !RunewordPurityHelper.isRunewordIntact(stack, active)) continue;

                List<SpellProc> procs = getProcs(active.id());
                if (procs.isEmpty()) continue;

                double purityScale = RunewordPurityHelper.getScale(active.purity());
                for (int i = 0; i < procs.size(); i++) {
                    SpellProc p = procs.get(i);
                    if (p.trigger() != trigger) continue;

                    String cdKey = "ex_proc_" + active.id() + "_" + i;
                    long readyAt = player.getPersistentData().getLong(cdKey);
                    if (now < readyAt) continue;

                    if (player.getRandom().nextDouble() * 100.0 <= p.chancePercent()) {
                        AbstractSpell spell = SpellRegistry.getSpell(p.spellId());
                        if (spell == null || spell == SpellRegistry.none()) continue;

                        int finalLevel = Math.max(1, (int) Math.round(p.baseLevel() * purityScale));
                        player.getPersistentData().putLong(cdKey, now + INTERNAL_COOLDOWN_TICKS);
                        try {
                            spell.onCast(level, finalLevel, player, CastSource.MOB, MagicData.getPlayerMagicData(player));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } finally {
            PROC_GUARD.remove(uuid);
        }
    }
}