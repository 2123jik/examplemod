package com.example.examplemod.skill.gui;

import com.example.examplemod.skill.*;
import com.example.examplemod.skill.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

import static com.example.examplemod.registry.ModRegistry.SKILL_DATA;

public class SkillTreeFragment extends Fragment {

    private final SkillHistoryManager historyManager = new SkillHistoryManager();

    private TextView tvPoints;
    private SkillConstellationView constellationView;
    private TooltipCard tooltipCard;
    private SkillNodeInspector inspector;
    private FrameLayout rootContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        Context context = requireContext();

        rootContainer = new FrameLayout(context) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                // 监听 Ctrl+Z 撤销与 Ctrl+Y 重做
                if (event.getAction() == KeyEvent.ACTION_DOWN && constellationView.isEditMode()) {
                    boolean ctrl = event.isCtrlPressed();
                    if (ctrl && event.getKeyCode() == KeyEvent.KEY_Z) {
                        if (event.isShiftPressed()) {
                            historyManager.redo();
                        } else {
                            historyManager.undo();
                        }
                        refreshUI();
                        return true;
                    } else if (ctrl && event.getKeyCode() == KeyEvent.KEY_Y) {
                        historyManager.redo();
                        refreshUI();
                        return true;
                    }
                }
                return super.dispatchKeyEvent(event);
            }
        };
        rootContainer.setFocusable(true);
        rootContainer.setFocusableInTouchMode(true);
        rootContainer.requestFocus();
        rootContainer.setBackground(new ColorDrawable(Color.argb(245, 10, 10, 15)));

        // 1. 星图画布
        constellationView = new SkillConstellationView(context, this::updatePointsText, historyManager);
        rootContainer.addView(constellationView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 2. 悬浮卡片
        tooltipCard = new TooltipCard(context);
        tooltipCard.setVisibility(View.GONE);
        rootContainer.addView(tooltipCard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        constellationView.setOnHoverNodeListener(this::onNodeHover);

        // 3. 检查器面板
        inspector = new SkillNodeInspector(
                context,
                historyManager,
                () -> constellationView.invalidate(),
                this::openSpellPicker,
                this::openAttrPicker
        );
        inspector.setVisibility(View.GONE);
        FrameLayout.LayoutParams inspLp = new FrameLayout.LayoutParams(
                dp(300, context),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        inspLp.gravity = Gravity.END;
        inspLp.setMargins(0, dp(55, context), dp(16, context), dp(16, context));
        rootContainer.addView(inspector, inspLp);

        constellationView.setOnNodeSelectedListener(node -> {
            if (constellationView.isEditMode()) {
                inspector.bind(node);
            }
        });

        // 4. 顶部操作栏
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16, context), dp(10, context), dp(16, context), dp(10, context));
        topBar.setBackground(new ColorDrawable(Color.argb(150, 0, 0, 0)));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(Component.translatable("gui.examplemod.skill_tree.title").getString());
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(Color.WHITE);
        topBar.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        tvPoints = new TextView(context);
        tvPoints.setTextSize(14);
        tvPoints.setTextColor(Color.argb(255, 255, 215, 0));
        topBar.addView(tvPoints);

        Button btnToggleEdit = new Button(context);
        btnToggleEdit.setText("编辑模式: 关");
        btnToggleEdit.setOnClickListener(v -> {
            boolean mode = !constellationView.isEditMode();
            constellationView.setEditMode(mode);
            btnToggleEdit.setText(mode ? "编辑模式: 开" : "编辑模式: 关");
            btnToggleEdit.setTextColor(mode ? Color.GREEN : Color.WHITE);
            if (!mode) inspector.setVisibility(View.GONE);
        });
        topBar.addView(btnToggleEdit);

        Button btnAddNode = new Button(context);
        btnAddNode.setText("+ 新建节点");
        btnAddNode.setOnClickListener(v -> {
            historyManager.pushSnapshot();
            String newId = "node_" + (System.currentTimeMillis() % 10000);
            SkillNode node = new SkillNode(newId, 1);
            node.setPosition(constellationView.getViewportCenterX(), constellationView.getViewportCenterY());
            SkillRegistry.register(node);
            constellationView.setSelectedNode(node);
            inspector.bind(node);
            constellationView.invalidate();
        });
        topBar.addView(btnAddNode);

        Button btnSave = new Button(context);
        btnSave.setText("保存 JSON");
        btnSave.setTextColor(Color.argb(255, 255, 215, 0));
        btnSave.setOnClickListener(v -> {
            SkillNodeData.saveAll(SkillRegistry.NODES.values());
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§a[技能星图] 已保存至 config/examplemod/skills.json！"), false
                );
            }
        });
        topBar.addView(btnSave);

        FrameLayout.LayoutParams topBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topBarParams.gravity = Gravity.TOP;
        rootContainer.addView(topBar, topBarParams);

        updatePointsText();
        return rootContainer;
    }

    private void openSpellPicker(java.util.function.Consumer<io.redspace.ironsspellbooks.api.spells.AbstractSpell> callback) {
        final SpellPickerDialog[] dialogHolder = new SpellPickerDialog[1];
        dialogHolder[0] = new SpellPickerDialog(requireContext(), callback, () -> rootContainer.removeView(dialogHolder[0]));
        rootContainer.addView(dialogHolder[0], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void openAttrPicker(java.util.function.Consumer<net.minecraft.resources.ResourceLocation> callback) {
        final AttributePickerDialog[] dialogHolder = new AttributePickerDialog[1];
        dialogHolder[0] = new AttributePickerDialog(requireContext(), callback, () -> rootContainer.removeView(dialogHolder[0]));
        rootContainer.addView(dialogHolder[0], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void refreshUI() {
        constellationView.invalidate();
        if (inspector.getVisibility() == View.VISIBLE) {
            inspector.bind(constellationView.getSelectedNode());
        }
    }

    private void updatePointsText() {
        var player = Minecraft.getInstance().player;
        if (player == null || tvPoints == null) return;
        PlayerSkillData data = player.getData(SKILL_DATA.get());
        tvPoints.setText(Component.translatable("gui.examplemod.skill_tree.points", data.getPoints()).getString());
    }

    private SkillNode currentBoundNode = null;

    private void onNodeHover(@Nullable SkillNode node, @Nullable MotionEvent event) {
        if (constellationView.isEditMode() || node == null || event == null) {
            tooltipCard.setVisibility(View.GONE);
            currentBoundNode = null;
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        if (currentBoundNode != node) {
            currentBoundNode = node;
            PlayerSkillData data = player.getData(SKILL_DATA.get());
            tooltipCard.bindData(node, data, constellationView.areParentsMet(node, data));

            int unconstrained = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            tooltipCard.measure(unconstrained, unconstrained);
        }

        if (tooltipCard.getVisibility() != View.VISIBLE) {
            tooltipCard.setVisibility(View.VISIBLE);
        }

        float cardW = tooltipCard.getMeasuredWidth();
        float cardH = tooltipCard.getMeasuredHeight();
        float mouseX = event.getX();
        float mouseY = event.getY();
        float spacing = 16f;

        float targetX = mouseX + spacing;
        float targetY = mouseY + spacing;

        if (targetX + cardW > rootContainer.getWidth() - spacing) targetX = mouseX - cardW - spacing;
        if (targetY + cardH > rootContainer.getHeight() - spacing) targetY = mouseY - cardH - spacing;

        targetX = Math.max(spacing, targetX);
        targetY = Math.max(spacing, targetY);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tooltipCard.getLayoutParams();
        if (lp != null) {
            lp.leftMargin = (int) targetX;
            lp.topMargin = (int) targetY;
            tooltipCard.setLayoutParams(lp);
        }
    }

    private int dp(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class TooltipCard extends LinearLayout {
        private final TextView tvTitle;
        private final TextView tvBonus;
        private final TextView tvStatus;

        public TooltipCard(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            setPadding(dp(10), dp(8), dp(10), dp(8));

            ShapeDrawable bg = new ShapeDrawable();
            bg.setColor(Color.argb(240, 16, 16, 20));
            bg.setCornerRadius(dp(4));
            bg.setStroke(dp(1.0f), Color.argb(160, 255, 215, 0));
            setBackground(bg);

            tvTitle = new TextView(context);
            tvTitle.setTextSize(13);
            tvTitle.setTextColor(Color.argb(255, 255, 215, 0));
            addView(tvTitle);

            tvBonus = new TextView(context);
            tvBonus.setTextSize(11);
            tvBonus.setTextColor(Color.argb(255, 120, 210, 255));
            addView(tvBonus);

            tvStatus = new TextView(context);
            tvStatus.setTextSize(10);
            addView(tvStatus);
        }

        public void bindData(SkillNode node, PlayerSkillData data, boolean parentsMet) {
            int currentRank = data.getRank(node.getId());
            boolean isMax = currentRank >= node.getMaxRank();

            Component localizedTitle = node.getLocalizedTitle();
            if (localizedTitle != null) {
                tvTitle.setText(localizedTitle.getString() + " [" + currentRank + "/" + node.getMaxRank() + "]");
                tvTitle.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setVisibility(View.GONE);
            }

            if (!node.getBonuses().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < node.getBonuses().size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(node.getBonuses().get(i).formatBonus(currentRank).getString());
                }
                tvBonus.setText(sb.toString());
                tvBonus.setVisibility(View.VISIBLE);
            } else {
                tvBonus.setVisibility(View.GONE);
            }

            if (!parentsMet) {
                tvStatus.setText(Component.translatable("gui.examplemod.skill_tree.requires_parent").getString());
                tvStatus.setTextColor(Color.argb(255, 255, 90, 90));
                tvStatus.setVisibility(View.VISIBLE);
            } else if (isMax) {
                tvStatus.setText(Component.translatable("gui.examplemod.skill_tree.max_rank").getString());
                tvStatus.setTextColor(Color.argb(255, 100, 220, 100));
                tvStatus.setVisibility(View.VISIBLE);
            } else if (data.getPoints() > 0) {
                tvStatus.setText(Component.translatable("gui.examplemod.skill_tree.upgrade").getString());
                tvStatus.setTextColor(Color.argb(255, 255, 215, 0));
                tvStatus.setVisibility(View.VISIBLE);
            } else {
                tvStatus.setVisibility(View.GONE);
            }
        }

    }
}