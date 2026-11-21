package com.example.examplemod.runeword.set;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.MapMaker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;

/**
 * 固定4件绿装套装基类（NeoForge 1.21.1）
 * 规则：
 * - 1件：无套装加成
 * - 2件：各部位激活第1条绿字词缀
 * - 3件：各部位激活第2条绿字词缀
 * - 4件：激活唯一1条全局金字套装词缀
 */
public abstract class AbstractArmorSet {

    private static final int FULL_SET_COUNT = 4;
    private static final String COOLDOWN_ROOT_TAG = ExampleMod.MODID + "_set_cooldowns";

    private static final List<AbstractArmorSet> REGISTERED_SETS = new CopyOnWriteArrayList<>();
    private static final Map<Class<? extends Event>, List<BoundEventAction<?>>> EVENT_DISPATCHERS = new ConcurrentHashMap<>();
    private static final Set<Class<? extends Event>> REGISTERED_EVENT_CLASSES = ConcurrentHashMap.newKeySet();

    private static final Map<LivingEntity, EntitySnapshot> SERVER_SNAPSHOTS =
            new MapMaker().weakKeys().makeMap();

    private final String setId;
    private final String setName;
    private final List<SetPiece> pieces = new ArrayList<>(FULL_SET_COUNT);
    private BoundAffix setBonus;

    protected AbstractArmorSet(String setId, String setName) {
        this.setId = Objects.requireNonNull(setId);
        this.setName = Objects.requireNonNull(setName);
    }

    public static void register(AbstractArmorSet set) {
        if (set.pieces.size() != FULL_SET_COUNT) {
            throw new IllegalStateException("套装 [" + set.setId + "] 必须正好包含4个部位，当前: " + set.pieces.size());
        }
        if (set.setBonus == null) {
            throw new IllegalStateException("套装 [" + set.setId + "] 缺少4件唯一套装词缀 setBonus(...)");
        }
        if (REGISTERED_SETS.stream().anyMatch(s -> s.setId.equals(set.setId))) {
            return;
        }
        REGISTERED_SETS.add(set);

        for (SetPiece piece : set.pieces) {
            bindEvents(set, piece.affix2());
            bindEvents(set, piece.affix3());
        }
        bindEvents(set, set.setBonus);
    }

    private static void bindEvents(AbstractArmorSet set, BoundAffix boundAffix) {
        for (EventBinding<?> binding : boundAffix.affix().events) {
            registerEventBinding(set, boundAffix, binding);
        }
    }

    private static <E extends Event> void registerEventBinding(AbstractArmorSet set,
                                                               BoundAffix boundAffix,
                                                               EventBinding<E> binding) {
        EVENT_DISPATCHERS.computeIfAbsent(binding.eventClass(), k -> new CopyOnWriteArrayList<>())
                .add(new BoundEventAction<>(set, boundAffix, binding.extractor(), binding.action()));

        if (REGISTERED_EVENT_CLASSES.add(binding.eventClass())) {
            Class<E> eventClass = binding.eventClass();
            NeoForge.EVENT_BUS.addListener(eventClass, event -> dispatchEvent(eventClass, event));
        }
    }

    public static List<AbstractArmorSet> getRegisteredSets() {
        return Collections.unmodifiableList(REGISTERED_SETS);
    }

    // ==================== 固定4件套构建 DSL ====================

    protected void helmet(Item item, Affix tier2Affix, Affix tier3Affix) {
        piece("helmet", item, EquipmentSlot.HEAD, tier2Affix, tier3Affix);
    }

    protected void chestplate(Item item, Affix tier2Affix, Affix tier3Affix) {
        piece("chestplate", item, EquipmentSlot.CHEST, tier2Affix, tier3Affix);
    }

    protected void leggings(Item item, Affix tier2Affix, Affix tier3Affix) {
        piece("leggings", item, EquipmentSlot.LEGS, tier2Affix, tier3Affix);
    }

