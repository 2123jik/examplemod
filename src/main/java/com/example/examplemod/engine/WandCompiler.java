package com.example.examplemod.engine;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public class WandCompiler {

    public static final int MAX_DEPTH = 6;

    public record WandSlot(
            boolean isActiveSpell,
            ResourceLocation id,
            int baseLevel
    ) {}

    public static class CastModifiers {
        public float manaMult = 1.0F;
        public float flatAddMana = 0.0F;
        public float damageMult = 1.0F;
        public float critAdd = 0.0F;
        public float radiusMult = 1.0F;
        public float speedAdd = 0.0F;
        public float speedMult = 1.0F;
        public float durationAddSec = 0.0F;
        public float durationMult = 1.0F;
        public float scatterDeg = 0.0F;
        public boolean uniformScatter = false;
        public float yawOffsetDeg = 0.0F;
        public float orbitPhaseRad = 0.0F;

        public int pierceAdd = 0;
        public int bounceAdd = 0;
        public int refractAdd = 0;
        public int splitCount = 0;
        public float hoverSec = 0.0F;
        public boolean orbit = false;
        public boolean freeOrbit = false;
        public float orbitRadius = 3.5F;
        public boolean cursorHoming = false;
        public float entityHomingDeg = 0.0F;
        public float selfHomingDeg = 0.0F;
        public boolean endTeleport = false;
        public boolean midTeleport = false;
        public boolean fallDown = false;
        public boolean reverseCast = false;
        public boolean remoteCast = false;
        public float scaleMult = 1.0F;

        public final Set<String> hitTags = new HashSet<>();

        public void mergeFrom(ModifierCatalog.ModSpec spec, int level) {
            manaMult *= spec.getManaMult(level);
            flatAddMana += spec.getFlatAddMana(level);
            damageMult *= spec.getDamageMult(level);
            critAdd += spec.critAdd() * level;
            radiusMult *= (1.0F + (spec.radiusMult() - 1.0F) * level);
            speedAdd += spec.speedAdd() * level;
            speedMult *= spec.speedMult();
            durationAddSec += spec.durationAddSec() * level;
            durationMult *= spec.durationMult();
            scatterDeg = Math.max(0.0F, scatterDeg + spec.scatterAddDeg());
            if (spec.uniformScatter()) uniformScatter = true;

            switch (spec.specialTag()) {
                case "PIERCE" -> pierceAdd += spec.getCount(level);
                case "BOUNCE" -> bounceAdd += spec.getCount(level);
                case "REFRACT" -> refractAdd += spec.getCount(level);
                case "SPLIT" -> splitCount = Math.max(splitCount, spec.getCount(level));
                case "HOVER" -> hoverSec += spec.getSecondary(level);
                case "ORBIT" -> { orbit = true; orbitRadius = spec.getSecondary(level); }
                case "FREE_ORBIT" -> { orbit = true; freeOrbit = true; orbitRadius = spec.getSecondary(level); }
                case "CURSOR_HOMING" -> cursorHoming = true;
                case "ENTITY_HOMING" -> entityHomingDeg = Math.max(entityHomingDeg, spec.getSecondary(level));
                case "SELF_HOMING" -> selfHomingDeg = Math.max(selfHomingDeg, spec.getSecondary(level));
                case "END_TP" -> endTeleport = true;
                case "MID_TP" -> midTeleport = true;
                case "FALL" -> fallDown = true;
                case "REVERSE" -> reverseCast = true;
                case "REMOTE_CAST" -> remoteCast = true;
                case "SCALE_UP" -> scaleMult *= (1.0F + 0.8F * level);
                case "IMPLODE" -> damageMult += Math.max(0.0F, (1.0F - radiusMult) * 2.0F);
                case "LINK_PREV", "HIT_SLOW", "HIT_POISON", "HIT_FREEZE",
                     "END_THUNDER", "HIT_MAX_HP_BURN", "CRIT_PULL", "HIT_COLLAPSE" -> hitTags.add(spec.specialTag());
            }
        }

        public CastModifiers copy() {
            CastModifiers c = new CastModifiers();
            c.manaMult = manaMult; c.flatAddMana = flatAddMana; c.damageMult = damageMult;
            c.critAdd = critAdd; c.radiusMult = radiusMult; c.speedAdd = speedAdd; c.speedMult = speedMult;
            c.durationAddSec = durationAddSec; c.durationMult = durationMult; c.scatterDeg = scatterDeg;
            c.uniformScatter = uniformScatter; c.yawOffsetDeg = yawOffsetDeg; c.orbitPhaseRad = orbitPhaseRad;
            c.pierceAdd = pierceAdd; c.bounceAdd = bounceAdd; c.refractAdd = refractAdd; c.splitCount = splitCount;
            c.hoverSec = hoverSec; c.orbit = orbit; c.freeOrbit = freeOrbit; c.orbitRadius = orbitRadius;
            c.cursorHoming = cursorHoming; c.entityHomingDeg = entityHomingDeg; c.selfHomingDeg = selfHomingDeg;
            c.endTeleport = endTeleport; c.midTeleport = midTeleport; c.fallDown = fallDown;
            c.reverseCast = reverseCast; c.remoteCast = remoteCast; c.scaleMult = scaleMult;
            c.hitTags.addAll(hitTags);
            return c;
        }
    }

    public record TriggerEntry(
            String timing,
            int maxTriggers,
            float intervalSec,
            CastNode childNode
    ) {}

    public record CastNode(
            ResourceLocation spellId,
            int effectiveBaseLevel,
            CastModifiers modifiers,
            List<TriggerEntry> triggers,
            int depth
    ) {}

    public record PostSlotTrigger(
            String chargeType,
            float chargeRate,
            CastNode targetNode
    ) {}

    public record CompiledWand(
            float flatReserve,
            float percentReserve,
            float cooldownMult,
            float bonusManaRegen,
            boolean stopNaturalRegen,
            float postChargeEfficiencyBonus,
            boolean hasArcaneBarrier,
            boolean hasGuardianSpirit,
            boolean hasResonanceRune,
            boolean hasPrismCore,
            boolean hasFlyingSword,
            int chargeModeMaxStacks,
            List<CastNode> rootCastBatch,
            List<PostSlotTrigger> postSlotTriggers,
            @Nullable ModifierCatalog.ModSpec wideAmplifyPayload
    ) {}

    public static CompiledWand compile(List<WandSlot> rawSlots, @Nullable ModifierCatalog.ModSpec externalWideMod) {
        float flatReserve = 0.0F;
        float percentReserve = 0.0F;
        float cdMult = 1.0F;
        float bonusRegen = 0.0F;
        boolean stopRegen = false;
        float postBoost = 0.0F;
        boolean barrier = false, guardian = false, resonance = false, prism = false, sword = false;
        int chargeStacks = 0;
        ModifierCatalog.ModSpec widePayload = null;

        int[] levelBoosts = new int[rawSlots.size()];
        for (int i = 0; i < rawSlots.size(); i++) {
            WandSlot s = rawSlots.get(i);
            if (!s.isActiveSpell()) {
                var spec = ModifierCatalog.get(s.id());
                if (spec != null && spec.category() == ModifierCatalog.Category.LEVEL_BOOST) {
                    int boost = spec.getCount(s.baseLevel());
                    if (i - 1 >= 0) levelBoosts[i - 1] += boost;
                    if (i + 1 < rawSlots.size()) levelBoosts[i + 1] += boost;
                }
            }
        }

        CastModifiers globalMods = new CastModifiers();
        if (externalWideMod != null) globalMods.mergeFrom(externalWideMod, 1);

        for (int i = 0; i < rawSlots.size(); i++) {
            WandSlot s = rawSlots.get(i);
            if (s.isActiveSpell()) continue;
            var spec = ModifierCatalog.get(s.id());
            if (spec == null) continue;

            int lvl = Math.max(1, s.baseLevel() + levelBoosts[i]);
            flatReserve += spec.getFlatReserve(lvl);
            percentReserve += spec.getPercentReserve(lvl);

            switch (spec.category()) {
                case PASSIVE_COOLDOWN -> cdMult *= spec.getSecondary(lvl);
                case PASSIVE_REGEN -> bonusRegen += spec.getSecondary(lvl);
                case PASSIVE_BARRIER -> barrier = true;
                case PASSIVE_GUARDIAN -> guardian = true;
                case PASSIVE_RESONANCE -> resonance = true;
                case PASSIVE_PRISM -> prism = true;
                case PASSIVE_FLYING_SWORD -> sword = true;
                case PASSIVE_CHARGE_MODE -> chargeStacks = Math.max(chargeStacks, spec.getCount(lvl));
                case PASSIVE_CONVERTER -> { stopRegen = true; postBoost += spec.getSecondary(lvl); }
                case PASSIVE_EXPAND -> globalMods.damageMult *= spec.getDamageMult(lvl);
                case PASSIVE_VINE -> bonusRegen += spec.getSecondary(lvl);
                case WIDE_AMPLIFY -> {
                    if (i + 1 < rawSlots.size() && !rawSlots.get(i + 1).isActiveSpell()) {
                        var next = ModifierCatalog.get(rawSlots.get(i + 1).id());
                        if (next != null && next.category() != ModifierCatalog.Category.WIDE_AMPLIFY) {
                            widePayload = next;
                        }
                    }
                }
                default -> {}
            }
        }

        List<CastNode> rootBatch = new ArrayList<>();
        List<PostSlotTrigger> postTriggers = new ArrayList<>();
        parseRange(rawSlots, levelBoosts, 0, 1, globalMods, rootBatch, postTriggers, 0);

        return new CompiledWand(flatReserve, percentReserve, cdMult, bonusRegen, stopRegen, postBoost,
                barrier, guardian, resonance, prism, sword, chargeStacks, rootBatch, postTriggers, widePayload);
    }

    private static int parseRange(
            List<WandSlot> slots, int[] lvlBoosts, int startIdx, int countToRead,
            CastModifiers inherited, List<CastNode> outBatch, List<PostSlotTrigger> outPost, int depth
    ) {
        if (depth >= MAX_DEPTH || startIdx >= slots.size()) return startIdx;

        int idx = startIdx;
        int collected = 0;
        CastModifiers mods = inherited.copy();

        while (idx < slots.size() && collected < countToRead) {
            WandSlot slot = slots.get(idx);
            int slotLvl = Math.max(1, slot.baseLevel() + lvlBoosts[idx]);

            if (slot.isActiveSpell()) {
                List<TriggerEntry> triggers = new ArrayList<>();
                int nextIdx = idx + 1;

                while (nextIdx < slots.size() && !slots.get(nextIdx).isActiveSpell()) {
                    WandSlot trigSlot = slots.get(nextIdx);
                    var trigSpec = ModifierCatalog.get(trigSlot.id());
                    if (trigSpec == null) break;

                    String timing = switch (trigSpec.category()) {
                        case TRIGGER_EXPIRE -> "EXPIRE";
                        case TRIGGER_TICK -> "TICK";
                        case TRIGGER_HIT -> "HIT";
                        case TRIGGER_ENTANGLE -> "ENTANGLE";
                        default -> null;
                    };
                    if (timing == null) break;

                    int trigLvl = Math.max(1, trigSlot.baseLevel() + lvlBoosts[nextIdx]);
                    CastModifiers childMods = new CastModifiers();
                    childMods.mergeFrom(trigSpec, trigLvl);
                    List<CastNode> childList = new ArrayList<>();
                    nextIdx = parseRange(slots, lvlBoosts, nextIdx + 1, 1, childMods, childList, outPost, depth + 1);
                    if (!childList.isEmpty()) {
                        triggers.add(new TriggerEntry(timing, trigSpec.getCount(trigLvl), trigSpec.getSecondary(trigLvl), childList.get(0)));
                    }
                }

                outBatch.add(new CastNode(slot.id(), slotLvl, mods.copy(), triggers, depth));
                mods = inherited.copy();
                collected++;
                idx = nextIdx;
                continue;
            }

            var spec = ModifierCatalog.get(slot.id());
            if (spec == null) { idx++; continue; }

            switch (spec.category()) {
                case SALVO -> {
                    int startSize = outBatch.size();
                    idx = parseRange(slots, lvlBoosts, idx + 1, spec.getCount(slotLvl), mods, outBatch, outPost, depth + 1);
                    int n = outBatch.size() - startSize;
                    for (int k = 0; k < n; k++) {
                        CastModifiers m = outBatch.get(startSize + k).modifiers();
                        m.orbitPhaseRad = (float) (2.0 * Math.PI * k / n);
                        float step = m.uniformScatter ? (Math.max(30f, m.scatterDeg) / Math.max(1, n - 1)) : 18.0F;
                        m.yawOffsetDeg = (n > 1) ? ((k - (n - 1) / 2.0F) * step) : 0.0F;
                    }
                    collected++;
                }
                case MULTICAST -> {
                    List<CastNode> target = new ArrayList<>();
                    idx = parseRange(slots, lvlBoosts, idx + 1, 1, mods, target, outPost, depth + 1);
                    if (!target.isEmpty()) {
                        int repeat = 1 + spec.getCount(slotLvl);
                        float multMana = spec.getManaMult(slotLvl);
                        for (int r = 0; r < repeat; r++) {
                            CastNode baseNode = target.get(0);
                            CastModifiers copyM = baseNode.modifiers().copy();
                            copyM.manaMult *= multMana;
                            outBatch.add(new CastNode(baseNode.spellId(), baseNode.effectiveBaseLevel(), copyM, baseNode.triggers(), depth));
                        }
                    }
                    collected++;
                }
                case MIMIC -> {
                    List<CastNode> peek = new ArrayList<>();
                    parseRange(slots, lvlBoosts, idx + 1, 1, mods, peek, outPost, depth + 1);
                    if (!peek.isEmpty()) outBatch.add(peek.get(0));
                    idx++;
                    collected++;
                }
                case POST_SLOT -> {
                    List<CastNode> postNodes = new ArrayList<>();
                    idx = parseRange(slots, lvlBoosts, idx + 1, 1, new CastModifiers(), postNodes, outPost, depth + 1);
                    if (!postNodes.isEmpty()) {
                        outPost.add(new PostSlotTrigger(spec.specialTag(), spec.getSecondary(slotLvl), postNodes.get(0)));
                    }
                }
                case STAT_MODIFIER -> {
                    mods.mergeFrom(spec, slotLvl);
                    idx++;
                }
                default -> idx++;
            }
        }
        return idx;
    }
}