package com.example.examplemod.skill.gui;

import com.example.examplemod.skill.SkillHistoryManager;
import com.example.examplemod.skill.SkillNode;
import com.example.examplemod.skill.SkillRegistry;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.ImageStore;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.Consumer;

public class SkillNodeInspector extends LinearLayout {

    private final EditText etId;
    private final EditText etMaxRank;
    private final TextView tvParents;

    // 法术卡片
    private final View spellIconPreview;
    private final TextView tvSpellName;
    private final TextView tvSpellDesc;

    // 属性加成卡片
    private final LinearLayout bonusListContainer;
    private final Button btnSelectAttr;
    private final EditText etBonusVal;

    // Operation 切换按钮
    private AttributeModifier.Operation selectedOp = AttributeModifier.Operation.ADD_VALUE;
    private final Button btnOpAdd;
    private final Button btnOpMulBase;
    private final Button btnOpMulTotal;
    private ResourceLocation currentSelectedAttr = null;

    private final SkillHistoryManager historyManager;
    private final Runnable onDataChanged;
    private final Consumer<Consumer<AbstractSpell>> requestSpellPicker;
    private final Consumer<Consumer<ResourceLocation>> requestAttrPicker;

    private SkillNode boundNode;

    public SkillNodeInspector(
            Context context,
            SkillHistoryManager historyManager,
            Runnable onDataChanged,
            Consumer<Consumer<AbstractSpell>> requestSpellPicker,
            Consumer<Consumer<ResourceLocation>> requestAttrPicker
    ) {
        super(context);
        this.historyManager = historyManager;
        this.onDataChanged = onDataChanged;
        this.requestSpellPicker = requestSpellPicker;
        this.requestAttrPicker = requestAttrPicker;

        setOrientation(LinearLayout.VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));