    protected void boots(Item item, Affix tier2Affix, Affix tier3Affix) {
        piece("boots", item, EquipmentSlot.FEET, tier2Affix, tier3Affix);
    }

    protected void piece(String pieceId, Item item, EquipmentSlot slot, Affix tier2Affix, Affix tier3Affix) {
        piece(pieceId, item, EnumSet.of(slot), tier2Affix, tier3Affix);
    }

    protected void piece(String pieceId, Item item, Set<EquipmentSlot> validSlots, Affix tier2Affix, Affix tier3Affix) {
        if (pieces.size() >= FULL_SET_COUNT) {
            throw new IllegalStateException("套装 [" + setId + "] 最多只能有4个部位");
        }
        if (pieces.stream().anyMatch(p -> p.pieceId().equals(pieceId))) {
            throw new IllegalArgumentException("套装 [" + setId + "] 部位ID重复: " + pieceId);
        }
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(tier2Affix, "tier2Affix");
        Objects.requireNonNull(tier3Affix, "tier3Affix");

        SetPiece dummy = new SetPiece(pieceId, item, EnumSet.copyOf(validSlots), null, null);
        BoundAffix b2 = bindAffix(pieceId + "_2p", 2, dummy, tier2Affix);
        BoundAffix b3 = bindAffix(pieceId + "_3p", 3, dummy, tier3Affix);
        SetPiece actual = new SetPiece(pieceId, item, EnumSet.copyOf(validSlots), b2, b3);
        // 重新绑定 ownerPiece 引用为 actual
        pieces.add(new SetPiece(
                pieceId, item, EnumSet.copyOf(validSlots),
                b2.withOwner(actual),
                b3.withOwner(actual)
        ));
    }

    protected void setBonus(Affix uniqueFullSetAffix) {
        if (this.setBonus != null) {
            throw new IllegalStateException("套装 [" + setId + "] 只能有唯一一条4件套装词缀");
        }
        this.setBonus = bindAffix("set_4p", FULL_SET_COUNT, null, Objects.requireNonNull(uniqueFullSetAffix));
    }

