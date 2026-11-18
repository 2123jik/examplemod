package com.example.examplemod.engine;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "examplemod")
public class WandEditMenu extends AbstractContainerMenu {

    private static final ResourceLocation MENU_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "wand_edit");
    public static final Supplier<MenuType<WandEditMenu>> MENU_TYPE =
            DeferredHolder.create(Registries.MENU, MENU_ID);

    public static final int WAND_SLOTS = 9;
    public static final int TEMP_SLOTS = SpellBackpackData.TEMP_SLOTS;          // 9
    public static final int PAGE_SLOTS = SpellBackpackData.BACKPACK_PAGE_SLOTS; // 36

    // 4 个标签页：0=主动卷轴, 1=法术增强, 2=法术被动, 3=身上背包(方便往法术背包里导物品)
    public int currentTab = 0;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack wandStack;
    private final ItemStackHandler wandHandler;
    private final SpellBackpackData backpack;
    private boolean initializing = true; // 新增标志位
    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.MENU, h ->
                h.register(MENU_ID, IMenuTypeExtension.create(WandEditMenu::new)));
    }

    public WandEditMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInv, InteractionHand.values()[extraData.readVarInt()]);
    }

    public WandEditMenu(int containerId, Inventory playerInv, InteractionHand hand) {
        super(MENU_TYPE.get(), containerId);
        this.player = playerInv.player;
        this.hand = hand;
        this.wandStack = player.getItemInHand(hand);
        this.backpack = player.getData(SpellBackpackData.BACKPACK_DATA);

        this.wandHandler = new ItemStackHandler(WAND_SLOTS) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return TestWandItem.isValidModule(stack); }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                if (!initializing) saveBackToStack();
            }
        };

        ItemContainerContents contents = wandStack.getOrDefault(TestWandItem.WAND_SLOTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> temp = NonNullList.withSize(WAND_SLOTS, ItemStack.EMPTY);
        contents.copyInto(temp);
        for (int i = 0; i < WAND_SLOTS; i++) wandHandler.setStackInSlot(i, temp.get(i));
        this.initializing = false;
        // 1. 顶部：法杖编排槽 (0 ~ 8) y=20
        for (int i = 0; i < WAND_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(wandHandler, i, 8 + i * 18, 20));
        }

        // 2. 中部：Temp 暂存条 (9 ~ 17) y=54
        for (int i = 0; i < TEMP_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(backpack.tempBar, i, 8 + i * 18, 54) {
                @Override public boolean mayPlace(ItemStack s) { return TestWandItem.isValidModule(s); }
            });
        }

        // 3. 底部：法术背包 Tab 0/1/2 + 身上背包 Tab 3（全部放在 y=94 的同一片 4×9 网格，用 isActive() 无损切换）
        addPagedSlots(backpack.activePage, 0, s -> s.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(s));
        addPagedSlots(backpack.enhancePage, 1, s -> {
            var spec = TestWandItem.getGemSpec(s);
            return spec != null && !spec.isPassiveTab();
        });
        addPagedSlots(backpack.passivePage, 2, s -> {
            var spec = TestWandItem.getGemSpec(s);
            return spec != null && spec.isPassiveTab();
        });

        int lockedHotbar = (hand == InteractionHand.MAIN_HAND) ? playerInv.selected : -1;
        for (int i = 0; i < 36; i++) {
            int row = i / 9;
            int col = i % 9;
            int invIndex = (i < 27) ? (i + 9) : (i - 27);
            final boolean isLocked = (invIndex == lockedHotbar);
            this.addSlot(new Slot(playerInv, invIndex, 8 + col * 18, 94 + row * 18) {
                @Override public boolean isActive() { return currentTab == 3; }
                @Override public boolean mayPickup(Player p) { return !isLocked && super.mayPickup(p); }
            });
        }
    }

    private void addPagedSlots(ItemStackHandler handler, int tabId, java.util.function.Predicate<ItemStack> filter) {
        for (int i = 0; i < PAGE_SLOTS; i++) {
            int row = i / 9;
            int col = i % 9;
            this.addSlot(new SlotItemHandler(handler, i, 8 + col * 18, 94 + row * 18) {
                @Override public boolean isActive() { return currentTab == tabId; }
                @Override public boolean mayPlace(ItemStack s) { return filter.test(s); }
            });
        }
    }

    /**
     * 切换标签页 (0~3) 或 一键收纳玩家身上全部法术 (10)
     */
    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id >= 0 && id <= 3) {
            this.currentTab = id;
            return true;
        }
        if (id == 10 && !p.level().isClientSide()) {
            collectFromPlayerInventory(p.getInventory());
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    private void collectFromPlayerInventory(Inventory inv) {
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || !TestWandItem.isValidModule(s)) continue;
            ItemStackHandler target = routeTargetHandler(s);
            if (target == null) continue;

            ItemStack remaining = insertIntoHandler(target, s.copy());
            inv.items.set(i, remaining);
        }
    }

    private ItemStackHandler routeTargetHandler(ItemStack s) {
        if (s.getItem() instanceof Scroll) return backpack.activePage;
        var spec = TestWandItem.getGemSpec(s);
        if (spec == null) return null;
        return spec.isPassiveTab() ? backpack.passivePage : backpack.enhancePage;
    }

    private ItemStack insertIntoHandler(ItemStackHandler handler, ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
            stack = handler.insertItem(slot, stack, false);
        }
        return stack;
    }

    private void saveBackToStack() {
        List<ItemStack> items = new ArrayList<>(WAND_SLOTS);
        for (int i = 0; i < WAND_SLOTS; i++) items.add(wandHandler.getStackInSlot(i).copy());
        wandStack.set(TestWandItem.WAND_SLOTS.get(), ItemContainerContents.fromItems(items));

        if (player instanceof ServerPlayer sp) {
            WandPassiveTickHandler.updateHeldWand(sp, WandExecutionEngine.compileConnectedPassives(sp));
        }
    }

    public int[] getCapacityStats() {
        List<ItemStack> items = new ArrayList<>(WAND_SLOTS);
        for (int i = 0; i < WAND_SLOTS; i++) items.add(wandHandler.getStackInSlot(i));
        return TestWandItem.computeSlotUsageAndMax(items, WAND_SLOTS);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int tab0Start = WAND_SLOTS + TEMP_SLOTS;
        int tab1Start = tab0Start + PAGE_SLOTS;
        int tab2Start = tab1Start + PAGE_SLOTS;
        int tab3Start = tab2Start + PAGE_SLOTS;

        if (index < WAND_SLOTS) {
            // 从法杖 Shift 取出：优先进 Temp 暂存区，满了再自动进对应的分类背包
            if (!moveItemStackTo(stack, WAND_SLOTS, tab0Start, false)) {
                int targetStart = getPageStartFor(stack);
                if (!moveItemStackTo(stack, targetStart, targetStart + PAGE_SLOTS, false)) return ItemStack.EMPTY;
            }
        } else if (index < tab0Start) {
            // 从 Temp Shift：进法杖；法杖满则退回对应分类背包
            if (!moveItemStackTo(stack, 0, WAND_SLOTS, false)) {
                int targetStart = getPageStartFor(stack);
                if (!moveItemStackTo(stack, targetStart, targetStart + PAGE_SLOTS, false)) return ItemStack.EMPTY;
            }
        } else if (index < tab3Start) {
            // 从法术背包 Shift：优先进法杖，满了进 Temp
            if (!moveItemStackTo(stack, 0, WAND_SLOTS + TEMP_SLOTS, false)) return ItemStack.EMPTY;
        } else {
            // 从身上背包 Shift：进对应分类法术背包
            if (!TestWandItem.isValidModule(stack)) return ItemStack.EMPTY;
            int targetStart = getPageStartFor(stack);
            if (!moveItemStackTo(stack, targetStart, targetStart + PAGE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private int getPageStartFor(ItemStack s) {
        int base = WAND_SLOTS + TEMP_SLOTS;
        if (s.getItem() instanceof Scroll) return base;
        var spec = TestWandItem.getGemSpec(s);
        return (spec != null && spec.isPassiveTab()) ? (base + PAGE_SLOTS * 2) : (base + PAGE_SLOTS);
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand) == wandStack && !wandStack.isEmpty();
    }
}