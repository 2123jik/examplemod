package com.example.examplemod.engine;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public class WandExecutionEngine {
    public static final int BOOK_UI_SLOTS = 6; // 左边：法术书槽位
    public static final int WAND_UI_SLOTS = WandEditMenu.WAND_SLOTS; // 中/右：副手/主手法杖槽位

    // 按 "UUID:connected" 和 "UUID:sword_offhand" 分别维护独立游标
    private static final Map<String, Integer> POINTERS = new ConcurrentHashMap<>();

    private static String key(ServerPlayer player, String suffix) {
        return player.getUUID() + ":" + suffix;
    }

    public static int getPointer(ServerPlayer player) {
        return POINTERS.getOrDefault(key(player, "connected"), 0);
    }

    public static void setPointer(ServerPlayer player, int ptr) {
        POINTERS.put(key(player, "connected"), Math.max(0, ptr));
    }
    // 默认一套施法冷却：1 秒 = 20 ticks
    public static final int DEFAULT_CAST_COOLDOWN_TICKS = 20;

    /**
     * 计算施法速度对默认冷却的缩放倍率：
     * x <= 0.5 时线性衰减至 0.5；x > 0.5 时 C1 平滑过渡到反比例衰减 0.25 / x
     */
    public static float computeCastSpeedCooldownScale(ServerPlayer player) {
        double attr = player.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION);
        double bonus = attr - 1.0D;

        if (bonus < 0.0D) {
            return (float) (1.0D / Math.max(0.1D, 1.0D + bonus));
        }
        if (bonus <= 0.5D) {
            return (float) (1.0D - bonus);
        }
        return (float) (0.25D / bonus);
    }

    /**
     * 收集本次三段链中参与施法的载体物品（法术书、副手法杖、主手法杖/魔剑）
     */
    private static Set<Item> collectCarrierItems(ServerPlayer player) {
        Set<Item> carriers = new LinkedHashSet<>();

        ItemStack bookStack = Utils.getPlayerSpellbookStack(player);
        if (bookStack != null && !bookStack.isEmpty()) {
            carriers.add(bookStack.getItem());
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.has(TestWandItem.WAND_SLOTS.get())) {
            carriers.add(offHand.getItem());
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            if (mainHand.has(TestWandItem.WAND_SLOTS.get())
                    || (!(mainHand.getItem() instanceof Scroll) && ISpellContainer.isSpellContainer(mainHand))) {
                carriers.add(mainHand.getItem());
            }
        }
        return carriers;
    }
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String prefix = event.getEntity().getUUID().toString();
        POINTERS.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * 接管 ISS 原版单格施法（法术书 SPELLBOOK 与 主手魔剑 SWORD 统一走三段链）
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTakeOverIssCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (event.getCastSource() == CastSource.SPELLBOOK || event.getCastSource() == CastSource.SWORD) {
            event.setCanceled(true);
            castConnected(player, false);
        }
    }

    /**
     * 完整三段链：法术书(左) -> 副手法杖(中) -> 主手魔剑/法杖(右)
     */
    public static List<WandCompiler.WandSlot> buildConnectedSlots(ServerPlayer player) {
        List<WandCompiler.WandSlot> connected = new ArrayList<>();

        ItemStack bookStack = Utils.getPlayerSpellbookStack(player);
        if (bookStack != null && !bookStack.isEmpty()) {
            appendIssSpellContainer(bookStack, connected);
            appendContainerSlots(bookStack, BOOK_UI_SLOTS, connected);
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand != bookStack && offHand.has(TestWandItem.WAND_SLOTS.get())) {
            appendContainerSlots(offHand, WAND_UI_SLOTS, connected);
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand != bookStack) {
            if (mainHand.has(TestWandItem.WAND_SLOTS.get())) {
                appendContainerSlots(mainHand, WAND_UI_SLOTS, connected);
            } else if (!(mainHand.getItem() instanceof Scroll) && ISpellContainer.isSpellContainer(mainHand)) {
                appendIssSpellContainer(mainHand, connected);
            }
        }

        return connected;
    }

    public static WandCompiler.CompiledWand compileConnectedPassives(ServerPlayer player) {
        return WandCompiler.compile(buildConnectedSlots(player), null);
    }

    public static void castConnected(ServerPlayer player) {
        castConnected(player, false);
    }

    public static void castConnected(ServerPlayer player, boolean silentOnFail) {
        List<WandCompiler.WandSlot> slots = buildConnectedSlots(player);
        if (slots.isEmpty()) return;
        executeRibbonWithKey(player, slots, "connected", silentOnFail);
    }

    private static void executeRibbonWithKey(
            ServerPlayer player, List<WandCompiler.WandSlot> slots, String suffix, boolean silentOnFail
    ) {
        Set<Item> carriers = collectCarrierItems(player);
        for (Item item : carriers) {
            if (player.getCooldowns().isOnCooldown(item)) {
                return;
            }
        }

        WandCompiler.CompiledWand compiledInfo = WandCompiler.compile(slots, null);
        if ("connected".equals(suffix)) {
            WandPassiveTickHandler.updateHeldWand(player, compiledInfo);
        }

        int size = slots.size();
        String pKey = key(player, suffix);
        int ptr = POINTERS.getOrDefault(pKey, 0);
        if (ptr < 0 || ptr >= size) ptr = 0;

        int[] lvlBoosts = new int[size];
        for (int i = 0; i < size; i++) {
            var s = slots.get(i);
            if (!s.isActiveSpell()) {
                var spec = ModifierCatalog.get(s.id());
                if (spec != null && spec.category() == ModifierCatalog.Category.LEVEL_BOOST) {
                    int boost = spec.getCount(s.baseLevel());
                    if (i - 1 >= 0) lvlBoosts[i - 1] += boost;
                    if (i + 1 < size) lvlBoosts[i + 1] += boost;
                }
            }
        }

        WandCompiler.CastModifiers stackMods = new WandCompiler.CastModifiers();
        if (compiledInfo.wideAmplifyPayload() != null) {
            stackMods.mergeFrom(compiledInfo.wideAmplifyPayload(), 1);
        }

        int neededActives = 1;
        boolean salvoApplied = false;
        int repeatCount = 1;
        List<WandCompiler.CastNode> releaseBatch = new ArrayList<>();

        int steps = 0;
        int cursor = ptr;

        while (steps < size && releaseBatch.size() < neededActives) {
            WandCompiler.WandSlot slot = slots.get(cursor);
            int slotLvl = Math.max(1, slot.baseLevel() + lvlBoosts[cursor]);

            if (slot.isActiveSpell()) {
                List<WandCompiler.TriggerEntry> triggers = new ArrayList<>();
                int lookAhead = (cursor + 1) % size;
                int consumedSteps = 1;

                while (steps + consumedSteps < size && !slots.get(lookAhead).isActiveSpell()) {
                    var trigSpec = ModifierCatalog.get(slots.get(lookAhead).id());
                    if (trigSpec == null) break;
                    String timing = switch (trigSpec.category()) {
                        case TRIGGER_EXPIRE -> "EXPIRE";
                        case TRIGGER_TICK -> "TICK";
                        case TRIGGER_HIT -> "HIT";
                        case TRIGGER_ENTANGLE -> "ENTANGLE";
                        default -> null;
                    };
                    if (timing == null) break;

                    // 允许触发器与目标法术之间夹增强词条
                    int trigLvl = Math.max(1, slots.get(lookAhead).baseLevel() + lvlBoosts[lookAhead]);
                    var childMods = new WandCompiler.CastModifiers();
                    childMods.mergeFrom(trigSpec, trigLvl);

                    int scan = (lookAhead + 1) % size;
                    int extraUsed = 1;
                    boolean foundTarget = false;
                    while (steps + consumedSteps + extraUsed < size) {
                        var tSlot = slots.get(scan);
                        int tLvl = Math.max(1, tSlot.baseLevel() + lvlBoosts[scan]);
                        if (tSlot.isActiveSpell()) {
                            var childNode = new WandCompiler.CastNode(tSlot.id(), tLvl, childMods, List.of(), 1);
                            triggers.add(new WandCompiler.TriggerEntry(
                                    timing, trigSpec.getCount(trigLvl), trigSpec.getSecondary(trigLvl), childNode));
                            consumedSteps += 1 + extraUsed;
                            lookAhead = (scan + 1) % size;
                            foundTarget = true;
                            break;
                        }
                        var innerSpec = ModifierCatalog.get(tSlot.id());
                        if (innerSpec != null && innerSpec.category() == ModifierCatalog.Category.STAT_MODIFIER) {
                            childMods.mergeFrom(innerSpec, tLvl);
                            scan = (scan + 1) % size;
                            extraUsed++;
                        } else {
                            break;
                        }
                    }
                    if (!foundTarget) break;
                }

                for (int r = 0; r < repeatCount; r++) {
                    releaseBatch.add(new WandCompiler.CastNode(
                            slot.id(), slotLvl, stackMods.copy(), triggers, 0
                    ));
                }
                // 仅当是纯连发(repeatCount>1)且不是齐射时，多重施法才占满本轮
                if (repeatCount > 1 && neededActives == 1) {
                    cursor = (cursor + consumedSteps) % size;
                    steps += consumedSteps;
                    break;
                }

                cursor = (cursor + consumedSteps) % size;
                steps += consumedSteps;
                continue;
            }

            var spec = ModifierCatalog.get(slot.id());
            if (spec != null) {
                switch (spec.category()) {
                    case SALVO -> {
                        int count = spec.getCount(slotLvl);
                        neededActives = salvoApplied ? (neededActives + count) : Math.max(1, count);
                        salvoApplied = true;
                        stackMods.mergeFrom(spec, slotLvl);
                    }
                    case MULTICAST -> {
                        repeatCount += spec.getCount(slotLvl);
                        stackMods.manaMult *= spec.getManaMult(slotLvl);
                        stackMods.flatAddMana += spec.getFlatAddMana(slotLvl);
                    }
                    case POST_SLOT -> {
                        // 破界石后的法术由被动充能自动放，主动游标直接跳过破界石 + 其右侧紧邻目标
                        int skip = (steps + 1 < size) ? 2 : 1;
                        cursor = (cursor + skip) % size;
                        steps += skip;
                        continue;
                    }
                    case STAT_MODIFIER -> stackMods.mergeFrom(spec, slotLvl);
                    default -> {}
                }
            }

            cursor = (cursor + 1) % size;
            steps++;
        }

        if (releaseBatch.isEmpty()) {
            POINTERS.put(pKey, cursor);
            return;
        }

        int n = releaseBatch.size();
        for (int i = 0; i < n; i++) {
            var m = releaseBatch.get(i).modifiers();
            m.orbitPhaseRad = (float) (2.0 * Math.PI * i / n);
            float totalSpread = Math.max(18.0F * Math.max(1, n - 1), m.scatterDeg);
            float step = (n > 1) ? (totalSpread / (n - 1)) : 0.0F;
            m.yawOffsetDeg = (n > 1) ? ((i - (n - 1) / 2.0F) * step) : 0.0F;
        }

        boolean anyCastSuccess = false;
        for (WandCompiler.CastNode node : releaseBatch) {
            if (executeSingleNode(player, node, null, null)) {
                anyCastSuccess = true;
            } else {
                if (!silentOnFail) {
                    player.displayClientMessage(
                            Component.literal("法力不足或条件不满足，队列中止（当前格 #" + (ptr + 1) + "）")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                break;
            }
        }

        // 至少成功放出一发，才推进指针并触发通知与冷却
        if (anyCastSuccess) {
            POINTERS.put(pKey, cursor);
            WandPassiveTickHandler.notifyWandCast(player);

            if (!carriers.isEmpty()) {
                float speedScale = computeCastSpeedCooldownScale(player);
                int cdTicks = Math.max(1, Math.round(DEFAULT_CAST_COOLDOWN_TICKS * speedScale * compiledInfo.cooldownMult()));
                for (Item item : carriers) {
                    player.getCooldowns().addCooldown(item, cdTicks);
                }
            }
        }
    }

    public static boolean executeSingleNode(
            ServerPlayer player,
            WandCompiler.CastNode node,
            @Nullable Vec3 overrideCenterPos,
            @Nullable UUID followEntityUuid
    ) {
        AbstractSpell spell = SpellRegistry.getSpell(node.spellId());
        if (spell == null) return false;

        int effectiveLevel = spell.getLevelFor(node.effectiveBaseLevel(), player);
        var mods = node.modifiers();
        float rawCost = spell.getManaCost(effectiveLevel) * mods.manaMult + mods.flatAddMana;

        float oldYaw = player.getYRot();
        if (mods.yawOffsetDeg != 0.0F) {
            player.setYRot(oldYaw + mods.yawOffsetDeg);
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        // 先检查前置条件，失败直接退出且不扣蓝
        try {
            if (!spell.checkPreCastConditions(player.level(), effectiveLevel, player, magicData)) {
                return false;
            }
            if (!ManaBudgetService.tryConsumeMana(player, rawCost)) {
                return false;
            }

            SpellTransformEngine.pushCapture(new SpellTransformEngine.CaptureContext(
                    player.getUUID(), overrideCenterPos, followEntityUuid, node
            ));
            try {
                spell.onCast(player.level(), effectiveLevel, player, CastSource.COMMAND, magicData);
            } finally {
                SpellTransformEngine.popCapture();
            }
        } finally {
            if (mods.yawOffsetDeg != 0.0F) {
                player.setYRot(oldYaw);
            }
        }
        return true;
    }

    private static void appendIssSpellContainer(ItemStack stack, List<WandCompiler.WandSlot> out) {
        if (!ISpellContainer.isSpellContainer(stack)) return;
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) return;

        for (SpellSlot slot : container.getActiveSpells()) {
            SpellData sd = slot.spellData();
            if (sd != null
                    && !sd.equals(SpellData.EMPTY)
                    && sd.getSpell() != SpellRegistry.none()
                    && sd.getLevel() > 0) {
                out.add(new WandCompiler.WandSlot(true, sd.getSpell().getSpellResource(), sd.getLevel()));
            }
        }
    }

    private static void appendContainerSlots(ItemStack containerItem, int maxPhysical, List<WandCompiler.WandSlot> out) {
        ItemContainerContents contents = containerItem.getOrDefault(TestWandItem.WAND_SLOTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> raw = NonNullList.withSize(maxPhysical, ItemStack.EMPTY);
        contents.copyInto(raw);

        int[] usage = TestWandItem.computeSlotUsageAndMax(raw, maxPhysical);
        int maxCap = usage[1];
        int running = 0;

        for (ItemStack s : raw) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(s)) {
                if (running + 1 > maxCap) break;
                SpellData sd = ISpellContainer.get(s).getSpellAtIndex(0);
                if (!sd.equals(SpellData.EMPTY)
                        && sd.getSpell() != SpellRegistry.none()
                        && sd.getLevel() > 0) {
                    running += 1;
                    out.add(new WandCompiler.WandSlot(true, sd.getSpell().getSpellResource(), sd.getLevel()));
                }
            } else if (s.getItem() instanceof TestWandItem.ModifierGemItem) {
                var spec = TestWandItem.getGemSpec(s);
                if (spec != null) {
                    if (running + spec.slotCost() > maxCap) break;
                    running += spec.slotCost();
                    out.add(new WandCompiler.WandSlot(false, s.get(TestWandItem.MODIFIER_ID.get()), TestWandItem.getGemLevel(s)));
                }
            }
        }
    }
}