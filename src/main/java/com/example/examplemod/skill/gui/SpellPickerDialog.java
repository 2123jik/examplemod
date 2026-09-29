package com.example.examplemod.skill.gui;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.ImageStore;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpellPickerDialog extends FrameLayout {
    private AbstractSpell currentHoveredSpell = null;
    private final LinearLayout gridContainer;
    private final TextView tvHoverSpellTitle;
    private final TextView tvHoverSpellDesc;
    private final Consumer<AbstractSpell> onSelected;
    private final Runnable onClose;
    private String filterText = "";

    public SpellPickerDialog(Context context, Consumer<AbstractSpell> onSelected, Runnable onClose) {
        super(context);
        this.onSelected = onSelected;
        this.onClose = onClose;

        // 半透明黑色遮罩背景
        setClickable(true); // 拦截向下点击

        // 主体卡片
        LinearLayout dialogCard = new LinearLayout(context);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        ShapeDrawable cardBg = new ShapeDrawable();
        cardBg.setColor(Color.argb(250, 20, 24, 30));
        cardBg.setCornerRadius(dp(8));
        cardBg.setStroke(dp(1.5f), Color.argb(200, 255, 215, 0));
        dialogCard.setBackground(cardBg);

        LayoutParams cardLp = new LayoutParams(dp(540), dp(440));
        cardLp.gravity = Gravity.CENTER;
        addView(dialogCard, cardLp);

        // 顶部操作栏
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("选择铁魔法术 (Select Spell)");
        tvTitle.setTextSize(15);
        tvTitle.setTextColor(Color.argb(255, 255, 215, 0));
        header.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnClearSpell = new Button(context);
        btnClearSpell.setText("清除绑定");
        btnClearSpell.setTextColor(Color.argb(255, 255, 120, 120));
        btnClearSpell.setOnClickListener(v -> {
            onSelected.accept(null);
            onClose.run();
        });
        header.addView(btnClearSpell);

        Button btnClose = new Button(context);
        btnClose.setText("✕");
        btnClose.setOnClickListener(v -> onClose.run());
        header.addView(btnClose);
        dialogCard.addView(header);

        // 搜索输入框
        EditText etSearch = new EditText(context);
        etSearch.setHint("搜索法术名称或 ID...");
        etSearch.addTextChangedListener(new SimpleTextWatcher(s -> {
            filterText = s.toString().trim().toLowerCase();
            refreshGrid();
        }));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.topMargin = dp(8);
        searchLp.bottomMargin = dp(8);
        dialogCard.addView(etSearch, searchLp);

        // 图标网格滚动区
        ScrollView scrollView = new ScrollView(context);
        gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(gridContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        dialogCard.addView(scrollView, scrollLp);

        // 底部悬浮信息详情卡片
        LinearLayout previewCard = new LinearLayout(context);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(dp(8), dp(6), dp(8), dp(6));
        ShapeDrawable pBg = new ShapeDrawable();
        pBg.setColor(Color.argb(180, 12, 14, 18));
        pBg.setCornerRadius(dp(4));
        previewCard.setBackground(pBg);

        tvHoverSpellTitle = new TextView(context);
        tvHoverSpellTitle.setTextSize(12);
        tvHoverSpellTitle.setTextColor(Color.argb(255, 255, 215, 0));
        tvHoverSpellTitle.setText("鼠标悬停在上方图标可查看法术详情");
        previewCard.addView(tvHoverSpellTitle);

        tvHoverSpellDesc = new TextView(context);
        tvHoverSpellDesc.setTextSize(10);
        tvHoverSpellDesc.setTextColor(Color.LTGRAY);
        previewCard.addView(tvHoverSpellDesc);

        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(65));
        prevLp.topMargin = dp(8);
        dialogCard.addView(previewCard, prevLp);

        refreshGrid();
    }
    private void handleSpellHover(AbstractSpell spell, boolean enter) {
        if (enter) {
            currentHoveredSpell = spell;
            updatePreview(spell);
        } else {
            // 关键：只有当前悬停的确实是离开的这个法术时，才清空显示
            // 如果鼠标已经移到了下一个法术上（currentHoveredSpell 已经改变），则忽略本次清空
            if (currentHoveredSpell == spell) {
                currentHoveredSpell = null;
                updatePreview(null);
            }
        }
    }
    private void refreshGrid() {
        gridContainer.removeAllViews();
        List<AbstractSpell> spells = new ArrayList<>();
        for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
            if (spell == SpellRegistry.none()) continue;
            String name = spell.getDisplayName(null).getString().toLowerCase();
            String id = spell.getSpellId().toLowerCase();
            if (filterText.isEmpty() || name.contains(filterText) || id.contains(filterText)) {
                spells.add(spell);
            }
        }

        int columns = 10;
        LinearLayout row = null;
        for (int i = 0; i < spells.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                gridContainer.addView(row);
            }
            AbstractSpell spell = spells.get(i);
            SpellIconItemView itemView = new SpellIconItemView(getContext(), spell, s -> {
                onSelected.accept(s);
                onClose.run();
            }, this::handleSpellHover);
            row.addView(itemView);
        }
    }

    private void updatePreview(@Nullable AbstractSpell spell) {
        if (spell == null) {
            tvHoverSpellTitle.setText("鼠标悬停在上方图标可查看法术详情");
            tvHoverSpellDesc.setText("");
            return;
        }
        tvHoverSpellTitle.setText(spell.getDisplayName(null).getString() + " (" + spell.getSpellId() + ")");
        Component guideComp = Component.translatable(spell.getComponentId() + ".guide").withStyle(ChatFormatting.GREEN);
        String desc = guideComp.getString();
        tvHoverSpellDesc.setText(desc);
    }

    private static class SpellIconItemView extends View {
        private final AbstractSpell spell;
        private final Consumer<AbstractSpell> onClick;
        private final java.util.function.BiConsumer<AbstractSpell, Boolean> onHover; // 修改类型
        private final Paint borderPaint = new Paint();
        private final Paint bgPaint = new Paint();
        private final RectF dst = new RectF();
        private boolean hovered = false;

        public SpellIconItemView(Context context, AbstractSpell spell, Consumer<AbstractSpell> onClick,
                                 java.util.function.BiConsumer<AbstractSpell, Boolean> onHover) {
            super(context);
            this.spell = spell;
            this.onClick = onClick;
            this.onHover = onHover;

            int size = (int) (44 * context.getResources().getDisplayMetrics().density + 0.5f);
            setLayoutParams(new LinearLayout.LayoutParams(size, size));

            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Paint.STROKE);
            borderPaint.setStrokeWidth(2f);

            bgPaint.setAntiAlias(true);
            bgPaint.setStyle(Paint.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();

            bgPaint.setColor(hovered ? Color.argb(160, 255, 215, 0) : Color.argb(120, 28, 32, 40));
            canvas.drawRoundRect(2f, 2f, w - 2f, h - 2f, 6f, 6, bgPaint);

            borderPaint.setColor(hovered ? Color.argb(255, 255, 230, 100) : Color.argb(100, 150, 150, 150));
            canvas.drawRoundRect(2f, 2f, w - 2f, h - 2f, 6f, 6, borderPaint);

            Image iconImg = ImageStore.getInstance().getOrCreate(spell.getSpellIconResource());
            if (iconImg != null) {
                dst.set(6f, 6f, w - 6f, h - 6f);
                canvas.drawImage(iconImg, null, dst, null);
            }
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
                hovered = true;
                onHover.accept(spell, true); // true 代表进入
                invalidate();
                return true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
                hovered = false;
                onHover.accept(spell, false); // false 代表移出当前法术
                invalidate();
                return true;
            }
            return super.onGenericMotionEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                onClick.accept(spell);
                return true;
            }
            return true;
        }
    }
}