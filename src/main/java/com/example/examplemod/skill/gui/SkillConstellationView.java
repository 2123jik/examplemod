package com.example.examplemod.skill.gui;

import com.example.examplemod.registry.ModPayloads;
import com.example.examplemod.skill.*;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.text.Font;
import icyllis.modernui.graphics.text.FontFamily;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.FontPaint;
import icyllis.modernui.mc.ImageStore;
import icyllis.modernui.mc.ModernUIClient;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.example.examplemod.registry.ModRegistry.SKILL_DATA;

public class SkillConstellationView extends View {

    private static final float NODE_RADIUS = 24f;
    private static final float HIT_PADDING = 8f;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 3.0f;

    private float offsetX = 0;
    private float offsetY = 0;
    private float scale = 1.0f;

    private float downX, downY;
    private float lastX, lastY;
    private boolean isDragging;
    private final int touchSlop;

    private SkillNode hoveredNode = null;
    private BiConsumer<SkillNode, MotionEvent> hoverListener;
    private final Runnable onPointsChanged;
    @Nullable
    private final SkillHistoryManager historyManager;

    // === 编辑器状态 ===
    private boolean editMode = false;
    private SkillNode selectedNode = null;
    private SkillNode movingNode = null;
    private SkillNode linkingSource = null;
    private float linkCurrentX, linkCurrentY;
    private Consumer<SkillNode> onNodeSelectedListener;

    // 画笔
    private final Paint lineLockedPaint = new Paint();
    private final Paint lineAvailablePaint = new Paint();
    private final Paint lineActivePaint = new Paint();
    private final Paint lineLinkingPaint = new Paint();
    private final Paint nodeBgPaint = new Paint();
    private final Paint nodeBorderPaint = new Paint();
    private final Paint selectedBorderPaint = new Paint();
    private final Paint textPaint = new Paint();

    private final FontPaint fontPaintPlus = new FontPaint();
    private final FontPaint fontPaintRank = new FontPaint();
    private final FontMetricsInt plusMetrics = new FontMetricsInt();
    private final FontMetricsInt rankMetrics = new FontMetricsInt();
    private final RectF dstRect = new RectF();

    private final RectF bgDstRect = new RectF();
    private final Paint bgOverlayPaint = new Paint();
    private Font regularFont;
    private Font boldFont;

    public SkillConstellationView(Context context, Runnable onPointsChanged, @Nullable SkillHistoryManager historyManager) {
        super(context);
        this.onPointsChanged = onPointsChanged;
        this.historyManager = historyManager;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        initPaints();
    }

    public SkillConstellationView(Context context, Runnable onPointsChanged) {
        this(context, onPointsChanged, null);
    }

    private void initPaints() {
        lineLockedPaint.setAntiAlias(true);
        lineLockedPaint.setColor(Color.argb(60, 255, 255, 255));
        lineLockedPaint.setStrokeWidth(2.0f);

        lineAvailablePaint.setAntiAlias(true);
        lineAvailablePaint.setColor(Color.argb(180, 100, 200, 255));
        lineAvailablePaint.setStrokeWidth(2.5f);

        lineActivePaint.setAntiAlias(true);
        lineActivePaint.setColor(Color.argb(220, 255, 215, 0));
        lineActivePaint.setStrokeWidth(3.5f);

        lineLinkingPaint.setAntiAlias(true);
        lineLinkingPaint.setColor(Color.argb(240, 0, 255, 255));
        lineLinkingPaint.setStrokeWidth(3.0f);

        nodeBgPaint.setAntiAlias(true);
        nodeBgPaint.setStyle(Paint.FILL);

        nodeBorderPaint.setAntiAlias(true);
        nodeBorderPaint.setStyle(Paint.STROKE);

        selectedBorderPaint.setAntiAlias(true);
        selectedBorderPaint.setStyle(Paint.STROKE);
        selectedBorderPaint.setColor(Color.argb(255, 0, 255, 200));
        selectedBorderPaint.setStrokeWidth(3.5f);

        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.FILL);
        // 背景暗色遮罩层（如果你的背景图片较亮，此遮罩能保证节点与连线清晰可见）
        // 0 = 全透，120 = 适度暗化，255 = 全黑
        bgOverlayPaint.setColor(Color.argb(120, 10, 12, 16));
        bgOverlayPaint.setStyle(Paint.FILL);

