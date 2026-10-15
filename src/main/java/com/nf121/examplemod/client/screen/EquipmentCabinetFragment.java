package com.nf121.examplemod.client.screen;

import com.nf121.examplemod.entity.EquipmentCabinetBlockEntity;
import com.nf121.examplemod.entity.EquipmentDisplay;
import com.nf121.examplemod.entity.ModernSlotView;
import com.nf121.examplemod.menu.EquipmentCabinetMenu;
import com.nf121.examplemod.util.EquipmentFilter;
import com.nf121.examplemod.util.EquipmentIndex;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public final class EquipmentCabinetFragment extends Fragment implements ScreenCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentCabinetFragment.class);
    private FrameLayout screenRoot;
    private static final int PAGE_SIZE_LIST = 16;
    private static final int SLOT_SIZE_DP = 42;
    private static final int SLOT_MARGIN_DP = 2;

    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int ACCENT = 0xFF8CC8FF;
    private static final int ERROR = 0xFFFF8888;
    private static final int WARNING = 0xFFFFCC00;
    private static final int SUCCESS = 0xFF88FF88;

    private final EquipmentCabinetMenu menu;

    private final Map<Integer, ItemStack> cachedStacks = new HashMap<>();
    private final Map<Integer, EquipmentIndex.Row> cachedRows = new HashMap<>();

    private boolean gridMode = true;
    private boolean playerInventory = false;
    private boolean transferPending = false;
    private boolean salvageUnlocked = false;
    private String activeSortKey = "rarity";
    private boolean sortAscending = false;

    // 浮层与 Tooltip
    private MinecraftSurfaceView tooltipOverlay;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    private boolean forceIndex = true;
    private boolean indexFailureLogged;

    private LinearLayout root;
    private LinearLayout table;
    private LinearLayout tableHeader;
    private LinearLayout activeConditionsView;
    private ScrollView tableScroll;

    private TextView status;
    private TextView pageLabel;
    private TextView capacityLabel;

    private EditText search;
    private EditText enchantmentMinimum;
    private EditText attributeMinimum;

    private Button sourceButton;
    private Button viewModeButton;
    private Button dumpOrExtractButton;
    private Button salvageFilteredButton;
    private Button salvageSafetyButton;
    private Button quickSortButton;
    private Button previous;
    private Button next;

    private ChoiceField materialChoice;
    private ChoiceField rarityChoice;
    private ChoiceField categoryChoice;
    private ChoiceField durabilityChoice;
    private ChoiceField enchantedChoice;
    private ChoiceField sortChoice;

    private ChoiceField enchantmentChoice;
    private ChoiceField attributeChoice;
    private ChoiceField operationChoice;
    private ChoiceField comparisonChoice;

    private final List<ChoiceField> allChoiceFields = new ArrayList<>();
    private final List<NamedCondition> conditions = new ArrayList<>();

    private int page;
    private int lastComputedColumns = 14;

    private List<EquipmentIndex.Row> snapshot = List.of();
    private List<EquipmentIndex.Row> currentFiltered = List.of();

    private volatile long generation;
    private final AtomicBoolean refreshPending = new AtomicBoolean();

    private final Runnable refresh = this::requestSnapshot;

    private final Runnable filterChanged = () -> {
        page = 0;
        clearHoveredItem();
        if (tableScroll != null) tableScroll.scrollTo(0, 0);
        renderRows();
    };

    private record Choice(String key, String label) {}
    private record NamedCondition(String label, Predicate<EquipmentIndex.Row> predicate) {}
    private record HeaderMeta(String label, String sortKey) {}

    public EquipmentCabinetFragment(EquipmentCabinetMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean isBackKey(int keyCode, KeyEvent event) {
        if (keyCode == 256 && search != null && search.isFocused()) {
            search.clearFocus();
            return true;
        }

        if (keyCode == 256) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean isInventoryKey = mc.options.keyInventory.matches(keyCode, event.getScanCode());
        boolean anyTextFocused = (search != null && search.isFocused())
                || (enchantmentMinimum != null && enchantmentMinimum.isFocused())
                || (attributeMinimum != null && attributeMinimum.isFocused());

        return isInventoryKey && !anyTextFocused;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        Context context = requireContext();

        page = 0;
        transferPending = false;
        salvageUnlocked = false;
        conditions.clear();
        allChoiceFields.clear();

// 改为成员变量赋值
        this.screenRoot = new FrameLayout(context);
        root = column(context);
        root.setPadding(root.dp(12), root.dp(10), root.dp(12), root.dp(8));
        screenRoot.addView(root, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        root.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int newWidth = right - left;
            int oldWidth = oldRight - oldLeft;
            if (newWidth != oldWidth && newWidth > 0) {
                int newCols = calculateDynamicColumns();
                if (newCols != lastComputedColumns) {
                    lastComputedColumns = newCols;
                    renderRows();
                }
            }
        });

        createToolbar(context);

        LinearLayout statusRow = row(context);
        status = text(context, "正在同步装备信息……", MUTED);
        statusRow.addView(status, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
        capacityLabel = text(context, "", MUTED);
        statusRow.addView(capacityLabel, lp(WRAP_CONTENT, WRAP_CONTENT));
        root.addView(statusRow, lp(MATCH_PARENT, WRAP_CONTENT));

        LinearLayout body = row(context);
        root.addView(body, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));

        LinearLayout filters = column(context);
        ScrollView filterScroll = scroll(context, filters);
        body.addView(filterScroll, new LinearLayout.LayoutParams(root.dp(240), MATCH_PARENT));
        createFilters(filters);

        LinearLayout content = column(context);
        body.addView(content, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));

        createTable(context, content);
        createBottomBar(context, content);

        tooltipOverlay = new MinecraftSurfaceView(context);
        tooltipOverlay.setClickable(false);
        tooltipOverlay.setFocusable(false);
        tooltipOverlay.setRenderer(new MinecraftSurfaceView.Renderer() {
            @Override
            public void onSurfaceChanged(int width, int height) {}

            @Override
            public void onDraw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, double guiScale, float alpha) {
                if (hoveredStack.isEmpty()) return;

                Font font = Minecraft.getInstance().font;
                List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), hoveredStack));

                var level = Minecraft.getInstance().level;
                if (level != null) {
                    var matches = SalvagingMenu.findMatch(level, hoveredStack);
                    if (!matches.isEmpty()) {
                        tooltip.add(Component.empty());
                        tooltip.add(Component.literal("🔨 神化分解产物预估:").withStyle(ChatFormatting.GOLD));
                        for (var recipe : matches) {
                            for (var d : recipe.value().getOutputs()) {
                                int[] counts = SalvagingMenu.getSalvageCounts(d, hoveredStack);
                                tooltip.add(Component.literal(String.format("  • %d~%d × %s", counts[0], counts[1], d.stack().getHoverName().getString()))
                                        .withStyle(ChatFormatting.YELLOW));
                            }
                        }
                        if (salvageUnlocked) {
                            tooltip.add(Component.literal("▶ 右键: 原地直接分解").withStyle(ChatFormatting.RED));
                        } else {
                            tooltip.add(Component.literal("🔒 右键分解已锁定 (需在顶部解锁)").withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
                tooltip.add(Component.literal("▶ 左键: 快速转移").withStyle(ChatFormatting.AQUA));

                graphics.flush();

                boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
                if (scissorEnabled) {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }

                // 清空深度缓冲，保证 Tooltip 不受任何先前的图元阻挡
                com.mojang.blaze3d.systems.RenderSystem.clearDepth(1.0);
                com.mojang.blaze3d.systems.RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthFunc(GL11.GL_LEQUAL);

                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(0, 0, 400.0F);
                    graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                    graphics.flush();
                } finally {
                    graphics.pose().popPose();

                    if (scissorEnabled) {
                        GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    }
                    com.mojang.blaze3d.systems.RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                    com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                }
            }
        });
        screenRoot.addView(tooltipOverlay, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        refreshOptions();
        renderRows();

        return screenRoot;
    }

    private void setHoveredItem(ItemStack stack) {
        this.hoveredStack = stack != null ? stack : ItemStack.EMPTY;
        if (tooltipOverlay != null) {
            tooltipOverlay.invalidate();
        }
    }

    private void clearHoveredItem() {
        if (!this.hoveredStack.isEmpty()) {
            this.hoveredStack = ItemStack.EMPTY;
            if (tooltipOverlay != null) {
                tooltipOverlay.invalidate();
            }
        }
    }
    /**
     * 强迫 tooltipOverlay 的 DrawHandler 重新注册到 ViewRootImpl 的最末尾，
     * 确保它永远在所有新创建的格子图标之后被绘制！
     */
    private void ensureTooltipOnTop() {
        if (tooltipOverlay != null && screenRoot != null) {
            screenRoot.removeView(tooltipOverlay);
            screenRoot.addView(tooltipOverlay, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
    }
    private void createToolbar(Context context) {
        LinearLayout toolbar = row(context);
        root.addView(toolbar, lp(MATCH_PARENT, WRAP_CONTENT));

        sourceButton = button(context, "视图：装备柜");
        sourceButton.setTextColor(ACCENT);
        sourceButton.setOnClickListener(view -> {
            playerInventory = !playerInventory;
            sourceButton.setText(playerInventory ? "视图：玩家背包" : "视图：装备柜");
            updateDumpOrExtractButton();
            page = 0;
            clearHoveredItem();
            if (tableScroll != null) tableScroll.scrollTo(0, 0);
            renderRows();
        });
        toolbar.addView(sourceButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        viewModeButton = button(context, gridMode ? "模式：网格" : "模式：清单");
        viewModeButton.setOnClickListener(view -> {
            gridMode = !gridMode;
            viewModeButton.setText(gridMode ? "模式：网格" : "模式：清单");
            clearHoveredItem();
            if (tableScroll != null) tableScroll.scrollTo(0, 0);
            renderRows();
        });
        toolbar.addView(viewModeButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        dumpOrExtractButton = button(context, "📥 一键存入柜中");
        updateDumpOrExtractButton();
        dumpOrExtractButton.setOnClickListener(view -> {
            if (playerInventory) {
                dumpAllEquipment();
            } else {
                extractAllFiltered();
            }
        });
        toolbar.addView(dumpOrExtractButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        salvageSafetyButton = button(context, "🔒 分解锁定");
        salvageSafetyButton.setTextColor(MUTED);
        salvageSafetyButton.setOnClickListener(view -> {
            salvageUnlocked = !salvageUnlocked;
            salvageSafetyButton.setText(salvageUnlocked ? "🔓 分解已解锁" : "🔒 分解锁定");
            salvageSafetyButton.setTextColor(salvageUnlocked ? WARNING : MUTED);
            updateSalvageFilteredButtonState();
            playUiClickSound();
            showMessage(salvageUnlocked ? "已解除分解锁定，可直接右键或批量分解！" : "已恢复分解保护锁定。", salvageUnlocked ? WARNING : MUTED);
        });
        toolbar.addView(salvageSafetyButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        salvageFilteredButton = button(context, "🔨 批量分解匹配");
        salvageFilteredButton.setTextColor(ERROR);
        salvageFilteredButton.setEnabled(false);
        salvageFilteredButton.setAlpha(0.4F);
        salvageFilteredButton.setOnClickListener(view -> salvageAllFiltered());
        toolbar.addView(salvageFilteredButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        quickSortButton = button(context, "排序: 品质 ▼");
        quickSortButton.setOnClickListener(view -> cycleSortMode());
        toolbar.addView(quickSortButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        Button rebuild = button(context, "🔄 刷新");
        rebuild.setOnClickListener(view -> {
            Minecraft.getInstance().execute(() -> forceIndex = true);
            showMessage("正在重新建立装备索引……", MUTED);
        });
        toolbar.addView(rebuild, lp(WRAP_CONTENT, WRAP_CONTENT));
    }

    private void updateDumpOrExtractButton() {
        if (dumpOrExtractButton == null) return;
        dumpOrExtractButton.setText(playerInventory ? "📥 一键存入柜中" : "📤 一键取出匹配");
    }

    private void updateSalvageFilteredButtonState() {
        if (salvageFilteredButton == null) return;
        var level = Minecraft.getInstance().level;
        long salvageableCount = 0;
        if (level != null && !playerInventory) {
            salvageableCount = currentFiltered.stream()
                    .filter(row -> row.menuSlot() < EquipmentCabinetMenu.CABINET_SLOTS)
                    .map(row -> cachedStacks.get(row.menuSlot()))
                    .filter(s -> s != null && !s.isEmpty() && !SalvagingMenu.findMatch(level, s).isEmpty())
                    .count();
        }

        salvageFilteredButton.setText(salvageableCount > 0 ? "🔨 批量分解 (" + salvageableCount + ")" : "🔨 批量分解匹配");
        boolean canSalvage = salvageUnlocked && salvageableCount > 0;
        salvageFilteredButton.setEnabled(canSalvage);
        salvageFilteredButton.setAlpha(canSalvage ? 1.0F : 0.4F);
    }

    private void cycleSortMode() {
        switch (activeSortKey) {
            case "rarity" -> {
                activeSortKey = "durability";
                sortAscending = false;
                quickSortButton.setText("排序: 耐久 ▼");
            }
            case "durability" -> {
                activeSortKey = "enchantments";
                sortAscending = false;
                quickSortButton.setText("排序: 附魔 ▼");
            }
            case "enchantments" -> {
                activeSortKey = "name";
                sortAscending = true;
                quickSortButton.setText("排序: 名称 ▲");
            }
            default -> {
                activeSortKey = "rarity";
                sortAscending = false;
                quickSortButton.setText("排序: 品质 ▼");
            }
        }
        if (sortChoice != null) sortChoice.select(activeSortKey);
        refreshHeaderLabels();
        renderRows();
    }

    private void createTable(Context context, LinearLayout content) {
        tableHeader = row(context);
        tableHeader.setPadding(0, root.dp(4), 0, root.dp(4));

        addHeaderButton(tableHeader, "装备名称", "name", 3.0F);
        addHeaderButton(tableHeader, "材质", "material", 1.3F);
        addHeaderButton(tableHeader, "耐久", "durability", 1.2F);
        addHeaderButton(tableHeader, "附魔", "enchantments", 0.7F);
        addHeaderButton(tableHeader, "品质", "rarity", 1.3F);
        addHeaderButton(tableHeader, "类型", "category", 1.3F);
        addCell(tableHeader, "操作", 0.8F, MUTED);

        content.addView(tableHeader, lp(MATCH_PARENT, WRAP_CONTENT));

        table = column(context);
        tableScroll = scroll(context, table);

        tableScroll.setOnGenericMotionListener((v, event) -> {
            if (gridMode && event.getAction() == MotionEvent.ACTION_SCROLL) {
                float axis = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                int totalPages = getTotalPages();
                if (axis < 0 && page + 1 < totalPages) {
                    page++;
                    clearHoveredItem();
                    renderRows();
                    return true;
                } else if (axis > 0 && page > 0) {
                    page--;
                    clearHoveredItem();
                    renderRows();
                    return true;
                }
            }
            return false;
        });

        content.addView(tableScroll, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
    }

    private void addHeaderButton(LinearLayout header, String label, String sortKey, float weight) {
        Button btn = new Button(header.getContext());
        btn.setTextSize(12);
        btn.setPadding(btn.dp(4), btn.dp(2), btn.dp(4), btn.dp(2));
        btn.setBackground(null);
        updateHeaderButtonText(btn, label, sortKey);

        btn.setOnClickListener(v -> {
            if (activeSortKey.equals(sortKey)) {
                sortAscending = !sortAscending;
            } else {
                activeSortKey = sortKey;
                sortAscending = true;
            }
            if (sortChoice != null) sortChoice.select(sortKey);
            refreshHeaderLabels();
            renderRows();
        });

        btn.setTag(new HeaderMeta(label, sortKey));
        header.addView(btn, new LinearLayout.LayoutParams(0, WRAP_CONTENT, weight));
    }

    private void updateHeaderButtonText(Button btn, String label, String sortKey) {
        if (activeSortKey.equals(sortKey)) {
            btn.setText(label + (sortAscending ? " ▲" : " ▼"));
            btn.setTextColor(ACCENT);
        } else {
            btn.setText(label);
            btn.setTextColor(TEXT);
        }
    }

    private void refreshHeaderLabels() {
        if (tableHeader == null) return;
        for (int i = 0; i < tableHeader.getChildCount(); i++) {
            View child = tableHeader.getChildAt(i);
            if (child instanceof Button btn && btn.getTag() instanceof HeaderMeta meta) {
                updateHeaderButtonText(btn, meta.label, meta.sortKey);
            }
        }
    }

    private void createBottomBar(Context context, LinearLayout content) {
        LinearLayout pager = row(context);
        pager.setPadding(0, root.dp(4), 0, 0);

        previous = button(context, "◀ 上一页");
        previous.setOnClickListener(view -> {
            if (page > 0) {
                page--;
                clearHoveredItem();
                renderRows();
            }
        });
        pager.addView(previous, lp(WRAP_CONTENT, WRAP_CONTENT));

        pageLabel = text(context, "", TEXT);
        pageLabel.setGravity(Gravity.CENTER);
        pager.addView(pageLabel, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));

        next = button(context, "下一页 ▶");
        next.setOnClickListener(view -> {
            if (page + 1 < getTotalPages()) {
                page++;
                clearHoveredItem();
                renderRows();
            }
        });
        pager.addView(next, lp(WRAP_CONTENT, WRAP_CONTENT));

        content.addView(pager, lp(MATCH_PARENT, WRAP_CONTENT));
    }

    private int calculateDynamicColumns() {
        if (root == null) return 14;
        int totalWidth = root.getWidth();
        if (totalWidth <= 0) return 14;
        int availableWidth = totalWidth - root.dp(240 + 36);
        int slotWithMargin = root.dp(SLOT_SIZE_DP + SLOT_MARGIN_DP * 2);
        if (slotWithMargin <= 0) return 14;
        return Math.max(6, availableWidth / slotWithMargin);
    }

    private int getPageSize() {
        if (!gridMode) return PAGE_SIZE_LIST;
        return calculateDynamicColumns() * 6;
    }

    private int getTotalPages() {
        int pageSize = getPageSize();
        return Math.max(1, (currentFiltered.size() + pageSize - 1) / pageSize);
    }

    private void updateCapacityDisplay() {
        if (capacityLabel == null) return;
        int cabinetCount = 0;
        int playerCount = 0;

        for (EquipmentIndex.Row row : snapshot) {
            if (row.menuSlot() < EquipmentCabinetMenu.CABINET_SLOTS) {
                cabinetCount++;
            } else {
                playerCount++;
            }
        }

        if (playerInventory) {
            int playerMax = menu.slots.size() - EquipmentCabinetMenu.PLAYER_START;
            capacityLabel.setText(String.format("背包栏: %d/%d (%.0f%%)", playerCount, playerMax, (playerCount * 100.0F / playerMax)));
        } else {
            int cabMax = EquipmentCabinetMenu.CABINET_SLOTS;
            capacityLabel.setText(String.format("装备柜: %d/%d (%.0f%%)", cabinetCount, cabMax, (cabinetCount * 100.0F / cabMax)));
        }
    }

    private void renderRows() {
        if (table == null) return;

        if (tableHeader != null) {
            tableHeader.setVisibility(gridMode ? View.GONE : View.VISIBLE);
        }

        String query = search != null ? search.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        EquipmentFilter.Durability durability = selectedDurability();
        EquipmentFilter.Enchanted enchanted = selectedEnchanted();

        List<EquipmentIndex.Row> source = snapshot.stream()
                .filter(data -> (data.menuSlot() >= EquipmentCabinetMenu.PLAYER_START) == playerInventory)
                .toList();

        currentFiltered = source.stream()
                .filter(data -> matchesAdvancedQuery(data, query))
                .filter(durability::matches)
                .filter(data -> {
                    if (enchanted == EquipmentFilter.Enchanted.YES && data.enchantments().isEmpty()) return false;
                    return enchanted != EquipmentFilter.Enchanted.NO || data.enchantments().isEmpty();
                })
                .filter(this::matchesSelectedChoices)
                .sorted(selectedComparator().thenComparingInt(EquipmentIndex.Row::menuSlot))
                .toList();

        int pageSize = getPageSize();
        int totalPages = getTotalPages();
        page = Math.max(0, Math.min(page, totalPages - 1));

        updateCapacityDisplay();
        updateSalvageFilteredButtonState();

        showMessage(
                (playerInventory ? "玩家背包" : "装备柜") + "：显示 " + currentFiltered.size() + " / " + source.size() + " 件装备",
                MUTED
        );

        table.removeAllViews();

        int start = page * pageSize;
        int end = Math.min(currentFiltered.size(), start + pageSize);
        List<EquipmentIndex.Row> pageItems = currentFiltered.subList(start, end);

        if (gridMode) {
            renderGrid(pageItems);
        } else {
            for (EquipmentIndex.Row rowData : pageItems) {
                addEquipmentRow(rowData);
            }
        }

        if (currentFiltered.isEmpty()) {
            table.addView(
                    text(
                            requireContext(),
                            source.isEmpty()
                                    ? (playerInventory ? "背包中没有符合存放条件的装备" : "装备柜中暂无装备")
                                    : "没有找到匹配条件的装备，请检查筛选词",
                            MUTED
                    ),
                    lp(MATCH_PARENT, WRAP_CONTENT)
            );
        }

        pageLabel.setText(String.format("第 %d / %d 页 (共 %d 件)", page + 1, totalPages, currentFiltered.size()));
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < totalPages);
        ensureTooltipOnTop();
    }

    private void renderGrid(List<EquipmentIndex.Row> items) {
        Context context = requireContext();
        final int columns = calculateDynamicColumns();

        LinearLayout currentRow = null;

        for (int i = 0; i < items.size(); i++) {
            if (i % columns == 0) {
                currentRow = row(context);
                currentRow.setPadding(0, root.dp(SLOT_MARGIN_DP), 0, root.dp(SLOT_MARGIN_DP));
                table.addView(currentRow, lp(MATCH_PARENT, WRAP_CONTENT));
            }

            EquipmentIndex.Row data = items.get(i);
            ItemStack stack = cachedStacks.get(data.menuSlot());

            ModernSlotView slotView = new ModernSlotView(context, data, stack);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    root.dp(SLOT_SIZE_DP), root.dp(SLOT_SIZE_DP)
            );
            lp.setMargins(root.dp(SLOT_MARGIN_DP), root.dp(SLOT_MARGIN_DP), root.dp(SLOT_MARGIN_DP), root.dp(SLOT_MARGIN_DP));
            slotView.setLayoutParams(lp);

            slotView.setOnHoverItemListener(
                    (view, itemData) -> {
                        setHoveredItem(cachedStacks.get(itemData.menuSlot()));
                        // 确保鼠标在格子里微动时，顶层 Tooltip 始终被唤醒重绘
                        if (tooltipOverlay != null) {
                            tooltipOverlay.invalidate();
                        }
                    },
                    this::clearHoveredItem
            );

            slotView.setOnClickListener(v -> {
                if (!transferPending) {
                    transferPending = true;
                    transfer(data);
                }
            });

            slotView.setOnContextClickListener(v -> {
                if (!salvageUnlocked) {
                    showMessage("请先点击顶部 [🔒 分解锁定] 解除保护后再分解！", WARNING);
                    return true;
                }
                executeDirectSalvage(data.menuSlot());
                return true;
            });

            if (currentRow != null) {
                currentRow.addView(slotView);
            }
        }
    }

    private void executeDirectSalvage(int slotId) {
        if (slotId < 0 || slotId >= EquipmentCabinetMenu.CABINET_SLOTS) {
            showMessage("背包中的装备请先存入柜中再进行分解！", ERROR);
            return;
        }
        ItemStack stack = cachedStacks.get(slotId);
        var level = Minecraft.getInstance().level;
        if (stack == null || stack.isEmpty() || level == null || SalvagingMenu.findMatch(level, stack).isEmpty()) {
            showMessage("该物品不属于可分解的神化装备！", ERROR);
            return;
        }

        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, slotId);
            playSalvageSound();
        });
        clearHoveredItem();
        showMessage("已分解: " + stack.getHoverName().getString(), SUCCESS);
    }

    /**
     * 带背包空位上限保护的一键提取，防止发包溢出
     */
    private void extractAllFiltered() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null) return;

        List<Integer> cabinetSlots = currentFiltered.stream()
                .map(EquipmentIndex.Row::menuSlot)
                .filter(slot -> slot < EquipmentCabinetMenu.CABINET_SLOTS)
                .toList();

        if (cabinetSlots.isEmpty()) {
            showMessage("当前筛选中没有柜内装备可取出！", WARNING);
            return;
        }

        minecraft.execute(() -> {
            // 计算玩家当前可用真实空槽数 (0..35)
            int freeSlots = 0;
            var inv = minecraft.player.getInventory();
            for (int i = 0; i < 36; i++) {
                if (inv.getItem(i).isEmpty()) {
                    freeSlots++;
                }
            }

            if (freeSlots == 0) {
                if (root != null) root.post(() -> showMessage("背包已满，无法取出装备！", ERROR));
                return;
            }

            int count = 0;
            for (int slotId : cabinetSlots) {
                if (count >= freeSlots) break; // 保护上限

                minecraft.gameMode.handleInventoryMouseClick(
                        menu.containerId, slotId, 0, ClickType.QUICK_MOVE, minecraft.player
                );
                count++;
            }
            final int finalCount = count;
            playUiClickSound();
            if (root != null) {
                root.post(() -> showMessage("已取出 " + finalCount + " 件装备到背包！", SUCCESS));
            }
        });
    }

    private void dumpAllEquipment() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null) return;

        minecraft.execute(() -> {
            int count = 0;
            for (int slot = EquipmentCabinetMenu.PLAYER_START; slot < menu.slots.size(); slot++) {
                ItemStack stack = menu.slots.get(slot).getItem();
                if (!stack.isEmpty() && EquipmentCabinetBlockEntity.accepts(stack)) {
                    minecraft.gameMode.handleInventoryMouseClick(
                            menu.containerId, slot, 0, ClickType.QUICK_MOVE, minecraft.player
                    );
                    count++;
                }
            }
            final int finalCount = count;
            playUiClickSound();
            if (root != null) {
                root.post(() -> showMessage("已存入 " + finalCount + " 件装备到装备柜！", SUCCESS));
            }
        });
    }

    private void salvageAllFiltered() {
        if (!salvageUnlocked) {
            showMessage("分解已锁定，请先解锁！", WARNING);
            return;
        }

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        List<Integer> targetSlots = currentFiltered.stream()
                .map(EquipmentIndex.Row::menuSlot)
                .filter(slot -> slot < EquipmentCabinetMenu.CABINET_SLOTS)
                .filter(slot -> {
                    ItemStack s = cachedStacks.get(slot);
                    return s != null && !s.isEmpty() && !SalvagingMenu.findMatch(level, s).isEmpty();
                })
                .toList();

        if (targetSlots.isEmpty()) {
            showMessage("当前筛选中没有可供分解的装备！", ERROR);
            return;
        }

        Minecraft.getInstance().execute(() -> {
            for (int slotId : targetSlots) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, slotId);
            }
            playSalvageSound();
        });

        clearHoveredItem();
        showMessage("批量分解完成，共 " + targetSlots.size() + " 件！", SUCCESS);
        renderRows();
    }

    private void addEquipmentRow(EquipmentIndex.Row data) {
        LinearLayout line = row(requireContext());
        line.setPadding(0, root.dp(2), 0, root.dp(2));

        TextView nameCell = addCell(line, data.name(), 3.0F, TEXT);
        nameCell.setTooltipText(data.name());

        // 清单模式增强：悬浮名称时展示完整装备 Tooltip 与分解预估
        nameCell.setOnHoverListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE) {
                setHoveredItem(cachedStacks.get(data.menuSlot()));
                return true;
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                clearHoveredItem();
                return true;
            }
            return false;
        });

        String materialName = EquipmentDisplay.materialName(data.materials());
        addCell(line, materialName, 1.3F, MUTED);

        addCell(line, data.durabilityText(), 1.2F,
                data.hasDurability() && data.durabilityPercent() < 20 ? ERROR : TEXT);

        addCell(line, data.enchantments().isEmpty() ? "无" : data.enchantments().size() + "项", 0.7F, TEXT);

        String rarityName = EquipmentDisplay.friendly(data.rarityName(), "未知品质");
        addCell(line, rarityName, 1.3F, data.rarityColor());

        String categoryName = EquipmentDisplay.friendly(data.categoryName(), "其他装备");
        addCell(line, categoryName, 1.3F, MUTED);

        Button actionBtn = button(requireContext(), playerInventory ? "存入" : "取出");
        actionBtn.setTextColor(ACCENT);
        actionBtn.setOnClickListener(v -> {
            if (!transferPending) {
                transferPending = true;
                transfer(data);
            }
        });
        line.addView(actionBtn, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 0.8F));

        table.addView(line, lp(MATCH_PARENT, WRAP_CONTENT));
    }

    // ==================== 智能全属性匹配引擎 ====================

    private boolean matchesAdvancedQuery(EquipmentIndex.Row data, String query) {
        if (query.isEmpty()) return true;

        String[] tokens = query.split("\\s+");
        ItemStack stack = cachedStacks.get(data.menuSlot());

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (token.startsWith("!") && token.length() > 1) {
                String exclude = token.substring(1);
                if (containsMatch(data, stack, exclude)) return false;
                continue;
            }

            if (token.startsWith("@") && token.length() > 1) {
                if (stack == null) return false;
                String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                if (!modId.contains(token.substring(1))) return false;
                continue;
            }

            if (token.startsWith("#") && token.length() > 1) {
                if (stack == null) return false;
                String tagQuery = token.substring(1);
                boolean tagMatch = stack.getTags().anyMatch(tag -> tag.location().getPath().contains(tagQuery));
                if (!tagMatch) return false;
                continue;
            }

            if (!containsMatch(data, stack, token)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsMatch(EquipmentIndex.Row data, ItemStack stack, String keyword) {
        if (data.name().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (data.categoryName().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        if (data.rarityName().toLowerCase(Locale.ROOT).contains(keyword)) return true;

        if (stack != null) {
            String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (itemPath.contains(keyword)) return true;
        }

        // 词缀名称
        for (String affix : data.affixes()) {
            if (affix.toLowerCase(Locale.ROOT).contains(keyword)) return true;
        }

        // 附魔名称与 ID
        for (EquipmentIndex.EnchantmentValue enchantment : data.enchantments()) {
            if (enchantment.name().toLowerCase(Locale.ROOT).contains(keyword)) return true;
            if (enchantment.id().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        }

        // 【新增迭代修复】：匹配神化装备的具体属性词条（如“暴击伤害”、“攻击速度”、“护甲穿透”）
        for (EquipmentIndex.AttributeValue attr : data.attributes()) {
            if (attr.name().toLowerCase(Locale.ROOT).contains(keyword)) return true;
            if (attr.id().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        }

        return false;
    }

    private boolean matchesSelectedChoices(EquipmentIndex.Row data) {
        if (!materialChoice.key().isEmpty() && !materialKey(data).equals(materialChoice.key())) return false;
        if (!rarityChoice.key().isEmpty() && !data.rarityId().equals(rarityChoice.key())) return false;
        if (!categoryChoice.key().isEmpty() && !data.categoryId().equals(categoryChoice.key())) return false;

        for (NamedCondition condition : conditions) {
            if (!condition.predicate().test(data)) return false;
        }
        return true;
    }

    private Comparator<EquipmentIndex.Row> selectedComparator() {
        Comparator<EquipmentIndex.Row> comparator = switch (activeSortKey) {
            case "durability" -> Comparator.comparingDouble(data -> data.hasDurability() ? data.durabilityPercent() : -1.0);
            case "rarity" -> Comparator.comparingInt(EquipmentIndex.Row::rarityOrder);
            case "enchantments" -> Comparator.comparingInt(data -> data.enchantments().size());
            case "material" -> Comparator.comparing(data -> EquipmentDisplay.materialName(data.materials()), String.CASE_INSENSITIVE_ORDER);
            case "category" -> Comparator.comparing(EquipmentIndex.Row::categoryName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(EquipmentIndex.Row::name, String.CASE_INSENSITIVE_ORDER);
        };
        return sortAscending ? comparator : comparator.reversed();
    }

    private static String materialKey(EquipmentIndex.Row data) {
        return String.join("\u001F", new TreeSet<>(data.materials()));
    }

    private EquipmentFilter.Durability selectedDurability() {
        String key = durabilityChoice.key();
        return key.isEmpty() ? EquipmentFilter.Durability.ANY : EquipmentFilter.Durability.valueOf(key);
    }

    private EquipmentFilter.Enchanted selectedEnchanted() {
        String key = enchantedChoice.key();
        return key.isEmpty() ? EquipmentFilter.Enchanted.ANY : EquipmentFilter.Enchanted.valueOf(key);
    }

    private void createFilters(LinearLayout filters) {
        Context context = filters.getContext();

        filters.addView(text(context, "搜索装备", TEXT), lp(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout searchContainer = row(context);
        search = new EditText(context);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setTextSize(13);
        search.setSingleLine();
        search.setHint("支持 @mod, #tag, !排除");
        search.setPadding(search.dp(5), search.dp(5), search.dp(5), search.dp(5));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { changed(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchContainer.addView(search, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));

        Button clearSearch = button(context, "✕");
        clearSearch.setPadding(clearSearch.dp(4), clearSearch.dp(2), clearSearch.dp(4), clearSearch.dp(2));
        clearSearch.setOnClickListener(v -> search.setText(""));
        searchContainer.addView(clearSearch, lp(WRAP_CONTENT, WRAP_CONTENT));
        filters.addView(searchContainer, lp(MATCH_PARENT, WRAP_CONTENT));

        materialChoice = new ChoiceField(filters, "装备材质", "不限", this::changed);
        rarityChoice = new ChoiceField(filters, "神化品质", "不限", this::changed);
        categoryChoice = new ChoiceField(filters, "装备类型", "不限", this::changed);

        durabilityChoice = new ChoiceField(filters, "剩余耐久", "不限", this::changed);
        durabilityChoice.setChoices(List.of(
                new Choice("FULL", "满耐久"),
                new Choice("GE_70", "70% 至不足 100%"),
                new Choice("GE_40", "40% 至不足 70%"),
                new Choice("GE_20", "20% 至不足 40%"),
                new Choice("BELOW_20", "不足 20%"),
                new Choice("NONE", "没有耐久")
        ));

        enchantedChoice = new ChoiceField(filters, "附魔状态", "不限", this::changed);
        enchantedChoice.setChoices(List.of(
                new Choice("YES", "有附魔"),
                new Choice("NO", "无附魔")
        ));

        sortChoice = new ChoiceField(filters, "排序方式", "品质从高到低", () -> {
            activeSortKey = sortChoice.key().isEmpty() ? "rarity" : sortChoice.key();
            quickSortButton.setText("排序: " + sortChoice.label());
            refreshHeaderLabels();
            changed();
        });
        sortChoice.setChoices(List.of(
                new Choice("rarity", "品质从高到低"),
                new Choice("durability", "耐久从高到低"),
                new Choice("enchantments", "附魔数量从多到少"),
                new Choice("name", "名称排序")
        ));

        filters.addView(text(context, "附魔精准筛选", ACCENT), lp(MATCH_PARENT, WRAP_CONTENT));
        enchantmentChoice = new ChoiceField(filters, "附魔类型", "请选择", () -> {});
        enchantmentMinimum = field(filters, "最低等级", "留空表示至少 1 级", () -> {});

        Button addEnchantment = button(context, "+ 添加附魔条件");
        addEnchantment.setOnClickListener(view -> addEnchantmentCondition());
        filters.addView(addEnchantment, lp(MATCH_PARENT, WRAP_CONTENT));

        filters.addView(text(context, "属性词缀筛选", ACCENT), lp(MATCH_PARENT, WRAP_CONTENT));
        attributeChoice = new ChoiceField(filters, "词缀属性", "请选择", () -> {});
        operationChoice = new ChoiceField(filters, "加成方式", "请选择", () -> {});
        operationChoice.setChoices(List.of(
                new Choice("ADD_VALUE", "固定数值"),
                new Choice("ADD_MULTIPLIED_BASE", "基础百分比"),
                new Choice("ADD_MULTIPLIED_TOTAL", "总量百分比")
        ));
        operationChoice.select("ADD_VALUE");

        comparisonChoice = new ChoiceField(filters, "要求范围", "包含该属性即可", () -> {});
        comparisonChoice.setChoices(List.of(
                new Choice("ge", "大于等于 (≥)"),
                new Choice("le", "小于等于 (≤)"),
                new Choice("eq", "精确等于 (=)")
        ));

        attributeMinimum = field(filters, "数值阈值", "如 10 或 10%", () -> {});

        Button addAttribute = button(context, "+ 添加属性条件");
        addAttribute.setOnClickListener(view -> addAttributeCondition());
        filters.addView(addAttribute, lp(MATCH_PARENT, WRAP_CONTENT));

        activeConditionsView = column(context);
        filters.addView(activeConditionsView, lp(MATCH_PARENT, WRAP_CONTENT));
        renderConditions();

        Button clear = button(context, "🧹 重置所有筛选");
        clear.setOnClickListener(view -> clearFilters());
        filters.addView(clear, lp(MATCH_PARENT, WRAP_CONTENT));
    }

    private void addEnchantmentCondition() {
        String id = enchantmentChoice.key();
        if (id.isEmpty()) {
            showMessage("请先选择附魔种类！", ERROR);
            return;
        }
        String input = enchantmentMinimum.getText().toString().trim();
        int minimum;
        try {
            minimum = input.isEmpty() ? 1 : Integer.parseInt(input);
            if (minimum < 1) {
                showMessage("附魔等级至少为 1", ERROR);
                return;
            }
        } catch (NumberFormatException exception) {
            showMessage("附魔等级必须是整数", ERROR);
            return;
        }

        final int threshold = minimum;
        String label = enchantmentChoice.label() + " ≥ " + threshold + "级";
        conditions.add(new NamedCondition(label, data -> data.enchantments().stream().anyMatch(value ->
                value.id().equals(id) && value.level() >= threshold)));
        renderConditions();
        changed();
    }

    private void addAttributeCondition() {
        String id = attributeChoice.key();
        if (id.isEmpty()) {
            showMessage("请先选择属性！", ERROR);
            return;
        }
        String operationKey = operationChoice.key();
        if (operationKey.isEmpty()) {
            showMessage("请选择加成方式！", ERROR);
            return;
        }
        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(operationKey);
        String comparison = comparisonChoice.key();
        if (comparison.isEmpty()) {
            String label = attributeChoice.label() + " · " + operationChoice.label();
            conditions.add(new NamedCondition(label, data -> data.attributes().stream().anyMatch(value ->
                    value.id().equals(id) && value.operation() == operation)));
            renderConditions();
            changed();
            return;
        }

        String input = attributeMinimum.getText().toString().trim();
        double entered;
        try {
            entered = Double.parseDouble(input);
            if (!Double.isFinite(entered)) {
                showMessage("请输入有效数值", ERROR);
                return;
            }
        } catch (NumberFormatException exception) {
            showMessage("属性数值请输入数字", ERROR);
            return;
        }

        boolean percentage = operation != AttributeModifier.Operation.ADD_VALUE;
        double threshold = percentage ? entered / 100.0 : entered;
        String compSign = comparison.equals("ge") ? "≥" : comparison.equals("le") ? "≤" : "=";
        String label = attributeChoice.label() + " " + compSign + " " + input + (percentage ? "%" : "");

        conditions.add(new NamedCondition(label, data -> data.attributes().stream().anyMatch(value ->
                value.id().equals(id) && value.operation() == operation && compare(value.amount(), threshold, comparison))));
        renderConditions();
        changed();
    }

    private static boolean compare(double actual, double threshold, String comparison) {
        return switch (comparison) {
            case "ge" -> actual >= threshold;
            case "le" -> actual <= threshold;
            case "eq" -> Math.abs(actual - threshold) <= 1.0E-6;
            default -> true;
        };
    }

    private void renderConditions() {
        if (activeConditionsView == null) return;
        activeConditionsView.removeAllViews();
        if (conditions.isEmpty()) return;

        for (NamedCondition condition : List.copyOf(conditions)) {
            LinearLayout line = row(requireContext());
            TextView label = text(requireContext(), "• " + condition.label(), ACCENT);
            line.addView(label, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
            Button remove = button(requireContext(), "×");
            remove.setTextColor(ERROR);
            remove.setOnClickListener(view -> {
                conditions.remove(condition);
                renderConditions();
                changed();
            });
            line.addView(remove, lp(WRAP_CONTENT, WRAP_CONTENT));
            activeConditionsView.addView(line, lp(MATCH_PARENT, WRAP_CONTENT));
        }
    }

    private void clearFilters() {
        if (search != null) search.setText("");
        materialChoice.select("");
        rarityChoice.select("");
        categoryChoice.select("");
        durabilityChoice.select("");
        enchantedChoice.select("");
        sortChoice.select("rarity");
        enchantmentChoice.select("");
        attributeChoice.select("");
        operationChoice.select("ADD_VALUE");
        comparisonChoice.select("");
        if (enchantmentMinimum != null) enchantmentMinimum.setText("");
        if (attributeMinimum != null) attributeMinimum.setText("");
        conditions.clear();
        renderConditions();
        changed();
    }

    private void transfer(EquipmentIndex.Row requested) {
        LinearLayout target = root;
        if (target == null) {
            transferPending = false;
            return;
        }

        long ticket = generation;
        Minecraft.getInstance().execute(() -> {
            String result = "操作完成。";
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (ticket != generation || minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != menu) {
                    return;
                }

                int slot = requested.menuSlot();
                ItemStack expected = cachedStacks.get(slot);
                EquipmentIndex.Row currentRow = cachedRows.get(slot);

                if (slot < 0 || slot >= menu.slots.size() || expected == null || !requested.equals(currentRow)
                        || !ItemStack.matches(expected, menu.slots.get(slot).getItem())) {
                    result = "装备信息发生变动，请重试。";
                } else {
                    minecraft.gameMode.handleInventoryMouseClick(
                            menu.containerId, slot, 0, ClickType.QUICK_MOVE, minecraft.player
                    );
                    playUiClickSound();
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to transfer equipment item", exception);
                result = "存取失败。";
            } finally {
                String message = result;
                target.post(() -> {
                    if (ticket != generation || root != target) return;
                    transferPending = false;
                    renderRows();
                    showMessage(message, MUTED);
                });
            }
        });
    }

    private void requestSnapshot() {
        LinearLayout target = root;
        if (target == null) return;
        target.postDelayed(refresh, 250);
        if (!refreshPending.compareAndSet(false, true)) return;

        long ticket = generation;
        Minecraft.getInstance().execute(() -> {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (ticket != generation || minecraft.player == null || minecraft.player.containerMenu != menu) {
                    return;
                }

                boolean dirty = forceIndex;
                if (forceIndex) {
                    cachedStacks.clear();
                    cachedRows.clear();
                    forceIndex = false;
                }

                for (int slot = 0; slot < menu.slots.size(); slot++) {
                    ItemStack stack = menu.slots.get(slot).getItem();
                    if (stack.isEmpty() || !EquipmentCabinetBlockEntity.accepts(stack)) {
                        if (cachedStacks.remove(slot) != null) dirty = true;
                        if (cachedRows.remove(slot) != null) dirty = true;
                        continue;
                    }

                    ItemStack old = cachedStacks.get(slot);
                    if (old == null || !ItemStack.matches(old, stack)) {
                        EquipmentIndex.Row data = EquipmentIndex.read(slot, stack);
                        cachedStacks.put(slot, stack.copy());
                        cachedRows.put(slot, data);
                        dirty = true;
                    }
                }

                indexFailureLogged = false;
                if (dirty) {
                    List<EquipmentIndex.Row> result = cachedRows.values().stream()
                            .sorted(Comparator.comparingInt(EquipmentIndex.Row::menuSlot))
                            .toList();

                    target.post(() -> {
                        if (ticket != generation || root != target) return;
                        snapshot = result;
                        refreshOptions();
                        renderRows();
                    });
                }
            } catch (RuntimeException exception) {
                forceIndex = true;
                if (!indexFailureLogged) {
                    LOGGER.error("Failed to index equipment cabinet", exception);
                    indexFailureLogged = true;
                }
            } finally {
                refreshPending.set(false);
            }
        });
    }

    private void refreshOptions() {
        if (materialChoice == null) return;
        Map<String, String> materials = new LinkedHashMap<>();
        Map<String, String> rarities = new LinkedHashMap<>();
        Map<String, String> categories = new LinkedHashMap<>();
        Map<String, String> enchantments = new LinkedHashMap<>();
        Map<String, String> attributes = new LinkedHashMap<>();

        for (EquipmentIndex.Row data : snapshot) {
            materials.putIfAbsent(materialKey(data), EquipmentDisplay.materialName(data.materials()));
            rarities.putIfAbsent(data.rarityId(), EquipmentDisplay.friendly(data.rarityName(), "未知品质"));
            categories.putIfAbsent(data.categoryId(), EquipmentDisplay.friendly(data.categoryName(), "其他装备"));

            for (EquipmentIndex.EnchantmentValue enchantment : data.enchantments()) {
                enchantments.putIfAbsent(enchantment.id(), EquipmentDisplay.friendly(enchantment.name(), "未命名附魔"));
            }
            for (EquipmentIndex.AttributeValue attribute : data.attributes()) {
                attributes.putIfAbsent(attribute.id(), EquipmentDisplay.friendly(attribute.name(), "未命名属性"));
            }
        }

        materialChoice.setChoices(toChoices(materials));
        rarityChoice.setChoices(toChoices(rarities));
        categoryChoice.setChoices(toChoices(categories));
        enchantmentChoice.setChoices(toChoices(enchantments));
        attributeChoice.setChoices(toChoices(attributes));
    }

    private static List<Choice> toChoices(Map<String, String> names) {
        return names.entrySet().stream()
                .map(entry -> new Choice(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Choice::label).thenComparing(Choice::key))
                .toList();
    }

    private void playUiClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void playSalvageSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1.2F));
    }

    @Override
    public void onViewCreated(View view, DataSet savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        generation++;
        Minecraft.getInstance().execute(() -> forceIndex = true);
        root.post(refresh);
    }

    @Override
    public void onDestroyView() {
        generation++;
        clearHoveredItem();
        if (root != null) {
            root.removeCallbacks(refresh);
            root.removeCallbacks(filterChanged);
        }
        allChoiceFields.clear();
        root = null;
        table = null;
        tableScroll = null;
        status = null;
        pageLabel = null;
        capacityLabel = null;
        activeConditionsView = null;
        tableHeader = null;
        sourceButton = null;
        viewModeButton = null;
        dumpOrExtractButton = null;
        salvageFilteredButton = null;
        salvageSafetyButton = null;
        quickSortButton = null;
        previous = null;
        next = null;
        search = null;
        super.onDestroyView();
    }

    private void changed() {
        if (root == null) return;
        root.removeCallbacks(filterChanged);
        root.postDelayed(filterChanged, 120);
    }

    private void showMessage(String message, int color) {
        if (status == null) return;
        status.setTextColor(color);
        status.setText(message);
    }

    private EditText field(LinearLayout parent, String label, String hint, Runnable onChanged) {
        Context context = parent.getContext();
        parent.addView(text(context, label, TEXT), lp(MATCH_PARENT, WRAP_CONTENT));
        EditText input = new EditText(context);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setTextSize(13);
        input.setSingleLine();
        input.setHint(hint);
        input.setPadding(input.dp(5), input.dp(5), input.dp(5), input.dp(5));

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged.run(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        parent.addView(input, lp(MATCH_PARENT, WRAP_CONTENT));
        return input;
    }

    private static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBaselineAligned(false);
        return layout;
    }

    private static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBaselineAligned(false);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private static ScrollView scroll(Context context, View child) {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(child, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        return scroll;
    }

    private static TextView text(Context context, String value, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(13);
        view.setTextColor(color);
        view.setPadding(view.dp(5), view.dp(5), view.dp(5), view.dp(5));
        return view;
    }

    private static Button button(Context context, String value) {
        Button button = new Button(context);
        button.setText(value);
        button.setTextSize(13);
        button.setTextColor(TEXT);
        button.setPadding(button.dp(8), button.dp(6), button.dp(8), button.dp(6));
        return button;
    }

    private static TextView addCell(LinearLayout line, String value, float weight, int color) {
        TextView cell = text(line.getContext(), value, color);
        cell.setSingleLine();
        cell.setEllipsize(TextUtils.TruncateAt.END);
        line.addView(cell, new LinearLayout.LayoutParams(0, WRAP_CONTENT, weight));
        return cell;
    }

    // ==================== 互斥展开的下拉筛选组件 ====================

    private final class ChoiceField {
        private static final int OPTION_PAGE_SIZE = 10;
        private final String title;
        private final String emptyLabel;
        private final Runnable onChanged;
        private final Button selectedButton;
        private final LinearLayout panel;
        private final EditText find;
        private final LinearLayout choicesView;
        private final Button optionsPrevious;
        private final Button optionsNext;
        private final TextView optionsPageLabel;

        private List<Choice> choices = List.of();
        private String selectedKey = "";
        private String selectedLabel;
        private int optionPage;

        private ChoiceField(LinearLayout parent, String title, String emptyLabel, Runnable onChanged) {
            this.title = title;
            this.emptyLabel = emptyLabel;
            this.onChanged = onChanged;
            this.selectedLabel = emptyLabel;
            Context context = parent.getContext();

            allChoiceFields.add(this);

            selectedButton = button(context, title + "：" + emptyLabel + " ▾");
            selectedButton.setTextColor(ACCENT);
            parent.addView(selectedButton, lp(MATCH_PARENT, WRAP_CONTENT));

            panel = column(context);
            panel.setVisibility(View.GONE);
            parent.addView(panel, lp(MATCH_PARENT, WRAP_CONTENT));

            find = new EditText(context);
            find.setSingleLine();
            find.setTextSize(13);
            find.setTextColor(TEXT);
            find.setHintTextColor(MUTED);
            find.setHint("输入名称检索");
            find.setPadding(find.dp(5), find.dp(5), find.dp(5), find.dp(5));
            panel.addView(find, lp(MATCH_PARENT, WRAP_CONTENT));

            choicesView = column(context);
            panel.addView(choicesView, lp(MATCH_PARENT, WRAP_CONTENT));

            LinearLayout pager = row(context);
            optionsPrevious = button(context, "◀");
            optionsPrevious.setOnClickListener(view -> {
                if (optionPage > 0) {
                    optionPage--;
                    renderChoices();
                }
            });
            pager.addView(optionsPrevious, lp(WRAP_CONTENT, WRAP_CONTENT));

            optionsPageLabel = text(context, "", MUTED);
            optionsPageLabel.setGravity(Gravity.CENTER);
            pager.addView(optionsPageLabel, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));

            optionsNext = button(context, "▶");
            optionsNext.setOnClickListener(view -> {
                optionPage++;
                renderChoices();
            });
            pager.addView(optionsNext, lp(WRAP_CONTENT, WRAP_CONTENT));
            panel.addView(pager, lp(MATCH_PARENT, WRAP_CONTENT));

            selectedButton.setOnClickListener(view -> {
                boolean willOpen = panel.getVisibility() != View.VISIBLE;
                if (willOpen) {
                    for (ChoiceField other : allChoiceFields) {
                        if (other != ChoiceField.this) {
                            other.close();
                        }
                    }
                    panel.setVisibility(View.VISIBLE);
                    renderChoices();
                } else {
                    close();
                }
            });

            find.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    optionPage = 0;
                    renderChoices();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            renderChoices();
        }

        private void close() {
            panel.setVisibility(View.GONE);
            find.clearFocus();
        }

        private String key() { return selectedKey; }
        private String label() { return selectedLabel; }

        private void select(String key) {
            if (key.isEmpty()) {
                selectedKey = "";
                selectedLabel = emptyLabel;
            } else {
                Choice found = choices.stream().filter(c -> c.key().equals(key)).findFirst().orElse(null);
                if (found == null) return;
                selectedKey = found.key();
                selectedLabel = found.label();
            }
            updateButton();
            close();
        }

        private void setChoices(List<Choice> newChoices) {
            choices = List.copyOf(newChoices);
            if (!selectedKey.isEmpty()) {
                choices.stream().filter(c -> c.key().equals(selectedKey)).findFirst().ifPresent(c -> selectedLabel = c.label());
            }
            updateButton();
            if (panel.getVisibility() == View.VISIBLE) renderChoices();
        }

        private void updateButton() { selectedButton.setText(title + "：" + selectedLabel + " ▾"); }

        private void choose(String key, String label) {
            selectedKey = key;
            selectedLabel = label;
            updateButton();
            close();
            onChanged.run();
        }

        private void renderChoices() {
            choicesView.removeAllViews();
            Button any = button(choicesView.getContext(), (selectedKey.isEmpty() ? "✓ " : "") + emptyLabel);
            any.setTextColor(selectedKey.isEmpty() ? ACCENT : TEXT);
            any.setOnClickListener(view -> choose("", emptyLabel));
            choicesView.addView(any, lp(MATCH_PARENT, WRAP_CONTENT));

            String query = find.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<Choice> matching = choices.stream().filter(c -> c.label().toLowerCase(Locale.ROOT).contains(query)).toList();

            int pageCount = Math.max(1, (matching.size() + OPTION_PAGE_SIZE - 1) / OPTION_PAGE_SIZE);
            optionPage = Math.max(0, Math.min(optionPage, pageCount - 1));

            int start = optionPage * OPTION_PAGE_SIZE;
            int end = Math.min(matching.size(), start + OPTION_PAGE_SIZE);

            for (int index = start; index < end; index++) {
                Choice choice = matching.get(index);
                boolean selected = choice.key().equals(selectedKey);
                Button option = button(choicesView.getContext(), (selected ? "✓ " : "") + choice.label());
                option.setTextColor(selected ? ACCENT : TEXT);
                option.setTooltipText(choice.label());
                option.setOnClickListener(view -> choose(choice.key(), choice.label()));
                choicesView.addView(option, lp(MATCH_PARENT, WRAP_CONTENT));
            }

            optionsPageLabel.setText((optionPage + 1) + "/" + pageCount);
            optionsPrevious.setEnabled(optionPage > 0);
            optionsNext.setEnabled(optionPage + 1 < pageCount);
        }
    }
}