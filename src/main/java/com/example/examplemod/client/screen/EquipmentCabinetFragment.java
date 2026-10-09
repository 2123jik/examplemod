// EquipmentCabinetFragment.java
package com.example.examplemod.client.screen;

import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import com.example.examplemod.menu.EquipmentCabinetMenu;
import com.example.examplemod.util.EquipmentFilter;
import com.example.examplemod.util.EquipmentIndex;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * UI 控件只在 Modern UI 线程访问。
 * Menu、ItemStack、注册表查询和网络发送只在 Minecraft 主线程访问。
 *
 * 不依赖 RecyclerView。
 * ScrollView + 每页最多 32 行，避免几千个 TextView 同时布局。
 *
 * 为降低渲染线程耦合，列表采用纯文本列，不创建 MinecraftSurfaceView。
 * 物品名称、数字耐久、附魔数量、稀有度和分类足够完成主要筛选。
 *
 * 材质/稀有度/分类支持名称或 ID 子串；逗号表示任选其一。
 * 附魔/属性支持分号组合数值条件。
 */
public final class EquipmentCabinetFragment extends Fragment implements ScreenCallback {
    private static final int PAGE_SIZE = 32;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int ACCENT = 0xFF8CC8FF;
    private static final int ERROR = 0xFFFF8888;

    private final EquipmentCabinetMenu menu;

    // 以下两个映射仅在 Minecraft 主线程访问。
    private final Map<Integer, ItemStack> cachedStacks = new HashMap<>();
    private final Map<Integer, EquipmentIndex.Row> cachedRows = new HashMap<>();
    private boolean forceIndex = true;

    // UI 线程状态。
    private LinearLayout root;
    private LinearLayout table;
    private ScrollView tableScroll;
    private TextView status;
    private TextView details;
    private TextView pageLabel;
    private TextView options;
    private EditText search;
    private EditText material;
    private EditText rarity;
    private EditText category;
    private EditText enchantments;
    private EditText attributes;
    private Button previous;
    private Button next;

    private EquipmentFilter.Durability durability = EquipmentFilter.Durability.ANY;
    private EquipmentFilter.Enchanted enchanted = EquipmentFilter.Enchanted.ANY;
    private int sortMode;
    private int page;
    private boolean playerInventory;
    private int selectedSlot = -1;
    private List<EquipmentIndex.Row> snapshot = List.of();

    // 跨线程生命周期标记。
    private volatile long generation;
    private final AtomicBoolean refreshPending = new AtomicBoolean();

    private final Runnable refresh = this::requestSnapshot;
    private final Runnable filterChanged = () -> {
        page = 0;
        renderRows();
    };

    public EquipmentCabinetFragment(EquipmentCabinetMenu menu) {
        this.menu = menu;
    }