        FontFamily family = ModernUIClient.getInstance().getFirstFontFamily();
        if (family == null) family = FontFamily.SANS_SERIF;
        this.regularFont = family.getClosestMatch(FontPaint.NORMAL);
        this.boldFont = family.getClosestMatch(FontPaint.BOLD);

        var typeface = ModernUI.getSelectedTypeface();
        fontPaintPlus.setFont(typeface);
        fontPaintPlus.setFontSize(22f);
        fontPaintPlus.getFontMetricsInt(plusMetrics);

        fontPaintRank.setFont(typeface);
        fontPaintRank.setFontSize(11f);
        fontPaintRank.getFontMetricsInt(rankMetrics);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        this.selectedNode = null;
        this.movingNode = null;
        this.linkingSource = null;
        invalidate();
    }

    public boolean isEditMode() { return editMode; }

    public void setSelectedNode(SkillNode node) {
        this.selectedNode = node;
        invalidate();
    }

    public SkillNode getSelectedNode() { return selectedNode; }

    public void setOnHoverNodeListener(BiConsumer<SkillNode, MotionEvent> listener) {
        this.hoverListener = listener;
    }

    public void setOnNodeSelectedListener(Consumer<SkillNode> listener) {
        this.onNodeSelectedListener = listener;
    }

    public float getViewportCenterX() { return -offsetX; }
    public float getViewportCenterY() { return -offsetY; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        var player = Minecraft.getInstance().player;
        PlayerSkillData data = (player != null) ? player.getData(SKILL_DATA.get()) : null;

        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;

        canvas.save();
        canvas.translate(cx + offsetX, cy + offsetY);
        canvas.scale(scale, scale);

        drawLines(canvas, data);
        drawNodes(canvas, data, cx, cy);

        // 绘制正在拉取的连线
        if (editMode && linkingSource != null) {
            canvas.drawLine(linkingSource.getX(), linkingSource.getY(), linkCurrentX, linkCurrentY, lineLinkingPaint);
        }

        canvas.restore();
    }