    private BoundAffix bindAffix(String scopeId, int requiredPieces, SetPiece ownerPiece, Affix affix) {
        List<BoundAttr> attrs = new ArrayList<>(affix.attrs.size());
        for (int i = 0; i < affix.attrs.size(); i++) {
            AttrBinding ab = affix.attrs.get(i);
            ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "set/" + setId + "/" + scopeId + "/" + i
            );
            attrs.add(new BoundAttr(ab, modId));
        }
        return new BoundAffix(affix, scopeId, requiredPieces, ownerPiece, List.copyOf(attrs));
    }

    // ==================== 基础查询 ====================

    public String getSetId() {
        return setId;
    }

    public String getSetName() {
        return setName;
    }

    public int getFullSetCount() {
        return FULL_SET_COUNT;
    }

    protected boolean appliesTo(LivingEntity entity) {
        return entity instanceof Player;
    }

    public int countEquippedPieces(LivingEntity entity) {
        int count = 0;
        for (SetPiece piece : pieces) {
            if (piece.isEquippedBy(entity)) count++;
        }
        return count;
    }

    public Optional<SetPiece> findPiece(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (SetPiece piece : pieces) {
            if (piece.matches(stack)) return Optional.of(piece);
        }
        return Optional.empty();
    }

    // ==================== 服务端快照与属性/药水同步 ====================

    private static EntitySnapshot getOrComputeSnapshot(LivingEntity entity) {
        return SERVER_SNAPSHOTS.computeIfAbsent(entity, e -> rebuildSnapshot(e, false));
    }

    private static EntitySnapshot rebuildSnapshot(LivingEntity entity, boolean isEquipmentChange) {
        EntitySnapshot previous = SERVER_SNAPSHOTS.get(entity);
        Map<AbstractArmorSet, ActiveSetState> activeSets = new LinkedHashMap<>();
        List<BoundTickAction> tickActions = new ArrayList<>();
        Map<Holder<MobEffect>, Integer> desiredEffects = new LinkedHashMap<>();

        for (AbstractArmorSet set : REGISTERED_SETS) {
            if (!set.appliesTo(entity)) continue;

            Set<SetPiece> wornPieces = new LinkedHashSet<>();
            for (SetPiece piece : set.pieces) {
                if (piece.isEquippedBy(entity)) wornPieces.add(piece);
            }

            int count = wornPieces.size();

            // 1. 部位词条（2件/3件）
            for (SetPiece piece : set.pieces) {
                boolean worn = wornPieces.contains(piece);
                syncBoundAffix(entity, set, piece.affix2(), worn && count >= 2, tickActions, desiredEffects);
                syncBoundAffix(entity, set, piece.affix3(), worn && count >= 3, tickActions, desiredEffects);
            }

            // 2. 唯一4件套装词缀
            syncBoundAffix(entity, set, set.setBonus, count >= FULL_SET_COUNT, tickActions, desiredEffects);

            if (count > 0) {
                activeSets.put(set, new ActiveSetState(count, wornPieces));
            }
        }

        if (isEquipmentChange && previous != null) {
            for (Holder<MobEffect> oldEffect : previous.appliedEffects().keySet()) {
                if (!desiredEffects.containsKey(oldEffect)) {
                    MobEffectInstance current = entity.getEffect(oldEffect);
                    if (current != null && current.isAmbient() && !current.isVisible() && current.getDuration() <= 340) {
                        entity.removeEffect(oldEffect);
                    }
                }
            }
        }

        applyDesiredEffects(entity, desiredEffects);

        EntitySnapshot next = new EntitySnapshot(activeSets, tickActions, desiredEffects);
        SERVER_SNAPSHOTS.put(entity, next);
        return next;
    }

    private static void syncBoundAffix(LivingEntity entity,
                                       AbstractArmorSet set,
                                       BoundAffix bound,
                                       boolean active,
                                       List<BoundTickAction> tickActions,
                                       Map<Holder<MobEffect>, Integer> desiredEffects) {
        for (BoundAttr attr : bound.attrs()) {
            attr.sync(entity, active);
        }
        if (!active) return;

        for (EffectBinding eff : bound.affix().effects) {
            eff.resolve().ifPresent(holder -> desiredEffects.merge(holder, eff.amplifier(), Math::max));
        }
        for (TickBinding tick : bound.affix().ticks) {
            tickActions.add(new BoundTickAction(set, bound, tick));
        }
    }

    private static void applyDesiredEffects(LivingEntity entity, Map<Holder<MobEffect>, Integer> desiredEffects) {
        for (Map.Entry<Holder<MobEffect>, Integer> entry : desiredEffects.entrySet()) {
            Holder<MobEffect> holder = entry.getKey();
            int amplifier = entry.getValue();
            MobEffectInstance existing = entity.getEffect(holder);

            if (existing != null && (existing.getAmplifier() > amplifier || existing.getDuration() > 400)) {
                continue;
            }
            if (existing == null || existing.getAmplifier() < amplifier || existing.getDuration() <= 260) {
                entity.addEffect(new MobEffectInstance(holder, 320, amplifier, true, false, true));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void dispatchEvent(Class<E> eventClass, E event) {
        List<BoundEventAction<?>> list = EVENT_DISPATCHERS.get(eventClass);
        if (list == null || list.isEmpty()) return;

        for (BoundEventAction<?> raw : list) {
            BoundEventAction<E> entry = (BoundEventAction<E>) raw;
            LivingEntity entity = entry.extractor().apply(event);
            if (entity == null || entity.level().isClientSide()) continue;
            if (!entry.set().appliesTo(entity)) continue;

            EntitySnapshot snapshot = getOrComputeSnapshot(entity);
            ActiveSetState state = snapshot.activeSets().get(entry.set());
            if (state == null) continue;

            BoundAffix bound = entry.boundAffix();
            if (state.equippedCount() < bound.requiredPieces()) continue;
            if (bound.ownerPiece() != null && !state.wornPieces().contains(bound.ownerPiece())) continue;

            AffixContext ctx = new AffixContext(entry.set(), entity, state.equippedCount(), bound.scopeId());
            entry.action().handle(event, entity, ctx);
        }
    }

    // ==================== 服务端基础事件 ====================

    @EventBusSubscriber(modid = ExampleMod.MODID)
    public static final class CommonEvents {

        @SubscribeEvent
        public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide()) return;
            rebuildSnapshot(entity, true);
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;
            if (event.getEntity() instanceof LivingEntity living) {
                for (AbstractArmorSet set : REGISTERED_SETS) {
                    if (set.appliesTo(living)) {
                        rebuildSnapshot(living, false);
                        break;
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;

            EntitySnapshot snapshot = getOrComputeSnapshot(player);
            if (snapshot.activeSets().isEmpty()) return;

            if (player.tickCount % 40 == 0 && !snapshot.appliedEffects().isEmpty()) {
                applyDesiredEffects(player, snapshot.appliedEffects());
            }

            for (BoundTickAction boundTick : snapshot.tickActions()) {
                if (player.tickCount % boundTick.tick().intervalTicks() == 0) {
                    ActiveSetState state = snapshot.activeSets().get(boundTick.set());
                    if (state == null) continue;
                    AffixContext ctx = new AffixContext(
                            boundTick.set(), player, state.equippedCount(), boundTick.boundAffix().scopeId()
                    );
                    boundTick.tick().action().accept(player, ctx);
                }
            }
        }
    }

    // ==================== 客户端 Tooltip 渲染 ====================

    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            for (AbstractArmorSet set : REGISTERED_SETS) {
                Optional<SetPiece> pieceOpt = set.findPiece(stack);
                if (pieceOpt.isEmpty()) continue;

                SetPiece hovered = pieceOpt.get();
                List<Component> tooltip = event.getToolTip();
                Player player = event.getEntity();
                int equippedCount = player != null ? set.countEquippedPieces(player) : 0;
                boolean wornHovered = player != null && hovered.isEquippedBy(player);
                boolean previewLocked = Screen.hasShiftDown();
                boolean hasLockedContent = false;

                // 1. 名称变绿
                if (!tooltip.isEmpty()) {
                    tooltip.set(0, tooltip.get(0).copy().withStyle(ChatFormatting.GREEN));
                }

                // 2. 该部位的 2件 / 3件 绿字词缀
                List<Component> pieceLines = new ArrayList<>(2);
                hasLockedContent |= appendAffixLine(pieceLines, hovered.affix2(), wornHovered && equippedCount >= 2, "(2件)", ChatFormatting.GREEN, previewLocked);
                hasLockedContent |= appendAffixLine(pieceLines, hovered.affix3(), wornHovered && equippedCount >= 3, "(3件)", ChatFormatting.GREEN, previewLocked);

                // 3. 唯一 4件 金字套装词缀
                List<Component> globalLines = new ArrayList<>(1);
                hasLockedContent |= appendAffixLine(globalLines, set.setBonus, equippedCount >= FULL_SET_COUNT, "(全套)", ChatFormatting.GOLD, previewLocked);

                if (!pieceLines.isEmpty() || !globalLines.isEmpty()) {
                    tooltip.add(Component.empty());
                    tooltip.addAll(pieceLines);
                    tooltip.addAll(globalLines);
                }

                // 4. 底部套装清单
                tooltip.add(Component.empty());
                tooltip.add(Component.literal(set.getSetName()).withStyle(ChatFormatting.GOLD));
                for (SetPiece piece : set.pieces) {
                    boolean worn = player != null && piece.isEquippedBy(player);
                    ChatFormatting color = worn ? ChatFormatting.GREEN : ChatFormatting.RED;
                    tooltip.add(Component.literal("  ").append(piece.getDisplayName()).withStyle(color));
                }

                if (hasLockedContent && !previewLocked) {
                    tooltip.add(Component.literal("[按住 Shift 预览未解锁套装属性]").withStyle(ChatFormatting.DARK_GRAY));
                }
                break;
            }
        }

        private static boolean appendAffixLine(List<Component> out,
                                               BoundAffix bound,
                                               boolean active,
                                               String lockedTag,
                                               ChatFormatting activeColor,
                                               boolean previewLocked) {
            Affix affix = bound.affix();
            if (!affix.isAvailable()) return false;

            if (active) {
                out.add(Component.literal(affix.description()).withStyle(activeColor));
                return false;
            }
            if (previewLocked) {
                out.add(Component.literal(affix.description() + " " + lockedTag).withStyle(ChatFormatting.DARK_GRAY));
            }
            return true;
        }
    }

    // ==================== 独立词缀工厂 Affix ====================

    public static final class Affix {
        private final String description;
        private final List<AttrBinding> attrs;
        private final List<EffectBinding> effects;
        private final List<TickBinding> ticks;
        private final List<EventBinding<?>> events;
        private final BooleanSupplier available;

        private Affix(String description,
                      List<AttrBinding> attrs,
                      List<EffectBinding> effects,
                      List<TickBinding> ticks,
                      List<EventBinding<?>> events,
                      BooleanSupplier available) {
            this.description = description;
            this.attrs = attrs;
            this.effects = effects;
            this.ticks = ticks;
            this.events = events;
            this.available = available;
        }

        public String description() {
            return description;
        }

        public boolean isAvailable() {
            return available.getAsBoolean();
        }

        // --- 属性词缀 ---

        public static Affix attr(Holder<Attribute> attribute, double amount, Operation operation, String description) {
            AttrBinding binding = new AttrBinding(() -> Optional.of(attribute), amount, operation);
            return new Affix(description, List.of(binding), List.of(), List.of(), List.of(), () -> true);
        }

        public static Affix attr(String attributeId, double amount, Operation operation, String description) {
            ResourceLocation id = ResourceLocation.parse(attributeId);
            AttrBinding binding = new AttrBinding(
                    () -> BuiltInRegistries.ATTRIBUTE.getHolder(id).map(h -> h),
                    amount, operation
            );
            return new Affix(description, List.of(binding), List.of(), List.of(), List.of(), binding::isAvailable);
        }

        // --- 药水词缀 ---

        public static Affix effect(Holder<MobEffect> effect, int amplifier, String description) {
            EffectBinding binding = new EffectBinding(() -> Optional.of(effect), amplifier);
            return new Affix(description, List.of(), List.of(binding), List.of(), List.of(), () -> true);
        }

        public static Affix effect(String effectId, int amplifier, String description) {
            ResourceLocation id = ResourceLocation.parse(effectId);
            EffectBinding binding = new EffectBinding(
                    () -> BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> h),
                    amplifier
            );
            return new Affix(description, List.of(), List.of(binding), List.of(), List.of(), binding::isAvailable);
        }

        // --- 常用机制词缀 ---

        public static Affix immuneTo(TagKey<DamageType> damageTag, String description) {
            return onIncomingDamage(description, (event, victim, ctx) -> {
                if (event.getSource().is(damageTag)) {
                    event.setCanceled(true);
                }
            });
        }

        public static Affix magnet(double radius, boolean pauseOnSneak, String description) {
            return onTick(4, description, (player, ctx) -> {
                if (pauseOnSneak && player.isShiftKeyDown()) return;
                AABB box = player.getBoundingBox().inflate(radius, 2.5, radius);
                List<ItemEntity> items = player.level().getEntitiesOfClass(
                        ItemEntity.class, box,
                        item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty()
                );
                if (items.isEmpty()) return;
                Vec3 targetPos = player.position().add(0.0, 0.5, 0.0);
                for (ItemEntity item : items) {
                    Vec3 delta = targetPos.subtract(item.position());
                    if (delta.lengthSqr() > 0.01) {
                        Vec3 pull = delta.normalize().scale(0.35);
                        item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(pull));
                        item.hasImpulse = true;
                    }
                }
            });
        }

        public static Affix onLowHealth(float hpRatioThreshold, long cooldownTicks, String description,
                                        AffixEventAction<LivingIncomingDamageEvent, LivingEntity> action) {
            return onIncomingDamage(description, (event, victim, ctx) -> {
                if (event.isCanceled()) return;
                float afterHp = victim.getHealth() - event.getAmount();
                if (afterHp <= victim.getMaxHealth() * hpRatioThreshold && ctx.tryConsumeCooldown("lowhp", cooldownTicks)) {
                    action.handle(event, victim, ctx);
                }
            });
        }

        // --- 事件词缀 ---

        public static Affix onTick(int intervalTicks, String description, BiConsumer<Player, AffixContext> action) {
            return new Affix(description, List.of(), List.of(), List.of(new TickBinding(Math.max(1, intervalTicks), action)), List.of(), () -> true);
        }

        public static Affix onIncomingDamage(String description, AffixEventAction<LivingIncomingDamageEvent, LivingEntity> action) {
            return onEvent(LivingIncomingDamageEvent.class, LivingIncomingDamageEvent::getEntity, description, action);
        }

        public static Affix onAttack(String description, AffixEventAction<LivingDamageEvent.Post, LivingEntity> action) {
            return onEvent(
                    LivingDamageEvent.Post.class,
                    e -> e.getSource().getEntity() instanceof LivingEntity living ? living : null,
                    description, action
            );
        }

        public static Affix onKill(String description, AffixEventAction<LivingDeathEvent, LivingEntity> action) {
            return onEvent(
                    LivingDeathEvent.class,
                    e -> e.getSource().getEntity() instanceof LivingEntity living ? living : null,
                    description, action
            );
        }

        public static <E extends Event> Affix onEvent(Class<E> eventClass,
                                                      Function<E, LivingEntity> extractor,
                                                      String description,
                                                      AffixEventAction<E, LivingEntity> action) {
            return new Affix(description, List.of(), List.of(), List.of(), List.of(new EventBinding<>(eventClass, extractor, action)), () -> true);
        }

        /**
         * 将多个子效果合并为「视觉上的一条词缀」（例如：夜视 + 急迫 I，或 免疫冻伤 + 地形效率）
         */
        public static Affix all(String description, Affix... parts) {
            List<AttrBinding> attrs = new ArrayList<>();
            List<EffectBinding> effects = new ArrayList<>();
            List<TickBinding> ticks = new ArrayList<>();
            List<EventBinding<?>> events = new ArrayList<>();
            for (Affix part : parts) {
                attrs.addAll(part.attrs);
                effects.addAll(part.effects);
                ticks.addAll(part.ticks);
                events.addAll(part.events);
            }
            return new Affix(
                    description,
                    List.copyOf(attrs),
                    List.copyOf(effects),
                    List.copyOf(ticks),
                    List.copyOf(events),
                    () -> Arrays.stream(parts).allMatch(Affix::isAvailable)
            );
        }
    }

    // ==================== 内部数据结构 ====================

    @FunctionalInterface
    public interface AffixEventAction<E extends Event, T extends LivingEntity> {
        void handle(E event, T entity, AffixContext ctx);
    }

    public record AffixContext(AbstractArmorSet set, LivingEntity entity, int equippedCount, String scopeId) {
        public boolean tryConsumeCooldown(String key, long cooldownTicks) {
            long now = entity.level().getGameTime();
            CompoundTag root = entity.getPersistentData();
            CompoundTag cds = root.getCompound(COOLDOWN_ROOT_TAG);
            String fullKey = set.getSetId() + ":" + scopeId + ":" + key;
            if (now < cds.getLong(fullKey)) {
                return false;
            }
            cds.putLong(fullKey, now + cooldownTicks);
            root.put(COOLDOWN_ROOT_TAG, cds);
            return true;
        }
    }

    public record SetPiece(
            String pieceId,
            Item displayItem,
            Set<EquipmentSlot> validSlots,
            BoundAffix affix2,
            BoundAffix affix3
    ) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(displayItem);
        }

        public boolean isEquippedBy(LivingEntity entity) {
            for (EquipmentSlot slot : validSlots) {
                if (matches(entity.getItemBySlot(slot))) return true;
            }
            return false;
        }

        public Component getDisplayName() {
            return displayItem.getDescription();
        }
    }

    public record BoundAffix(
            Affix affix,
            String scopeId,
            int requiredPieces,
            SetPiece ownerPiece,
            List<BoundAttr> attrs
    ) {
        private BoundAffix withOwner(SetPiece owner) {
            return new BoundAffix(affix, scopeId, requiredPieces, owner, attrs);
        }
    }

    private static final class AttrBinding {
        private final Supplier<Optional<Holder<Attribute>>> resolver;
        private final double amount;
        private final Operation operation;
        private Optional<Holder<Attribute>> cached;

        private AttrBinding(Supplier<Optional<Holder<Attribute>>> resolver, double amount, Operation operation) {
            this.resolver = resolver;
            this.amount = amount;
            this.operation = operation;
        }

        private Optional<Holder<Attribute>> resolve() {
            if (cached == null) cached = resolver.get();
            return cached;
        }

        private boolean isAvailable() {
            return resolve().isPresent();
        }
    }

    public record BoundAttr(AttrBinding binding, ResourceLocation modifierId) {
        public void sync(LivingEntity entity, boolean active) {
            Optional<Holder<Attribute>> holderOpt = binding.resolve();
            if (holderOpt.isEmpty()) return;
            AttributeInstance instance = entity.getAttribute(holderOpt.get());
            if (instance == null) return;
            if (active) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId, binding.amount, binding.operation));
            } else {
                instance.removeModifier(modifierId);
            }
        }
    }

    private static final class EffectBinding {
        private final Supplier<Optional<Holder<MobEffect>>> resolver;
        private final int amplifier;
        private Optional<Holder<MobEffect>> cached;

        private EffectBinding(Supplier<Optional<Holder<MobEffect>>> resolver, int amplifier) {
            this.resolver = resolver;
            this.amplifier = amplifier;
        }

        private Optional<Holder<MobEffect>> resolve() {
            if (cached == null) cached = resolver.get();
            return cached;
        }

        private boolean isAvailable() {
            return resolve().isPresent();
        }

        private int amplifier() {
            return amplifier;
        }
    }

    private record TickBinding(int intervalTicks, BiConsumer<Player, AffixContext> action) {}

    private record EventBinding<E extends Event>(
            Class<E> eventClass,
            Function<E, LivingEntity> extractor,
            AffixEventAction<E, LivingEntity> action
    ) {}

    private record BoundEventAction<E extends Event>(
            AbstractArmorSet set,
            BoundAffix boundAffix,
            Function<E, LivingEntity> extractor,
            AffixEventAction<E, LivingEntity> action
    ) {}

    private record BoundTickAction(AbstractArmorSet set, BoundAffix boundAffix, TickBinding tick) {}

    private record ActiveSetState(int equippedCount, Set<SetPiece> wornPieces) {}

    private record EntitySnapshot(
            Map<AbstractArmorSet, ActiveSetState> activeSets,
            List<BoundTickAction> tickActions,
            Map<Holder<MobEffect>, Integer> appliedEffects
    ) {}
}