    /**
     * ESC 退出；输入搜索文本时不会因为输入背包快捷键 E 而关闭界面。
     * Modern UI 自己处理编辑框聚焦时的 ESC。
     */
    @Override
    public boolean isBackKey(int keyCode, KeyEvent event) {
        return keyCode == 256;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState
    ) {
        Context context = requireContext();
        root = column(context);
        root.setPadding(root.dp(12), root.dp(12), root.dp(12), root.dp(12));

        LinearLayout toolbar = row(context);
        root.addView(toolbar, lp(MATCH_PARENT, WRAP_CONTENT));

        Button source = button(context, "查看：装备柜");
        source.setOnClickListener(view -> {
            playerInventory = !playerInventory;
            source.setText(playerInventory ? "查看：玩家背包" : "查看：装备柜");
            selectedSlot = -1;
            page = 0;
            details.setText("点击“详情”查看装备；点击操作按钮整件存取。");
            renderRows();
        });
        toolbar.addView(source, lp(WRAP_CONTENT, WRAP_CONTENT));

        Button durabilityButton = button(context, durability.label);
        durabilityButton.setOnClickListener(view -> {
            var values = EquipmentFilter.Durability.values();
            durability = values[(durability.ordinal() + 1) % values.length];
            durabilityButton.setText(durability.label);
            changed();
        });
        toolbar.addView(durabilityButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        Button enchantedButton = button(context, enchanted.label);
        enchantedButton.setOnClickListener(view -> {
            var values = EquipmentFilter.Enchanted.values();
            enchanted = values[(enchanted.ordinal() + 1) % values.length];
            enchantedButton.setText(enchanted.label);
            changed();
        });
        toolbar.addView(enchantedButton, lp(WRAP_CONTENT, WRAP_CONTENT));

        Button sort = button(context, "排序：名称");
        sort.setOnClickListener(view -> {
            sortMode = (sortMode + 1) % 4;
            sort.setText(switch (sortMode) {
                case 1 -> "排序：耐久降序";
                case 2 -> "排序：稀有度降序";
                case 3 -> "排序：附魔数量降序";
                default -> "排序：名称";
            });
            changed();
        });
        toolbar.addView(sort, lp(WRAP_CONTENT, WRAP_CONTENT));

        Button rebuild = button(context, "重建索引");
        rebuild.setOnClickListener(view -> {
            Minecraft.getInstance().execute(() -> forceIndex = true);
            status.setText("已请求重建索引");
        });
        toolbar.addView(rebuild, lp(WRAP_CONTENT, WRAP_CONTENT));

        status = text(context, "等待物品同步……", MUTED);
        root.addView(status, lp(MATCH_PARENT, WRAP_CONTENT));

        LinearLayout body = row(context);
        root.addView(body, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));

        LinearLayout filters = column(context);
        ScrollView filterScroll = scroll(context, filters);
        body.addView(filterScroll, new LinearLayout.LayoutParams(root.dp(245), MATCH_PARENT));

        search = field(filters, "搜索名称 / 物品 ID / 词缀", "例如：铁木");
        material = field(filters, "材质 / 系列", "钻石,铁木");
        rarity = field(filters, "神化稀有度", "名称或 ID；none 表示无");
        category = field(filters, "LootCategory", "名称或 ID");
        enchantments = field(filters, "包含附魔，分号表示同时满足", "锋利>=4;耐久>=3");
        attributes = field(filters, "属性修饰符，分号表示同时满足", "攻击伤害>=7");

        TextView help = text(context,
                "留空表示不限。\n"
                        + "属性按单条修饰符匹配。\n"
                        + "@ADD_VALUE：加值\n"
                        + "@ADD_MULTIPLIED_BASE：基础乘算\n"
                        + "@ADD_MULTIPLIED_TOTAL：总乘算\n"
                        + "乘算输入 0.1 表示 10%。\n"
                        + "例如：攻击速度@ADD_VALUE>-3\n"
                        + "耐久筛选不把“不可破坏”视为满耐久。",
                MUTED);
        filters.addView(help, lp(MATCH_PARENT, WRAP_CONTENT));

        Button clear = button(context, "清空筛选");
        clear.setOnClickListener(view -> {
            search.setText("");
            material.setText("");
            rarity.setText("");
            category.setText("");
            enchantments.setText("");
            attributes.setText("");
            durability = EquipmentFilter.Durability.ANY;
            enchanted = EquipmentFilter.Enchanted.ANY;
            durabilityButton.setText(durability.label);
            enchantedButton.setText(enchanted.label);
            changed();
        });
        filters.addView(clear, lp(MATCH_PARENT, WRAP_CONTENT));

        options = text(context, "", MUTED);
        filters.addView(options, lp(MATCH_PARENT, WRAP_CONTENT));

        LinearLayout content = column(context);
        body.addView(content, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));

        LinearLayout header = row(context);
        addCell(header, "装备名称", 3.0F, TEXT);
        addCell(header, "材质", 1.3F, TEXT);
        addCell(header, "耐久", 1.2F, TEXT);
        addCell(header, "附魔", 0.6F, TEXT);
        addCell(header, "稀有度", 1.3F, TEXT);
        addCell(header, "分类", 1.3F, TEXT);
        addCell(header, "详情", 0.8F, TEXT);
        addCell(header, "存取", 0.8F, TEXT);
        content.addView(header, lp(MATCH_PARENT, WRAP_CONTENT));

