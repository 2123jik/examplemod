package com.example.examplemod.util;

import dev.xkmc.l2backpack.content.bag.AbstractBag;
import dev.xkmc.l2backpack.content.common.BaseBagItem;
import dev.xkmc.l2backpack.content.remote.common.StorageContainer;
import dev.xkmc.l2backpack.content.remote.dimensional.DimensionalItem;
import dev.xkmc.l2backpack.content.remote.player.EnderBackpackItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public final class EnderBackpackStorage {

    private static final int MAX_DEPTH = 12;
    private static final int MAX_NODES = 128;
    private static final int MAX_TOTAL_SLOTS = 32768;

    private static final int MAX_COMPRESS_BATCHES = 64;
    private static final int MAX_UNPACK_STEPS = 64;
    private static final int MAX_UNPACK_DEPTH = 4;

    /*
     * 全局配方静态缓存，避免每次 new Session 重复查询 RecipeManager。
     * 当服务器执行 /reload 时通过事件清空缓存。
     */
    private static final Map<Item, List<CompressionRule>> GLOBAL_COMPRESS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, Optional<ReverseRule>> GLOBAL_REVERSE_CACHE = new ConcurrentHashMap<>();

    private EnderBackpackStorage() {
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        GLOBAL_COMPRESS_CACHE.clear();
        GLOBAL_REVERSE_CACHE.clear();
    }

    public static boolean compact(ServerPlayer player) {
        Session session = open(player);
        if (session == null) {
            return false;
        }

        session.compact();
        return session.commit();
    }

    public static int extract(ServerPlayer player, ItemStack target, int requested) {
        if (target.isEmpty() || requested <= 0 || isStorageItem(target)) {
            return 0;
        }

        Session session = open(player);
        if (session == null) {
            return 0;
        }

        int extracted = session.extract(target, requested);
        if (extracted <= 0) {
            return 0;
        }

        return session.commit() ? extracted : 0;
    }

    public static boolean fillMerchant(ServerPlayer player, MerchantMenu menu, int selectedIndex) {
        if (player.containerMenu != menu
                || !menu.stillValid(player)
                || selectedIndex < 0
                || selectedIndex >= menu.getOffers().size()) {
            return false;
        }

        MerchantOffer offer = menu.getOffers().get(selectedIndex);
        if (offer.isOutOfStock()) {
            return false;
        }

        ItemStack beforeA = menu.getSlot(0).getItem();
        ItemStack beforeB = menu.getSlot(1).getItem();

        ItemCost costA = offer.getItemCostA();
        Optional<ItemCost> costB = offer.getItemCostB();

        ItemStack priceA = offer.getCostA();
        ItemStack priceB = offer.getCostB();

        if (priceA.isEmpty()) {
            return false;
        }

        if (!beforeA.isEmpty() && !costA.test(beforeA)) {
            return false;
        }

        if (costB.isPresent()) {
            if (!beforeB.isEmpty() && !costB.get().test(beforeB)) {
                return false;
            }
        } else if (!beforeB.isEmpty()) {
            return false;
        }

        int missingA = Math.max(0, priceA.getCount() - beforeA.getCount());
        int missingB = costB.isPresent() ? Math.max(0, priceB.getCount() - beforeB.getCount()) : 0;

        if (missingA == 0 && missingB == 0) {
            return false;
        }

        Session session = open(player);
        if (session == null) {
            return false;
        }

        ItemStack afterA = prepareMerchantPayment(session, menu, 0, costA, priceA, beforeA);
        if (afterA == null) {
            return false;
        }

        ItemStack afterB;
        if (costB.isPresent()) {
            afterB = prepareMerchantPayment(session, menu, 1, costB.get(), priceB, beforeB);
            if (afterB == null) {
                return false;
            }
        } else {
            afterB = ItemStack.EMPTY;
        }

        if (!offer.satisfiedBy(afterA, afterB)) {
            return false;
        }

        if (player.containerMenu != menu
                || !menu.stillValid(player)
                || selectedIndex >= menu.getOffers().size()
                || menu.getOffers().get(selectedIndex) != offer
                || offer.isOutOfStock()
                || !equal(priceA, offer.getCostA())
                || !equal(priceB, offer.getCostB())
                || !equal(beforeA, menu.getSlot(0).getItem())
                || !equal(beforeB, menu.getSlot(1).getItem())) {
            return false;
        }

        if (!session.commit()) {
            return false;
        }

        menu.getSlot(0).set(afterA);
        menu.getSlot(1).set(afterB);
        menu.slotsChanged(menu.getSlot(0).container);
        menu.broadcastChanges();

        return true;
    }

    private static ItemStack prepareMerchantPayment(
            Session session,
            MerchantMenu menu,
            int slotIndex,
            ItemCost cost,
            ItemStack price,
            ItemStack existing
    ) {
        int required = price.getCount();
        int present = existing.isEmpty() ? 0 : existing.getCount();

        if (present >= required) {
            return existing.copy();
        }

        int missing = required - present;
        List<ItemStack> candidates = new ArrayList<>();

        if (!existing.isEmpty()) {
            addMerchantCandidate(candidates, existing, cost);
        } else {
            List<ItemStack> storedKinds = session.kinds();
            for (ItemStack kind : storedKinds) {
                addMerchantCandidate(candidates, kind, cost);
            }

            for (ItemStack kind : storedKinds) {
                ItemStack current = kind;
                for (int depth = 0; depth < MAX_UNPACK_DEPTH; depth++) {
                    Optional<ReverseRule> reverse = session.getReverseRule(current);
                    if (reverse.isEmpty()) {
                        break;
                    }
                    current = reverse.get().output();
                    addMerchantCandidate(candidates, current, cost);
                }
            }
            addMerchantCandidate(candidates, price, cost);
        }

        for (ItemStack candidate : candidates) {
            int limit = Math.min(
                    candidate.getMaxStackSize(),
                    menu.getSlot(slotIndex).getMaxStackSize(candidate)
            );

            if (required > limit || !menu.getSlot(slotIndex).mayPlace(candidate)) {
                continue;
            }

            List<List<ItemStack>> saved = session.checkpoint();
            int extracted = session.extract(candidate, missing);

            if (extracted == missing) {
                return candidate.copyWithCount(present + extracted);
            }

            session.restore(saved);
        }

        return null;
    }

    private static void addMerchantCandidate(List<ItemStack> candidates, ItemStack candidate, ItemCost cost) {
        if (candidate.isEmpty() || isStorageItem(candidate) || !cost.test(candidate)) {
            return;
        }

        for (ItemStack existing : candidates) {
            if (same(existing, candidate)) {
                return;
            }
        }
        candidates.add(candidate.copyWithCount(1));
    }

    public static boolean hasEquippedEnderBackpack(ServerPlayer player) {
        boolean[] found = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            inventory.getCurios().forEach((identifier, handler) -> {
                var stacks = handler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    if (stacks.getStackInSlot(slot).getItem() instanceof EnderBackpackItem) {
                        found[0] = true;
                        break;
                    }
                }
            });
        });
        return found[0];
    }

    private static Session open(ServerPlayer player) {
        if (!hasEquippedEnderBackpack(player)) {
            return null;
        }

        Session session = new Session(player);
        session.scanRemote(player.getEnderChestInventory(), 0);
        return session.complete ? session : null;
    }

    public static boolean isStorageItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof EnderBackpackItem
                || stack.getItem() instanceof DimensionalItem
                || stack.getItem() instanceof BaseBagItem
                || stack.getItem() instanceof AbstractBag);
    }

    public static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    public static boolean equal(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.getCount() == b.getCount() && same(a, b);
    }

    private static boolean equalLists(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!equal(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> copyList(List<ItemStack> source) {
        List<ItemStack> result = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            result.add(stack.copy());
        }
        return result;
    }

    private static long count(List<ItemStack> items, ItemStack target) {
        long result = 0;
        for (ItemStack stack : items) {
            if (same(stack, target)) {
                result += stack.getCount();
            }
        }
        return result;
    }

    private static int remove(List<ItemStack> items, ItemStack target, int requested) {
        int removed = 0;
        for (ItemStack stack : items) {
            if (!same(stack, target)) {
                continue;
            }

            int take = Math.min(requested - removed, stack.getCount());
            stack.shrink(take);
            removed += take;

            if (removed == requested) {
                break;
            }
        }
        return removed;
    }

    private static boolean standard(CraftingRecipe recipe) {
        return recipe.getClass() == ShapedRecipe.class || recipe.getClass() == ShapelessRecipe.class;
    }

    private static boolean hasRemainders(CraftingRecipe recipe, CraftingInput input) {
        for (ItemStack remainder : recipe.getRemainingItems(input)) {
            if (!remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static CraftingInput square(ItemStack source, int width) {
        List<ItemStack> items = new ArrayList<>(width * width);
        for (int i = 0; i < width * width; i++) {
            items.add(source.copyWithCount(1));
        }
        return CraftingInput.of(width, width, items);
    }

    private record CompressionRule(Item inputItem, Item outputItem, int cost, ItemStack outputTemplate) {}
    private record ReverseRule(ItemStack output, int count) {}

    private static final class Node {
        private final Container remote;
        private final Node parent;
        private final int parentSlot;
        private final ItemStack bagOriginal;
        private final List<ItemStack> original;
        private List<ItemStack> items;

        private Node(Container remote, Node parent, int parentSlot, ItemStack bagOriginal, List<ItemStack> original) {
            this.remote = remote;
            this.parent = parent;
            this.parentSlot = parentSlot;
            this.bagOriginal = bagOriginal.copy();
            this.original = copyList(original);
            this.items = copyList(original);
        }

        private int limit(ItemStack stack) {
            if (remote == null) {
                return stack.getMaxStackSize();
            }
            return Math.min(stack.getMaxStackSize(), remote.getMaxStackSize());
        }

        private boolean accepts(int slot, ItemStack stack) {
            return remote == null || remote.canPlaceItem(slot, stack);
        }
    }

    private static final class Session {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final List<Node> nodes = new ArrayList<>();
        private final Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        private boolean complete = true;
        private int totalSlots;
        private int unpackSteps = MAX_UNPACK_STEPS;

        private Session(ServerPlayer player) {
            this.player = player;
            this.level = player.serverLevel();
        }

        private boolean allowNode(int depth, int slots) {
            if (!complete) return false;
            if (depth > MAX_DEPTH || nodes.size() >= MAX_NODES || slots < 0 || (long) totalSlots + slots > MAX_TOTAL_SLOTS) {
                complete = false;
                return false;
            }
            totalSlots += slots;
            return true;
        }

        private void scanRemote(Container container, int depth) {
            if (!complete || !visited.add(container)) return;
            if (!allowNode(depth, container.getContainerSize())) return;

            List<ItemStack> contents = new ArrayList<>(container.getContainerSize());
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                contents.add(container.getItem(slot).copy());
            }

            Node node = new Node(container, null, -1, ItemStack.EMPTY, contents);
            nodes.add(node);
            scanChildren(node, depth);
        }

        private void scanOrdinary(Node parent, int slot, ItemStack bag, int depth) {
            List<ItemStack> contents = BaseBagItem.getItems(bag);
            if (!allowNode(depth, contents.size())) return;

            Node node = new Node(null, parent, slot, bag, contents);
            nodes.add(node);
            scanChildren(node, depth);
        }

        private void scanChildren(Node parent, int depth) {
            for (int slot = 0; slot < parent.items.size(); slot++) {
                if (!complete) return;
                ItemStack stack = parent.items.get(slot);
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof EnderBackpackItem) {
                    scanRemote(player.getEnderChestInventory(), depth + 1);
                } else if (stack.getItem() instanceof DimensionalItem dim) {
                    Optional<StorageContainer> storage = dim.getContainer(stack, level);
                    storage.ifPresent(storageContainer -> scanRemote(storageContainer.get(), depth + 1));
                } else if (stack.getItem() instanceof BaseBagItem && !(stack.getItem() instanceof AbstractBag)) {
                    scanOrdinary(parent, slot, stack, depth + 1);
                }
            }
        }

        private List<List<ItemStack>> checkpoint() {
            List<List<ItemStack>> result = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                result.add(copyList(node.items));
            }
            return result;
        }

        private void restore(List<List<ItemStack>> saved) {
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).items = saved.get(i);
            }
        }

        private List<ItemStack> kinds() {
            List<ItemStack> result = new ArrayList<>();
            for (Node node : nodes) {
                for (ItemStack stack : node.items) {
                    if (stack.isEmpty() || isStorageItem(stack)) continue;
                    boolean found = false;
                    for (ItemStack existing : result) {
                        if (same(existing, stack)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        result.add(stack.copyWithCount(1));
                    }
                }
            }
            return result;
        }

        private long movable(ItemStack target) {
            long result = 0;
            for (Node node : nodes) {
                long amount = count(node.items, target);
                if (amount > 1) {
                    result += amount - 1;
                }
            }
            return result;
        }

        private int removeAcross(ItemStack target, int requested, boolean keepOne) {
            int removed = 0;
            for (Node node : nodes) {
                long amount = count(node.items, target);
                long available = Math.max(0L, amount - (keepOne ? 1L : 0L));
                int take = (int) Math.min((long) requested - removed, available);
                if (take > 0) {
                    removed += remove(node.items, target, take);
                }
                if (removed == requested) break;
            }
            return removed;
        }

        private List<Node> preferredNodes(ItemStack target) {
            List<Node> order = new ArrayList<>(nodes);
            order.sort(Comparator.comparingLong((Node node) -> count(node.items, target)).reversed());
            return order;
        }

        private int insertInto(Node node, ItemStack template, int requested) {
            int remaining = requested;
            for (int slot = 0; slot < node.items.size(); slot++) {
                ItemStack existing = node.items.get(slot);
                if (!same(existing, template)) continue;

                ItemStack probe = template.copyWithCount(1);
                if (!node.accepts(slot, probe)) continue;

                int room = node.limit(template) - existing.getCount();
                if (room <= 0) continue;

                int moved = Math.min(room, remaining);
                existing.grow(moved);
                remaining -= moved;
                if (remaining == 0) return requested;
            }

            for (int slot = 0; slot < node.items.size(); slot++) {
                if (!node.items.get(slot).isEmpty()) continue;

                ItemStack probe = template.copyWithCount(1);
                if (!node.accepts(slot, probe)) continue;

                int limit = node.limit(template);
                if (limit <= 0) continue;

                int moved = Math.min(limit, remaining);
                node.items.set(slot, template.copyWithCount(moved));
                remaining -= moved;
                if (remaining == 0) return requested;
            }
            return requested - remaining;
        }

        private boolean insertAcross(ItemStack stack) {
            int remaining = stack.getCount();
            for (Node node : preferredNodes(stack)) {
                remaining -= insertInto(node, stack, remaining);
                if (remaining == 0) return true;
            }
            return false;
        }

        // ---------- 带有全局缓存的配方计算 ----------

        private List<CompressionRule> getCompressionRules(ItemStack source) {
            Item item = source.getItem();
            List<CompressionRule> cached = GLOBAL_COMPRESS_CACHE.get(item);
            if (cached != null) {
                return cached;
            }

            List<CompressionRule> rules = new ArrayList<>();
            for (int width : new int[]{3, 2}) {
                int cost = width * width;
                CraftingInput input = square(source, width);
                Optional<RecipeHolder<CraftingRecipe>> holder = level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level);

                if (holder.isEmpty()) continue;

                CraftingRecipe recipe = holder.get().value();
                if (!standard(recipe) || recipe.getIngredients().size() != cost || hasRemainders(recipe, input)) {
                    continue;
                }

                ItemStack output = recipe.assemble(input, level.registryAccess());
                if (output.isEmpty() || output.getCount() != 1 || same(source, output) || isStorageItem(output)) {
                    continue;
                }

                Optional<ReverseRule> reverse = getReverseRule(output);
                if (reverse.isPresent() && reverse.get().count() == cost && same(reverse.get().output(), source)) {
                    rules.add(new CompressionRule(item, output.getItem(), cost, output.copyWithCount(1)));
                }
            }

            GLOBAL_COMPRESS_CACHE.put(item, rules);
            return rules;
        }

        private Optional<ReverseRule> getReverseRule(ItemStack compressed) {
            Item item = compressed.getItem();
            Optional<ReverseRule> cached = GLOBAL_REVERSE_CACHE.get(item);
            if (cached != null) {
                return cached;
            }

            Optional<ReverseRule> computed = computeReverseRule(compressed);
            GLOBAL_REVERSE_CACHE.put(item, computed);
            return computed;
        }

        private Optional<ReverseRule> computeReverseRule(ItemStack compressed) {
            if (isStorageItem(compressed)) return Optional.empty();

            CraftingInput reverseInput = CraftingInput.of(1, 1, List.of(compressed.copyWithCount(1)));
            Optional<RecipeHolder<CraftingRecipe>> reverseHolder = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, reverseInput, level);

            if (reverseHolder.isEmpty()) return Optional.empty();

            CraftingRecipe reverse = reverseHolder.get().value();
            if (!standard(reverse) || hasRemainders(reverse, reverseInput)) return Optional.empty();

            ItemStack restored = reverse.assemble(reverseInput, level.registryAccess());
            if (restored.isEmpty() || isStorageItem(restored) || same(restored, compressed)) return Optional.empty();

            int amount = restored.getCount();
            if (amount != 4 && amount != 9) return Optional.empty();

            CraftingInput forwardInput = square(restored, amount == 4 ? 2 : 3);
            Optional<RecipeHolder<CraftingRecipe>> forwardHolder = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, forwardInput, level);

            if (forwardHolder.isEmpty()) return Optional.empty();

            CraftingRecipe forward = forwardHolder.get().value();
            if (!standard(forward) || forward.getIngredients().size() != amount || hasRemainders(forward, forwardInput)) {
                return Optional.empty();
            }

            ItemStack rebuilt = forward.assemble(forwardInput, level.registryAccess());
            if (rebuilt.getCount() != 1 || !same(rebuilt, compressed)) return Optional.empty();

            return Optional.of(new ReverseRule(restored.copyWithCount(1), amount));
        }

        private void compact() {
            int batches = 0;
            while (batches < MAX_COMPRESS_BATCHES) {
                boolean progressed = false;
                for (ItemStack source : kinds()) {
                    for (CompressionRule rule : getCompressionRules(source)) {
                        if (compressOneBatch(source, rule)) {
                            batches++;
                            progressed = true;
                            break;
                        }
                    }
                    if (progressed) break;
                }
                if (!progressed) break;
            }

            for (ItemStack kind : kinds()) {
                if (getReverseRule(kind).isPresent()) {
                    consolidate(kind);
                }
            }
        }

        private boolean compressOneBatch(ItemStack source, CompressionRule rule) {
            int max = (int) Math.min(movable(source) / rule.cost(), rule.outputTemplate().getMaxStackSize());
            for (int amount = max; amount > 0; amount--) {
                List<List<ItemStack>> saved = checkpoint();
                int required = amount * rule.cost();
                int removed = removeAcross(source, required, true);

                if (removed == required && insertAcross(rule.outputTemplate().copyWithCount(amount))) {
                    return true;
                }
                restore(saved);
            }
            return false;
        }

        private void consolidate(ItemStack kind) {
            List<Node> order = preferredNodes(kind);
            if (order.isEmpty()) return;

            Node destination = order.get(0);
            if (count(destination.items, kind) <= 0) return;

            for (Node source : order) {
                if (source == destination) continue;
                while (count(source.items, kind) > 1) {
                    int requested = (int) Math.min(count(source.items, kind) - 1, kind.getMaxStackSize());
                    int moved = insertInto(destination, kind, requested);
                    if (moved <= 0) break;
                    remove(source.items, kind, moved);
                }
            }
        }

        private int extract(ItemStack target, int requested) {
            int extracted = removeAcross(target, requested, false);

            while (extracted < requested && unpackSteps > 0) {
                boolean progressed = false;

                search:
                for (Node node : nodes) {
                    for (int slot = 0; slot < node.items.size(); slot++) {
                        ItemStack source = node.items.get(slot);
                        if (source.isEmpty() || isStorageItem(source) || same(source, target)
                                || !canReach(source, target, MAX_UNPACK_DEPTH, new ArrayList<>())) {
                            continue;
                        }

                        List<List<ItemStack>> saved = checkpoint();
                        ItemStack one = source.copyWithCount(1);
                        source.shrink(1);

                        List<ItemStack> leftovers = new ArrayList<>();
                        int gained = expandOne(one, target, requested - extracted, MAX_UNPACK_DEPTH, leftovers);

                        boolean fits = gained > 0;
                        if (fits) {
                            for (ItemStack remainder : leftovers) {
                                if (!insertAcross(remainder)) {
                                    fits = false;
                                    break;
                                }
                            }
                        }

                        if (fits) {
                            extracted += gained;
                            progressed = true;
                            break search;
                        }
                        restore(saved);
                    }
                }
                if (!progressed) break;
            }
            return extracted;
        }

        private boolean canReach(ItemStack source, ItemStack target, int depth, List<ItemStack> path) {
            if (same(source, target)) return true;
            if (depth <= 0 || isStorageItem(source)) return false;

            for (ItemStack visitedItem : path) {
                if (same(visitedItem, source)) return false;
            }

            Optional<ReverseRule> optional = getReverseRule(source);
            if (optional.isEmpty()) return false;

            path.add(source.copyWithCount(1));
            boolean result = canReach(optional.get().output(), target, depth - 1, path);
            path.remove(path.size() - 1);
            return result;
        }

        private int expandOne(ItemStack source, ItemStack target, int requested, int depth, List<ItemStack> leftovers) {
            if (same(source, target)) {
                if (requested > 0) return 1;
                leftovers.add(source.copyWithCount(1));
                return 0;
            }

            if (requested <= 0 || depth <= 0 || unpackSteps <= 0) {
                leftovers.add(source.copyWithCount(1));
                return 0;
            }

            Optional<ReverseRule> optional = getReverseRule(source);
            if (optional.isEmpty()) {
                leftovers.add(source.copyWithCount(1));
                return 0;
            }

            ReverseRule rule = optional.get();
            if (!canReach(rule.output(), target, depth - 1, new ArrayList<>())) {
                leftovers.add(source.copyWithCount(1));
                return 0;
            }

            unpackSteps--;
            if (same(rule.output(), target)) {
                int taken = Math.min(requested, rule.count());
                int rest = rule.count() - taken;
                if (rest > 0) leftovers.add(rule.output().copyWithCount(rest));
                return taken;
            }

            int extracted = 0;
            for (int i = 0; i < rule.count(); i++) {
                if (extracted >= requested || unpackSteps <= 0) {
                    leftovers.add(rule.output().copyWithCount(rule.count() - i));
                    break;
                }
                extracted += expandOne(rule.output(), target, requested - extracted, depth - 1, leftovers);
            }
            return extracted;
        }

        private boolean commit() {
            if (!complete) return false;

            for (int i = nodes.size() - 1; i >= 0; i--) {
                Node node = nodes.get(i);
                if (node.remote != null || equalLists(node.original, node.items)) continue;

                ItemStack updatedBag = node.bagOriginal.copy();
                BaseBagItem.setItems(updatedBag, copyList(node.items));
                node.parent.items.set(node.parentSlot, updatedBag);
            }

            for (Node node : nodes) {
                if (node.remote == null) continue;
                if (node.remote.getContainerSize() != node.original.size()) return false;

                for (int slot = 0; slot < node.original.size(); slot++) {
                    if (!equal(node.remote.getItem(slot), node.original.get(slot))) {
                        return false;
                    }
                }
            }

            boolean changed = false;
            for (Node node : nodes) {
                if (node.remote == null) continue;
                boolean containerChanged = false;

                for (int slot = 0; slot < node.items.size(); slot++) {
                    if (equal(node.original.get(slot), node.items.get(slot))) continue;
                    node.remote.setItem(slot, node.items.get(slot).copy());
                    containerChanged = true;
                }

                if (containerChanged) {
                    node.remote.setChanged();
                    changed = true;
                }
            }

            if (changed) {
                player.getInventory().setChanged();
            }
            return changed;
        }
        private int countAvailable(ItemStack target) {
            long total = 0;
            for (Node node : nodes) {
                for (ItemStack stack : node.items) {
                    if (stack.isEmpty() || isStorageItem(stack)) {
                        continue;
                    }
                    if (same(stack, target)) {
                        total += stack.getCount();
                    } else {
                        int multiplier = getDecompressionMultiplier(stack, target, MAX_UNPACK_DEPTH);
                        if (multiplier > 0) {
                            total += (long) stack.getCount() * multiplier;
                        }
                    }
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        private int getDecompressionMultiplier(ItemStack source, ItemStack target, int depth) {
            if (depth <= 0 || isStorageItem(source)) {
                return 0;
            }
            Optional<ReverseRule> reverse = getReverseRule(source);
            if (reverse.isEmpty()) {
                return 0;
            }
            ReverseRule rule = reverse.get();
            if (same(rule.output(), target)) {
                return rule.count();
            }
            int sub = getDecompressionMultiplier(rule.output(), target, depth - 1);
            return sub > 0 ? rule.count() * sub : 0;
        }
    }
    /**
     * 快速查询玩家末影背包系统中某个物品的可用总量（含已存储的压缩物按配方解压后的折算量）
     */
    public static int countAvailable(ServerPlayer player, ItemStack target) {
        if (target.isEmpty() || isStorageItem(target)) {
            return 0;
        }
        Session session = open(player);
        if (session == null) {
            return 0;
        }
        return session.countAvailable(target);
    }
}