        ShapeDrawable bg = new ShapeDrawable();
        bg.setColor(Color.argb(240, 16, 18, 22));
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1.2f), Color.argb(180, 255, 215, 0));
        setBackground(bg);

        // ================= 1. 顶部 Header =================
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("节点检查器 (Inspector)");
        title.setTextSize(14);
        title.setTextColor(Color.argb(255, 255, 215, 0));
        header.addView(title, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnUndo = new Button(context);
        btnUndo.setText("↶ 撤销");
        btnUndo.setTextSize(11);
        btnUndo.setOnClickListener(v -> {
            if (historyManager.undo()) {
                onDataChanged.run();
                bind(boundNode != null ? SkillRegistry.NODES.get(boundNode.getId()) : null);
            }
        });
        header.addView(btnUndo);
        addView(header);

        // ================= 2. 中间滚动区域 =================
        ScrollView scrollView = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // 卡片 A: 基础信息
        content.addView(createSectionHeader(context, "基础信息"));
        content.addView(createLabel(context, "节点唯一标识 (ID):"));
        etId = new EditText(context);
        content.addView(etId);

        content.addView(createLabel(context, "最大等级 (Max Rank):"));
        etMaxRank = new EditText(context);
        content.addView(etMaxRank);

        content.addView(createLabel(context, "前置依赖 (右键从父节点拉线绑定):"));
        tvParents = new TextView(context);
        tvParents.setTextSize(11);
        tvParents.setTextColor(Color.LTGRAY);
        content.addView(tvParents);

        // 卡片 B: 法术绑定
        content.addView(createSectionHeader(context, "关联法术绑定"));
        LinearLayout spellCard = new LinearLayout(context);
        spellCard.setOrientation(LinearLayout.HORIZONTAL);
        spellCard.setGravity(Gravity.CENTER_VERTICAL);
        spellCard.setPadding(dp(8), dp(8), dp(8), dp(8));
        ShapeDrawable scBg = new ShapeDrawable();
        scBg.setColor(Color.argb(120, 30, 35, 45));
        scBg.setCornerRadius(dp(6));
        spellCard.setBackground(scBg);

        spellIconPreview = new View(context) {
            private final RectF dst = new RectF();
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (boundNode != null && boundNode.getIcon() != null) {
                    Image img = ImageStore.getInstance().getOrCreate(boundNode.getIcon());
                    if (img != null) {
                        dst.set(0, 0, getWidth(), getHeight());
                        canvas.drawImage(img, null, dst, null);
                    }
                }
            }
        };
        int iconSize = dp(36);
        spellCard.addView(spellIconPreview, new LayoutParams(iconSize, iconSize));

        LinearLayout spellInfo = new LinearLayout(context);
        spellInfo.setOrientation(LinearLayout.VERTICAL);
        spellInfo.setPadding(dp(8), 0, dp(8), 0);

        tvSpellName = new TextView(context);
        tvSpellName.setTextSize(12);
        tvSpellName.setTextColor(Color.argb(255, 255, 215, 0));
        spellInfo.addView(tvSpellName);

        tvSpellDesc = new TextView(context);
        tvSpellDesc.setTextSize(10);
        tvSpellDesc.setTextColor(Color.GRAY);
        spellInfo.addView(tvSpellDesc);

        spellCard.addView(spellInfo, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnPickSpell = new Button(context);
        btnPickSpell.setText("选择法术");
        btnPickSpell.setOnClickListener(v -> this.requestSpellPicker.accept(this::applyPickedSpell));
        spellCard.addView(btnPickSpell);

        content.addView(spellCard);

        // 卡片 C: 属性加成
        content.addView(createSectionHeader(context, "属性加成词条"));
        bonusListContainer = new LinearLayout(context);
        bonusListContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(bonusListContainer);

        content.addView(createLabel(context, "添加加成:"));
        btnSelectAttr = new Button(context);
        btnSelectAttr.setText("点击选择属性...");
        btnSelectAttr.setOnClickListener(v -> this.requestAttrPicker.accept(this::applyPickedAttr));
        content.addView(btnSelectAttr);

        // Operation 3 选 1 分段按钮
        LinearLayout opRow = new LinearLayout(context);
        opRow.setOrientation(LinearLayout.HORIZONTAL);
        opRow.setPadding(0, dp(4), 0, dp(4));

        btnOpAdd = createOpButton(context, "+数值", AttributeModifier.Operation.ADD_VALUE);
        btnOpMulBase = createOpButton(context, "+基础%", AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        btnOpMulTotal = createOpButton(context, "+总计%", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        opRow.addView(btnOpAdd, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        opRow.addView(btnOpMulBase, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        opRow.addView(btnOpMulTotal, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        content.addView(opRow);
        updateOpButtons();

        LinearLayout valRow = new LinearLayout(context);
        valRow.setOrientation(LinearLayout.HORIZONTAL);
        valRow.setGravity(Gravity.CENTER_VERTICAL);

        etBonusVal = new EditText(context);
        etBonusVal.setText("0.05");
        valRow.addView(etBonusVal, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnAddBonus = new Button(context);
        btnAddBonus.setText("+ 添加");
        btnAddBonus.setOnClickListener(v -> handleAddBonus());
        valRow.addView(btnAddBonus);
        content.addView(valRow);

        scrollView.addView(content);
        addView(scrollView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // ================= 3. 底部固定底栏 (绝对防遮挡) =================
        LinearLayout bottomFixedBar = new LinearLayout(context);
        bottomFixedBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomFixedBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomFixedBar.setPadding(0, dp(8), 0, 0);

        Button btnApply = new Button(context);
        btnApply.setText("确认并更新");
        btnApply.setOnClickListener(v -> applyChanges());
        bottomFixedBar.addView(btnApply, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnDelete = new Button(context);
        btnDelete.setText("删除节点");
        btnDelete.setTextColor(Color.argb(255, 255, 80, 80));
        btnDelete.setOnClickListener(v -> handleDeleteNode());
        bottomFixedBar.addView(btnDelete);

        addView(bottomFixedBar);
    }

    private Button createOpButton(Context context, String text, AttributeModifier.Operation op) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(10);
        btn.setOnClickListener(v -> {
            this.selectedOp = op;
            updateOpButtons();
        });
        return btn;
    }

    private void updateOpButtons() {
        btnOpAdd.setTextColor(selectedOp == AttributeModifier.Operation.ADD_VALUE ? Color.argb(255, 255, 215, 0) : Color.GRAY);
        btnOpMulBase.setTextColor(selectedOp == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ? Color.argb(255, 255, 215, 0) : Color.GRAY);
        btnOpMulTotal.setTextColor(selectedOp == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? Color.argb(255, 255, 215, 0) : Color.GRAY);
    }

    private TextView createSectionHeader(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText("▪ " + text);
        tv.setTextSize(12);
        tv.setTextColor(Color.argb(255, 255, 215, 0));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView createLabel(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(Color.argb(200, 180, 180, 180));
        return tv;
    }

    public void bind(SkillNode node) {
        this.boundNode = node;
        if (node == null) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);

        etId.setText(node.getId());
        etMaxRank.setText(String.valueOf(node.getMaxRank()));
        tvParents.setText(node.getParents().isEmpty() ? "（无，根节点）" : String.join(", ", node.getParents()));

        // 法术信息刷新
        if (node.getSpell() != null) {
            tvSpellName.setText(node.getSpell().getDisplayName(null).getString());
            tvSpellDesc.setText(node.getSpell().getSpellId());
        } else {
            tvSpellName.setText("未绑定法术");
            tvSpellDesc.setText("点击右侧按钮选择");
        }
        spellIconPreview.invalidate();

        refreshBonuses();
    }

    private void refreshBonuses() {
        bonusListContainer.removeAllViews();
        if (boundNode == null) return;

        for (int i = 0; i < boundNode.getBonuses().size(); i++) {
            final int index = i;
            SkillNode.AttributeBonus b = boundNode.getBonuses().get(i);

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(2), dp(4), dp(2));

            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(b.attributeId());
            String attrName = attr != null ? Component.translatable(attr.getDescriptionId()).getString() : b.attributeId().getPath();

            String opStr = switch (b.operation()) {
                case ADD_VALUE -> "+" + b.amountPerRank();
                case ADD_MULTIPLIED_BASE -> "+" + (b.amountPerRank() * 100.0) + "% (基础)";
                case ADD_MULTIPLIED_TOTAL -> "+" + (b.amountPerRank() * 100.0) + "% (总计)";
            };

            TextView info = new TextView(getContext());
            info.setText(attrName + " " + opStr);
            info.setTextSize(11);
            info.setTextColor(Color.WHITE);
            row.addView(info, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Button btnDel = new Button(getContext());
            btnDel.setText("✕");
            btnDel.setTextColor(Color.argb(255, 255, 100, 100));
            btnDel.setOnClickListener(v -> {
                historyManager.pushSnapshot();
                boundNode.getBonuses().remove(index);
                refreshBonuses();
                onDataChanged.run();
            });
            row.addView(btnDel);

            bonusListContainer.addView(row);
        }
    }

    private void applyPickedSpell(AbstractSpell spell) {
        if (boundNode == null) return;
        historyManager.pushSnapshot();
        boundNode.setSpell(spell);
        bind(boundNode);
        onDataChanged.run();
    }

    private void applyPickedAttr(ResourceLocation attrId) {
        this.currentSelectedAttr = attrId;
        Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attrId);
        String name = attr != null ? Component.translatable(attr.getDescriptionId()).getString() : attrId.getPath();
        btnSelectAttr.setText(name);
    }

    private void handleAddBonus() {
        if (boundNode == null || currentSelectedAttr == null) return;
        double val = 0.05;
        try {
            val = Double.parseDouble(etBonusVal.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        historyManager.pushSnapshot();
        boundNode.addBonus(currentSelectedAttr.toString(), val, selectedOp);
        refreshBonuses();
        onDataChanged.run();
    }

    private void applyChanges() {
        if (boundNode == null) return;
        historyManager.pushSnapshot();

        String newId = etId.getText().toString().trim();
        if (!newId.isEmpty() && !newId.equals(boundNode.getId())) {
            SkillRegistry.remove(boundNode.getId());
            boundNode.setId(newId);
            SkillRegistry.register(boundNode);
        }

        try {
            boundNode.setMaxRank(Integer.parseInt(etMaxRank.getText().toString().trim()));
        } catch (NumberFormatException ignored) {}

        bind(boundNode);
        onDataChanged.run();
    }

    private void handleDeleteNode() {
        if (boundNode == null) return;
        historyManager.pushSnapshot(); // 记录快照，支持 Ctrl+Z 瞬间撤销找回！

        SkillRegistry.remove(boundNode.getId());
        for (SkillNode node : SkillRegistry.NODES.values()) {
            node.getParents().remove(boundNode.getId());
        }
        bind(null);
        onDataChanged.run();
    }

}