        table = column(context);
        tableScroll = scroll(context, table);
        content.addView(tableScroll, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 3));

        LinearLayout pager = row(context);
        previous = button(context, "上一页");
        previous.setOnClickListener(view -> {
            if (page > 0) {
                page--;
                tableScroll.scrollTo(0, 0);
                renderRows();
            }
        });
        pager.addView(previous, lp(WRAP_CONTENT, WRAP_CONTENT));

        pageLabel = text(context, "", MUTED);
        pager.addView(pageLabel, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));

        next = button(context, "下一页");
        next.setOnClickListener(view -> {
            page++;
            tableScroll.scrollTo(0, 0);
            renderRows();
        });
        pager.addView(next, lp(WRAP_CONTENT, WRAP_CONTENT));
        content.addView(pager, lp(MATCH_PARENT, WRAP_CONTENT));

        details = text(context, "点击“详情”查看装备；点击操作按钮整件存取。", TEXT);
        details.setTextIsSelectable(true);
        ScrollView detailsScroll = scroll(context, details);
        content.addView(detailsScroll, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 2));

        return root;
    }

    @Override
    public void onViewCreated(View view, DataSet savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        generation++;
        root.post(refresh);
    }

    @Override
    public void onDestroyView() {
        generation++;

        if (root != null) {
            root.removeCallbacks(refresh);
            root.removeCallbacks(filterChanged);
        }

        root = null;
        table = null;
        tableScroll = null;
        details = null;

        super.onDestroyView();
    }

    private void changed() {
        if (root == null) return;
        root.removeCallbacks(filterChanged);
        root.postDelayed(filterChanged, 120);
    }

    /**
     * 每 250ms 检查同步槽位，但只重新提取发生变化的 ItemStack。
     * 数据包重载后可按“重建索引”刷新未改变物品对应的动态定义。
     */
    private void requestSnapshot() {
        LinearLayout target = root;
        if (target == null) return;

        target.postDelayed(refresh, 250);
        if (!refreshPending.compareAndSet(false, true)) return;

        long ticket = generation;

        Minecraft.getInstance().execute(() -> {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (ticket != generation || minecraft.player == null
                        || minecraft.player.containerMenu != menu) {
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
                        // 提取失败时不缓存新 stack，下次仍可重试。
                        EquipmentIndex.Row data = EquipmentIndex.read(slot, stack);
                        cachedStacks.put(slot, stack.copy());
                        cachedRows.put(slot, data);
                        dirty = true;
                    }
                }

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
                String message = exception.getClass().getSimpleName()
                        + ": " + String.valueOf(exception.getMessage());

                target.post(() -> {
                    if (ticket != generation || root != target) return;
                    status.setTextColor(ERROR);
                    status.setText("索引失败：" + message);
                });
            } finally {
                refreshPending.set(false);
            }
        });
    }

    private EquipmentFilter currentFilter() {
        return new EquipmentFilter(
                search.getText().toString(),
                material.getText().toString(),
                durability,
                enchanted,
                rarity.getText().toString(),
                category.getText().toString(),
                EquipmentFilter.parseRules(enchantments.getText().toString(), false),
                EquipmentFilter.parseRules(attributes.getText().toString(), true)
        );
    }

    private void renderRows() {
        if (table == null) return;

        EquipmentFilter filter;
        try {
            filter = currentFilter();
        } catch (IllegalArgumentException exception) {
            table.removeAllViews();
            status.setTextColor(ERROR);
            status.setText(exception.getMessage());
            previous.setEnabled(false);
            next.setEnabled(false);
            pageLabel.setText("筛选条件有误");
            return;
        }

        Comparator<EquipmentIndex.Row> comparator = switch (sortMode) {
            case 1 -> Comparator.comparingDouble(
                    (EquipmentIndex.Row row) ->
                            row.hasDurability() ? row.durabilityPercent() : -1
            ).reversed();
            case 2 -> Comparator.comparingInt(EquipmentIndex.Row::rarityOrder).reversed();
            case 3 -> Comparator.comparingInt(
                    (EquipmentIndex.Row row) -> row.enchantments().size()
            ).reversed();
            default -> Comparator.comparing(
                    EquipmentIndex.Row::name, String.CASE_INSENSITIVE_ORDER
            );
        };

        List<EquipmentIndex.Row> source = snapshot.stream()
                .filter(row -> (row.menuSlot() >= EquipmentCabinetMenu.PLAYER_START)
                        == playerInventory)
                .toList();

        List<EquipmentIndex.Row> filtered = source.stream()
                .filter(filter)
                .sorted(comparator.thenComparingInt(EquipmentIndex.Row::menuSlot))
                .toList();

        int pages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));

        status.setTextColor(MUTED);
        status.setText((playerInventory ? "玩家背包" : "装备柜")
                + "：匹配 " + filtered.size() + " / " + source.size()
                + " 件；" + (playerInventory ? "点击存入" : "点击取出"));

        table.removeAllViews();
        int start = page * PAGE_SIZE;
        int end = Math.min(filtered.size(), start + PAGE_SIZE);

        for (int i = start; i < end; i++) {
            EquipmentIndex.Row data = filtered.get(i);
            LinearLayout line = row(requireContext());

            TextView name = addCell(line, data.name(), 3.0F, TEXT);
            name.setTooltipText(data.itemId());

            String materialText = String.join("/", new TreeSet<>(data.materials()));
            TextView materialCell = addCell(line, materialText, 1.3F, MUTED);
            materialCell.setTooltipText(materialText);

            addCell(line, data.durabilityText(), 1.2F,
                    data.hasDurability() && data.durabilityPercent() < 20 ? ERROR : TEXT);
            addCell(line, Integer.toString(data.enchantments().size()), 0.6F, TEXT);
            addCell(line, data.rarityName(), 1.3F, data.rarityColor());
            TextView categoryCell = addCell(line, data.categoryName(), 1.3F, MUTED);
            categoryCell.setTooltipText(data.categoryId());

            Button inspect = button(requireContext(), "详情");
            inspect.setTextColor(ACCENT);
            inspect.setOnClickListener(view -> {
                selectedSlot = data.menuSlot();
                details.setText(data.details());
            });
            line.addView(inspect, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 0.8F));

            Button transfer = button(requireContext(), playerInventory ? "存入" : "取出");
            transfer.setTextColor(ACCENT);
            transfer.setOnClickListener(view -> {
                transfer.setEnabled(false);
                transfer(data);
            });
            line.addView(transfer, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 0.8F));

            table.addView(line, lp(MATCH_PARENT, WRAP_CONTENT));
        }

        if (filtered.isEmpty()) {
            table.addView(text(requireContext(), "没有符合条件的装备", MUTED),
                    lp(MATCH_PARENT, WRAP_CONTENT));
        }

        pageLabel.setText("第 " + (page + 1) + " / " + pages + " 页");
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);

        if (selectedSlot >= 0) {
            EquipmentIndex.Row selected = snapshot.stream()
                    .filter(row -> row.menuSlot() == selectedSlot)
                    .findFirst().orElse(null);

            if (selected == null) {
                selectedSlot = -1;
                details.setText("该装备已移动。");
            } else {
                details.setText(selected.details());
            }
        }
    }

    private void refreshOptions() {
        TreeSet<String> materials = new TreeSet<>();
        TreeSet<String> rarities = new TreeSet<>();
        TreeSet<String> categories = new TreeSet<>();

        for (EquipmentIndex.Row data : snapshot) {
            materials.addAll(data.materials());
            rarities.add(data.rarityName() + " [" + data.rarityId() + "]");
            categories.add(data.categoryName() + " [" + data.categoryId() + "]");
        }

        options.setText("\n当前可用材质：\n" + String.join("\n", materials)
                + "\n\n当前稀有度：\n" + String.join("\n", rarities)
                + "\n\n当前分类：\n" + String.join("\n", categories));
    }

    private void transfer(EquipmentIndex.Row requested) {
        LinearLayout target = root;
        long ticket = generation;

        Minecraft.getInstance().execute(() -> {
            String result = "已发送存取请求；目标空间不足时装备会留在原处。";

            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (ticket != generation || minecraft.player == null
                        || minecraft.gameMode == null
                        || minecraft.player.containerMenu != menu) {
                    return;
                }

                int slot = requested.menuSlot();

                // 防止 UI 的旧行恰好指向已被替换的客户端槽位。
                ItemStack expected = cachedStacks.get(slot);
                EquipmentIndex.Row currentRow = cachedRows.get(slot);

                if (slot < 0 || slot >= menu.slots.size()
                        || expected == null
                        || !requested.equals(currentRow)
                        || !ItemStack.matches(expected, menu.slots.get(slot).getItem())) {
                    result = "装备已变化，请刷新后重试。";
                } else {
                    minecraft.gameMode.handleInventoryMouseClick(
                            menu.containerId, slot, 0,
                            ClickType.QUICK_MOVE, minecraft.player
                    );
                }
            } finally {
                String message = result;
                if (target != null) {
                    target.post(() -> {
                        if (ticket != generation || root != target) return;
                        renderRows();
                        status.setText(message);
                    });
                }
            }
        });
    }

    private EditText field(LinearLayout parent, String label, String hint) {
        Context context = requireContext();
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
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changed();
            }

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

    private static TextView addCell(
            LinearLayout line, String value, float weight, int color
    ) {
        TextView cell = text(line.getContext(), value, color);
        cell.setSingleLine();
        cell.setEllipsize(TextUtils.TruncateAt.END);
        line.addView(cell, new LinearLayout.LayoutParams(0, WRAP_CONTENT, weight));
        return cell;
    }
}
