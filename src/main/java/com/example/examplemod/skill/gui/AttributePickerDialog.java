package com.example.examplemod.skill.gui;

import com.example.examplemod.util.AttributeUtils;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AttributePickerDialog extends FrameLayout {

    private final LinearLayout listContainer;
    private final Consumer<ResourceLocation> onSelected;
    private final Runnable onClose;
    private String filter = "";

    public AttributePickerDialog(Context context, Consumer<ResourceLocation> onSelected, Runnable onClose) {
        super(context);
        this.onSelected = onSelected;
        this.onClose = onClose;

        setClickable(true);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        ShapeDrawable cardBg = new ShapeDrawable();
        cardBg.setColor(Color.argb(250, 20, 24, 30));
        cardBg.setCornerRadius(dp(8));
        cardBg.setStroke(dp(1.5f), Color.argb(200, 255, 215, 0));
        card.setBackground(cardBg);

        LayoutParams cardLp = new LayoutParams(dp(420), dp(420));
        cardLp.gravity = Gravity.CENTER;
        addView(card, cardLp);

        // 标题与关闭
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("选择属性 (Select Attribute)");
        title.setTextSize(15);
        title.setTextColor(Color.argb(255, 255, 215, 0));
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnClose = new Button(context);
        btnClose.setText("✕");
        btnClose.setOnClickListener(v -> onClose.run());
        header.addView(btnClose);
        card.addView(header);

        // 搜索框
        EditText etSearch = new EditText(context);
        etSearch.setHint("输入名称或 ID 过滤...");
        etSearch.addTextChangedListener(new SimpleTextWatcher(s -> {
            filter = s.toString().trim().toLowerCase();
            refreshList();
        }));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.topMargin = dp(8);
        searchLp.bottomMargin = dp(8);
        card.addView(etSearch, searchLp);

        ScrollView scrollView = new ScrollView(context);
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        refreshList();
    }

    private void refreshList() {
        listContainer.removeAllViews();
        List<ResourceLocation> matched = new ArrayList<>();

        for (var entry : BuiltInRegistries.ATTRIBUTE.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Attribute attr = entry.getValue();
            String trans = Component.translatable(attr.getDescriptionId()).getString().toLowerCase();

            if (filter.isEmpty() || trans.contains(filter) || id.toString().toLowerCase().contains(filter)) {
                matched.add(id);
            }
        }

        for (ResourceLocation id : matched) {
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(id);
            if (attr == null) continue;

            String transName = Component.translatable(attr.getDescriptionId()).getString();
            // 获取描述（无描述则返回 null）
            String description = AttributeUtils.getAttributeDescription(id, attr);

            LinearLayout item = new LinearLayout(getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(8), dp(6), dp(8), dp(6));
            item.setClickable(true);

            // 1. 属性名
            TextView tvName = new TextView(getContext());
            tvName.setText(transName);
            tvName.setTextSize(13);
            tvName.setTextColor(Color.WHITE);
            item.addView(tvName);

            // 2. 属性描述（只有存在描述时才添加并显示）
            if (description != null && !description.isEmpty()) {
                TextView tvDesc = new TextView(getContext());
                tvDesc.setText(description);
                tvDesc.setTextSize(11);
                tvDesc.setTextColor(Color.argb(200, 160, 210, 255)); // 浅蓝/灰蓝提示色
                item.addView(tvDesc);
            }

            // 3. 属性命名空间 ID
            TextView tvId = new TextView(getContext());
            tvId.setText(id.toString());
            tvId.setTextSize(10);
            tvId.setTextColor(Color.GRAY);
            item.addView(tvId);

            item.setOnClickListener(v -> {
                onSelected.accept(id);
                onClose.run();
            });

            listContainer.addView(item);
        }
    }

}