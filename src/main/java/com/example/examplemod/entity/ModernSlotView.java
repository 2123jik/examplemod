package com.example.examplemod.entity;

import com.example.examplemod.util.EquipmentIndex;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.widget.FrameLayout;
import net.minecraft.world.item.ItemStack;

public final class ModernSlotView extends FrameLayout {

    @FunctionalInterface
    public interface OnHoverListener {
        void onHover(ModernSlotView view, EquipmentIndex.Row rowData);
    }

    private final EquipmentIndex.Row rowData;
    private final ItemStack stack;
    private boolean isSelected;
    private boolean isHovered;

    private final Paint bgPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint warningPaint = new Paint();
    private final RectF bounds = new RectF();

    private OnHoverListener hoverListener;
    private Runnable unhoverListener;

    public ModernSlotView(Context context, EquipmentIndex.Row data, ItemStack stack) {
        super(context);
        this.rowData = data;
        this.stack = stack;

        setWillNotDraw(false);
        setClickable(true);

        int pad = dp(2);
        setPadding(pad, pad, pad, pad);

        if (stack != null && !stack.isEmpty()) {
            EquipmentItemIcon icon = new EquipmentItemIcon(context, stack);
            addView(icon, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        borderPaint.setStyle(Paint.Style.STROKE);
        warningPaint.setColor(0xFFFF4444);
        warningPaint.setStyle(Paint.Style.FILL);
    }

    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            invalidate();
        }
    }

    public void setOnHoverItemListener(OnHoverListener onHover, Runnable onUnhover) {
        this.hoverListener = onHover;
        this.unhoverListener = onUnhover;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE) {
            if (!isHovered) {
                isHovered = true;
                invalidate();
            }
            if (hoverListener != null) {
                hoverListener.onHover(this, rowData);
            }
            return true;
        } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
            isHovered = false;
            invalidate();
            if (unhoverListener != null) {
                unhoverListener.run();
            }
            return true;
        }
        return super.onHoverEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bounds.set(0, 0, getWidth(), getHeight());
        float r = dp(4);

        // 1. 槽位底板
        bgPaint.setColor(isHovered ? 0x4DFFFFFF : 0x26000000);
        canvas.drawRoundRect(bounds, r, bgPaint);

        // 2. 神化品质与选中外边框
        int rarityColor = rowData.rarityColor();
        if (isSelected) {
            borderPaint.setColor(0xFFFFFFFF);
            borderPaint.setStrokeWidth(dp(2));
        } else if (isHovered) {
            borderPaint.setColor(rarityColor);
            borderPaint.setStrokeWidth(dp(1.5f));
        } else {
            borderPaint.setColor((rarityColor & 0x00FFFFFF) | 0x66000000);
            borderPaint.setStrokeWidth(dp(1));
        }

        float inset = borderPaint.getStrokeWidth() / 2f;
        bounds.inset(inset, inset);
        canvas.drawRoundRect(bounds, r, borderPaint);

        // 3. 耐久危急红点指示（<20% 耐久时在右上角微标提示）
        if (rowData.hasDurability() && rowData.durabilityPercent() < 20.0) {
            float dotRadius = dp(2.5f);
            canvas.drawCircle(getWidth() - dp(5), dp(5), dotRadius, warningPaint);
        }
    }
}