//    private void drawBackground(Canvas canvas) {
//        float viewW = getWidth();
//        float viewH = getHeight();
//        if (viewW <= 0 || viewH <= 0) return;
//
//        Image bgImg = ImageStore.getInstance().getOrCreate(BG_TEXTURE);
//        if (bgImg != null) {
//            float imgW = bgImg.getWidth();
//            float imgH = bgImg.getHeight();
//
//            // 基础 Center-Crop 计算：计算图片铺满屏幕且不变形的缩放比
//            float scaleX = viewW / imgW;
//            float scaleY = viewH / imgH;
//            float baseScale = Math.max(scaleX, scaleY);
//
//            // 略微放大 1.25 倍，留出视差移动的安全边缘
//            float bgScale = baseScale * 1.25f;
//            float drawW = imgW * bgScale;
//            float drawH = imgH * bgScale;
//
//            // 视差效果：背景只跟随星图偏移 15%，产生深邃的空间纵深感
//            float parallaxFactor = 0.15f;
//            float bgCenterX = viewW * 0.5f + offsetX * parallaxFactor;
//            float bgCenterY = viewH * 0.5f + offsetY * parallaxFactor;
//
//            bgDstRect.set(
//                    bgCenterX - drawW * 0.5f,
//                    bgCenterY - drawH * 0.5f,
//                    bgCenterX + drawW * 0.5f,
//                    bgCenterY + drawH * 0.5f
//            );
//
//            // 绘制图片
//            canvas.drawImage(bgImg, null, bgDstRect, null);
//
//            // 绘制半透明暗色遮罩，保证上层的金色/高亮技能节点更加立体吸睛
//            canvas.drawRect(0, 0, viewW, viewH, bgOverlayPaint);
//        } else {
//            // 如果图片加载失败/不存在，优雅降级为纯黑深空色
//            canvas.drawColor(Color.argb(255, 10, 10, 15));
//        }
//    }
    private void drawLines(Canvas canvas, PlayerSkillData data) {
        for (SkillNode node : SkillRegistry.NODES.values()) {
            int currentRank = (data != null) ? data.getRank(node.getId()) : 0;
            float x1 = node.getX();
            float y1 = node.getY();

            for (String parentId : node.getParents()) {
                SkillNode parent = SkillRegistry.NODES.get(parentId);
                if (parent == null) continue;

                int parentRank = (data != null) ? data.getRank(parentId) : 0;
                Paint paint;
                if (editMode) {
                    paint = lineAvailablePaint;
                } else if (parentRank > 0 && currentRank > 0) {
                    paint = lineActivePaint;
                } else if (parentRank > 0) {
                    paint = lineAvailablePaint;
                } else {
                    paint = lineLockedPaint;
                }
                canvas.drawLine(parent.getX(), parent.getY(), x1, y1, paint);
            }
        }
    }

    private void drawNodes(Canvas canvas, PlayerSkillData data, float cx, float cy) {
        float viewW = getWidth();
        float viewH = getHeight();
        int points = (data != null) ? data.getPoints() : 0;

        for (SkillNode node : SkillRegistry.NODES.values()) {
            float nx = node.getX();
            float ny = node.getY();

            float screenX = cx + offsetX + nx * scale;
            float screenY = cy + offsetY + ny * scale;
            float cullBound = (NODE_RADIUS * 2.5f) * scale;

            if (screenX + cullBound < 0 || screenX - cullBound > viewW ||
                    screenY + cullBound < 0 || screenY - cullBound > viewH) {
                continue;
            }

            boolean isHovered = (node == hoveredNode);
            float r = isHovered ? (NODE_RADIUS * 1.15f) : NODE_RADIUS;

            int currentRank = (data != null) ? data.getRank(node.getId()) : 0;
            boolean isMax = currentRank >= node.getMaxRank();
            boolean parentsMet = (data == null) || areParentsMet(node, data);
            boolean canUpgrade = parentsMet && !isMax && points > 0;

            // 1. 底盘
            if (editMode) {
                nodeBgPaint.setColor(Color.argb(220, 35, 40, 50));
            } else if (isMax) {
                nodeBgPaint.setColor(Color.argb(230, 30, 75, 35));
            } else if (currentRank > 0) {
                nodeBgPaint.setColor(Color.argb(230, 60, 50, 20));
            } else if (parentsMet) {
                nodeBgPaint.setColor(Color.argb(210, 25, 45, 75));
            } else {
                nodeBgPaint.setColor(Color.argb(180, 22, 22, 26));
            }
            canvas.drawCircle(nx, ny, r, nodeBgPaint);

            // 2. 轮廓边框
            nodeBorderPaint.setStrokeWidth(isHovered ? 3.0f : 2.0f);
            if (isHovered && canUpgrade) {
                nodeBorderPaint.setColor(Color.argb(255, 255, 240, 150));
            } else if (isMax) {
                nodeBorderPaint.setColor(Color.argb(255, 80, 220, 100));
            } else if (canUpgrade) {
                nodeBorderPaint.setColor(Color.argb(255, 255, 215, 0));
            } else if (currentRank > 0) {
                nodeBorderPaint.setColor(Color.argb(200, 200, 180, 70));
            } else {
                nodeBorderPaint.setColor(Color.argb(80, 150, 150, 150));
            }
            canvas.drawCircle(nx, ny, r, nodeBorderPaint);

            // 编辑模式下选中外环
            if (editMode && node == selectedNode) {
                canvas.drawCircle(nx, ny, r + 4f, selectedBorderPaint);
            }

            // 3. 中心图标或 '+' 号
            if (node.getIcon() != null) {
                ResourceLocation icon = node.getIcon();

// 如果没有 .png 后缀，则自动补上
                if (!icon.getPath().endsWith(".png")) {
                    icon = icon.withSuffix(".png");
                }

                Image img = ImageStore.getInstance().getOrCreate(icon);                if (img != null) {
                    float iconSize = r * 1.3f;
                    dstRect.set(nx - iconSize * 0.5f, ny - iconSize * 0.5f, nx + iconSize * 0.5f, ny + iconSize * 0.5f);
                    canvas.drawImage(img, null, dstRect, null);
                }
            } else {
                textPaint.setColor(canUpgrade ? Color.argb(255, 255, 215, 0) : (parentsMet ? Color.WHITE : Color.GRAY));
                float plusBaseY = ny - (plusMetrics.ascent + plusMetrics.descent) * 0.5f;
                canvas.drawSimpleText("+", boldFont, nx - 6f, plusBaseY, textPaint, fontPaintPlus);
            }

            // 4. 等级与名称标注
            textPaint.setColor(isMax ? Color.GREEN : (currentRank > 0 ? Color.argb(255, 255, 215, 0) : Color.LTGRAY));
            String rankStr = editMode ? node.getId() : (currentRank + "/" + node.getMaxRank());
            float rankBaseY = ny + r + 12f;
            float textOffset = (rankStr.length() * 3.0f);
            canvas.drawSimpleText(rankStr, regularFont, nx - textOffset, rankBaseY, textPaint, fontPaintRank);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            if (!isDragging && movingNode == null && linkingSource == null) {
                updateHoverNode(x, y, event);
            }
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (vScroll != 0) {
                float oldScale = scale;
                float zoomFactor = vScroll > 0 ? 1.15f : (1f / 1.15f);
                scale = Math.clamp(scale * zoomFactor, MIN_SCALE, MAX_SCALE);

                float focusX = x - getWidth() * 0.5f;
                float focusY = y - getHeight() * 0.5f;
                offsetX = (offsetX - focusX) * (scale / oldScale) + focusX;
                offsetY = (offsetY - focusY) * (scale / oldScale) + focusY;

                updateHoverNode(x, y, event);
                invalidate();
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float wx = toWorldCoordX(x);
        float wy = toWorldCoordY(y);

        if (editMode) {
            boolean isRightClick = (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {
                    downX = x; downY = y;
                    lastX = x; lastY = y;
                    isDragging = false;

                    SkillNode clicked = findNodeAt(wx, wy);
                    if (isRightClick) {
                        if (clicked != null) {
                            linkingSource = clicked;
                            linkCurrentX = wx;
                            linkCurrentY = wy;
                            invalidate();
                            return true;
                        }
                    } else {
                        if (clicked != null) {
                            // 拖动前记录快照（支持拖动位置撤销）
                            if (historyManager != null) {
                                historyManager.pushSnapshot();
                            }
                            selectedNode = clicked;
                            movingNode = clicked;
                            if (onNodeSelectedListener != null) {
                                onNodeSelectedListener.accept(clicked);
                            }
                            invalidate();
                            return true;
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (linkingSource != null) {
                        linkCurrentX = wx;
                        linkCurrentY = wy;
                        invalidate();
                        return true;
                    }
                    if (movingNode != null) {
                        movingNode.setX(wx);
                        movingNode.setY(wy);
                        invalidate();
                        return true;
                    }

                    float dx = x - downX;
                    float dy = y - downY;
                    if (!isDragging && (dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                        isDragging = true;
                    }
                    if (isDragging) {
                        offsetX += (x - lastX);
                        offsetY += (y - lastY);
                        lastX = x;
                        lastY = y;
                        invalidate();
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP -> {
                    if (linkingSource != null) {
                        SkillNode target = findNodeAt(wx, wy);
                        if (target != null && target != linkingSource) {
                            toggleParent(target, linkingSource);
                        }
                        linkingSource = null;
                        invalidate();
                        return true;
                    }
                    if (movingNode != null) {
                        movingNode = null;
                        return true;
                    }
                    if (!isDragging) {
                        SkillNode clicked = findNodeAt(wx, wy);
                        if (clicked == null) {
                            selectedNode = null;
                            if (onNodeSelectedListener != null) onNodeSelectedListener.accept(null);
                            invalidate();
                        }
                    }
                    isDragging = false;
                    return true;
                }
            }
            return true;
        }

        // 常规玩家视图模式
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                downX = x; downY = y;
                lastX = x; lastY = y;
                isDragging = false;
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = x - downX;
                float dy = y - downY;
                if (!isDragging && (dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                    isDragging = true;
                    clearHoverNode();
                }
                if (isDragging) {
                    offsetX += (x - lastX);
                    offsetY += (y - lastY);
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    handleClick(x, y);
                }
                isDragging = false;
                updateHoverNode(x, y, event);
                return true;
            }
            case MotionEvent.ACTION_CANCEL -> {
                isDragging = false;
                clearHoverNode();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void toggleParent(SkillNode child, SkillNode parent) {
        if (historyManager != null) {
            historyManager.pushSnapshot();
        }
        if (child.getParents().contains(parent.getId())) {
            child.getParents().remove(parent.getId());
        } else {
            child.getParents().add(parent.getId());
        }
    }

    private SkillNode findNodeAt(float wx, float wy) {
        float maxDistSq = (NODE_RADIUS + HIT_PADDING) * (NODE_RADIUS + HIT_PADDING);
        for (SkillNode node : SkillRegistry.NODES.values()) {
            float dx = wx - node.getX();
            float dy = wy - node.getY();
            if (dx * dx + dy * dy <= maxDistSq) {
                return node;
            }
        }
        return null;
    }

    public float toWorldCoordX(float screenX) {
        return (screenX - getWidth() * 0.5f - offsetX) / scale;
    }

    public float toWorldCoordY(float screenY) {
        return (screenY - getHeight() * 0.5f - offsetY) / scale;
    }

    private void clearHoverNode() {
        if (hoveredNode != null) {
            hoveredNode = null;
            invalidate();
            if (hoverListener != null) {
                hoverListener.accept(null, null);
            }
        }
    }

    private void updateHoverNode(float screenX, float screenY, MotionEvent event) {
        SkillNode found = findNodeAt(toWorldCoordX(screenX), toWorldCoordY(screenY));
        if (this.hoveredNode != found) {
            this.hoveredNode = found;
            invalidate();
        }
        if (hoverListener != null) {
            hoverListener.accept(found, event);
        }
    }

    private void handleClick(float screenX, float screenY) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerSkillData data = player.getData(SKILL_DATA.get());

        SkillNode node = findNodeAt(toWorldCoordX(screenX), toWorldCoordY(screenY));
        if (node != null) {
            int currentRank = data.getRank(node.getId());
            boolean isMax = currentRank >= node.getMaxRank();

            if (data.getPoints() > 0 && !isMax && areParentsMet(node, data)) {
                PacketDistributor.sendToServer(new ModPayloads.UpgradeSkillPayload(node.getId()));
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.25f, 0.35f)
                );
                onPointsChanged.run();
            } else {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.7f)
                );
            }
        }
    }

    public boolean areParentsMet(SkillNode node, PlayerSkillData data) {
        for (String parentId : node.getParents()) {
            if (data.getRank(parentId) <= 0) return false;
        }
        return true;
    }
}