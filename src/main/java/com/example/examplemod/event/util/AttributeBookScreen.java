package com.example.examplemod.event.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public final class AttributeBookScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF6666;

    private final AttributeBookData.Table table;

    private final Map<String, String> draftText = new HashMap<>();
    private final Set<String> invalidDrafts = new HashSet<>();
    private final List<Row> visibleRows = new ArrayList<>();

    private String entityId;
    private boolean dirty;
    private boolean showHiddenAttributes;

    private String filter = "";
    private int scroll;
    private int rowCount;
    private String pickerQuery = "";
    private int pickerPage;
    private int split;
    private int listTop;
    private int listBottom;
    private int listX;
    private int listWidth;
    private int pageRows;

    private EditBox search;
    private Button toggleHidden;

    private LivingEntity preview;
    private String previewError = "";

    private float yaw = 25.0F;
    private float pitch = 0.0F;
    private float zoom = 1.0F;
    private float panX;
    private float panY;
    private boolean modelDragging;

    private String status = "";
    private int statusColor = 0xA0A0A0;

    private record Row(
            String id,
            int y,
            int labelWidth,
            EditBox editor,
            Button reset
    ) {}

    public AttributeBookScreen(AttributeBookData.Table table) {
        super(Component.literal("实体属性图鉴"));
        this.table = table;

        this.entityId = table.entities.keySet().stream()
                .filter(id -> !AttributeBookData.isExcluded(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void init() {
        split = Mth.clamp(
                (int) (width * 0.40F),
                110,
                Math.max(110, width - 190)
        );

        listX = split + 10;
        listWidth = Math.max(160, width - listX - 10);
        listTop = 63;
        listBottom = height - 35;
        pageRows = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);

        visibleRows.clear();

        addRenderableWidget(
                Button.builder(
                                Component.literal("保存"),
                                button -> save()
                        )
                        .bounds(width - 65, 9, 55, 20)
                        .build()
        );

        int pickW = Math.max(50, split - 55);
        int exclW = 45;

        addRenderableWidget(
                Button.builder(
                                Component.literal("选择实体"),
                                button -> minecraft.setScreen(new EntityPicker(this))
                        )
                        .bounds(10, 35, pickW, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("排除"),
                                button -> excludeCurrent()
                        )
                        .bounds(10 + pickW + 3, 35, exclW, 20)
                        .build()
        );

        /*
         * 右侧工具栏：[切换显示] [搜索框]
         */
        int toggleW = 74;
        int searchW = Math.max(80, listWidth - toggleW - 5);

        toggleHidden = addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        showHiddenAttributes ? "仅常用" : "显示全部"
                                ),
                                button -> {
                                    showHiddenAttributes = !showHiddenAttributes;
                                    scroll = 0;
                                    rebuildRows();
                                }
                        )
                        .bounds(listX, 35, toggleW, 20)
                        .build()
        );

        search = new EditBox(
                font,
                listX + toggleW + 5,
                35,
                searchW,
                20,
                Component.literal("搜索属性")
        );

        search.setMaxLength(256);
        search.setHint(Component.literal("搜索属性名称或 ID"));
        search.setValue(filter);
        search.setResponder(value -> {
            filter = value;
            scroll = 0;
            rebuildRows();
        });

        addRenderableWidget(search);

        if (preview == null && previewError.isEmpty()) {
            createPreview();
        }

        rebuildRows();
    }

    private void rebuildRows() {
        for (Row row : visibleRows) {
            removeWidget(row.editor());
            if (row.reset() != null) {
                removeWidget(row.reset());
            }
        }
        visibleRows.clear();

        if (entityId == null) {
            rowCount = 0;
            return;
        }

        Map<String, Double> values = AttributeBookData.entityValues(entityId);
        String query = filter.toLowerCase(Locale.ROOT);

        List<String> ids = values.entrySet().stream()
                .filter(entry -> {
                    String id = entry.getKey();
                    if (!query.isBlank()
                            && !id.toLowerCase(Locale.ROOT).contains(query)
                            && !attributeName(id).getString()
                            .toLowerCase(Locale.ROOT).contains(query)) {
                        return false;
                    }
                    return isVisible(id, entry.getValue());
                })
                .map(Map.Entry::getKey)
                .sorted(Comparator
                        .<String, Boolean>comparing(
                                id -> AttributeBookData.isRegisteredDefault(
                                        id, values.get(id)))
                        .thenComparing(Comparator.naturalOrder()))
                .toList();

        rowCount = ids.size();
        scroll = Mth.clamp(scroll, 0, Math.max(0, rowCount - pageRows));

        int resetWidth = 35;
        int editWidth = Math.min(100, Math.max(65, listWidth / 3));
        int editX = listX + listWidth - editWidth - resetWidth;
        int labelWidth = Math.max(20, editX - listX - 5);

        for (int i = 0; i < pageRows && scroll + i < ids.size(); i++) {
            String id = ids.get(scroll + i);
            String key = draftKey(id);
            int y = listTop + i * ROW_HEIGHT;

            double value = values.get(id);

            EditBox editor = new EditBox(
                    font, editX, y + 2, editWidth, 20, attributeName(id)
            );
            editor.setMaxLength(128);
            editor.setValue(draftText.getOrDefault(key, number(value)));
            editor.setTextColor(valueColor(id, value));
            editor.setResponder(text -> editValue(id, text, editor));
            addRenderableWidget(editor);

            Button reset = addRenderableWidget(Button.builder(
                    Component.literal("重置"),
                    button -> resetAttribute(id)
            ).bounds(editX + editWidth + 2, y + 2, 33, 20).build());

            visibleRows.add(new Row(id, y, labelWidth, editor, reset));
        }
    }

    /*
     * 显示判定：
     *   非默认值 —— 永远显示（避免已修改的属性被隐藏）
     *   白名单   —— 始终显示
     *   其他     —— 由“显示全部”开关控制
     */
    private boolean isVisible(String id, double value) {
        if (!AttributeBookData.isRegisteredDefault(id, value)) {
            return true;
        }
        if (AttributeBookData.ALWAYS_VISIBLE.contains(id)) {
            return true;
        }
        return showHiddenAttributes;
    }

    private int valueColor(String id, double value) {
        if (invalidDrafts.contains(draftKey(id))) {
            return RED;
        }
        return AttributeBookData.isRegisteredDefault(id, value) ? WHITE : RED;
    }

    private String draftKey(String attributeId) {
        return entityId + "|" + attributeId;
    }

    private void editValue(String id, String text, EditBox editor) {
        String key = draftKey(id);
        draftText.put(key, text);

        double value;
        try {
            value = Double.parseDouble(text.trim());
            String error = AttributeBookData.valueError(id, value);
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
        } catch (IllegalArgumentException exception) {
            invalidDrafts.add(key);
            editor.setTextColor(RED);
            status = "存在无效输入：请输入属性允许范围内的有限数字";
            statusColor = 0xFF7777;
            return;
        }

        invalidDrafts.remove(key);

        var entry = table.entities.computeIfAbsent(
                entityId,
                k -> new AttributeBookData.EntityEntry()
        );
        entry.attributes.put(id, value);

        editor.setTextColor(valueColor(id, value));
        markChanged();
    }

    private void resetAttribute(String id) {
        AttributeBookData.EntityEntry entry = table.entities.get(entityId);
        if (entry != null) {
            entry.attributes.remove(id);
        }
        draftText.remove(draftKey(id));
        invalidDrafts.remove(draftKey(id));
        markChanged();
        rebuildRows();
    }

    private void markChanged() {
        dirty = true;
        status = "已修改；保存后完全重启游戏生效";
        statusColor = 0xFFD080;
        applyTableToPreview();
    }

    private void save() {
        if (!invalidDrafts.isEmpty()) {
            status = "无法保存：还有 " + invalidDrafts.size() + " 项无效输入";
            statusColor = 0xFF7777;
            return;
        }

        try {
            AttributeBookData.save(table);
        } catch (IOException | RuntimeException exception) {
            status = "保存失败：" + exception.getMessage();
            statusColor = 0xFF7777;
            return;
        }

        dirty = false;
        status = "已保存到本地 JSON；完全重启游戏后应用";
        statusColor = 0x88FF88;
        rebuildRows();
    }

    private void selectEntity(String id) {
        entityId = id;
        filter = "";
        scroll = 0;
        draftText.clear();
        invalidDrafts.clear();
        preview = null;
        previewError = "";
        resetView();
        createPreview();
        rebuildRows();
    }

    private void excludeCurrent() {
        if (entityId == null) {
            return;
        }

        toggleExcluded(entityId);

        String next = table.entities.keySet().stream()
                .filter(id -> !AttributeBookData.isExcluded(id))
                .findFirst()
                .orElse(null);

        entityId = next;
        preview = null;
        previewError = "";
        filter = "";
        scroll = 0;
        draftText.clear();
        invalidDrafts.clear();
        resetView();
        rebuildWidgets();
    }

    private void toggleExcluded(String id) {
        if (table.excluded_entities.contains(id)) {
            table.excluded_entities.remove(id);
            status = "已恢复：" + id;
        } else {
            table.excluded_entities.add(id);
            status = "已排除：" + id;
        }
        dirty = true;
        statusColor = 0xFFD080;
    }

    private void resetView() {
        yaw = 25.0F;
        pitch = 0.0F;
        zoom = 1.0F;
        panX = 0;
        panY = 0;
    }

    private void createPreview() {
        preview = null;
        previewError = "";

        if (entityId == null || minecraft.level == null) {
            previewError = "没有可预览的实体";
            return;
        }

        ResourceLocation location = ResourceLocation.tryParse(entityId);
        EntityType<?> type = location == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE
                .getOptional(location)
                .orElse(null);

        if (type == null) {
            previewError = "该实体当前未注册";
            return;
        }

        try {
            Entity entity = type.create(minecraft.level);
            if (!(entity instanceof LivingEntity living)) {
                previewError = "该实体不能创建 LivingEntity 预览";
                return;
            }

            preview = living;
            preview.setYRot(0);
            preview.setXRot(0);
            preview.yRotO = 0;
            preview.xRotO = 0;
            preview.yBodyRot = 0;
            preview.yBodyRotO = 0;
            preview.yHeadRot = 0;
            preview.yHeadRotO = 0;

            applyTableToPreview();
        } catch (RuntimeException exception) {
            previewError = "创建预览失败："
                    + exception.getClass().getSimpleName();
        }
    }

    private void applyTableToPreview() {
        if (preview == null || entityId == null) {
            return;
        }

        for (var row : AttributeBookData.resolved(entityId).entrySet()) {
            Holder.Reference<Attribute> holder =
                    AttributeBookData.attribute(row.getKey());
            if (holder == null) {
                continue;
            }

            AttributeInstance instance = preview.getAttribute(holder);
            if (instance != null) {
                instance.setBaseValue(row.getValue());
            }
        }

        preview.refreshDimensions();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, width, height, 0xF0181B22);
        graphics.fill(5, 60, split - 4, height - 35, 0xFF11151C);
        graphics.fill(split + 4, 60, width - 5, height - 35, 0xFF202630);

        String heading = entityName(entityId).getString() + (dirty ? " *" : "");

        graphics.drawString(
                font,
                font.plainSubstrByWidth(heading, Math.max(10, width - 190)),
                10,
                15,
                0xFFFFFF,
                false
        );

        renderPreview(graphics);

        graphics.drawString(
                font,
                font.plainSubstrByWidth(
                        entityId == null ? "" : entityId,
                        Math.max(10, split - 20)
                ),
                12,
                64,
                0x8694A8,
                false
        );

        graphics.drawString(
                font,
                "左拖旋转  右/中拖平移",
                12,
                height - 60,
                0xA4B0C0,
                false
        );
        graphics.drawString(
                font,
                "滚轮缩放",
                12,
                height - 48,
                0xA4B0C0,
                false
        );

        for (Row row : visibleRows) {
            String name = attributeName(row.id()).getString();
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(name, row.labelWidth()),
                    listX + 2,
                    row.y() + 7,
                    WHITE,
                    false
            );
        }

        drawScrollbar(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        String footer = status.isEmpty()
                ? "Esc 关闭 | 只有点击保存才写入文件"
                : status;

        graphics.drawString(
                font,
                font.plainSubstrByWidth(footer, Math.max(10, width - 20)),
                10,
                height - 22,
                statusColor,
                false
        );

        for (Row row : visibleRows) {
            if (mouseX >= listX
                    && mouseX < row.editor().getX()
                    && mouseY >= row.y()
                    && mouseY < row.y() + ROW_HEIGHT) {
                graphics.renderComponentTooltip(
                        font,
                        tooltip(row.id()),
                        mouseX,
                        mouseY
                );
                break;
            }
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        int x1 = 10;
        int y1 = 80;
        int x2 = split - 10;
        int y2 = height - 68;

        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        if (preview == null) {
            graphics.drawWordWrap(
                    font,
                    Component.literal(previewError),
                    x1 + 5,
                    y1 + 10,
                    Math.max(20, x2 - x1 - 10),
                    0xFF9999
            );
            return;
        }

        float entityScale = Math.max(0.001F, preview.getScale());
        float modelHeight = Math.max(0.1F, preview.getBbHeight() / entityScale);
        float modelWidth = Math.max(0.1F, preview.getBbWidth() / entityScale);

        float fit = Math.min(
                (x2 - x1) / (modelWidth * 1.42F),
                (y2 - y1) / modelHeight
        ) * 0.72F;

        float scale = fit * zoom / entityScale;
        float centerX = (x1 + x2) / 2.0F + panX;
        float centerY = (y1 + y2) / 2.0F + panY;

        Quaternionf rotation = new Quaternionf()
                .rotationZ((float) Math.PI)
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw));

        Vector3f translation = new Vector3f(
                0.0F,
                preview.getBbHeight() / 2.0F,
                0.0F
        );

        graphics.enableScissor(x1, y1, x2, y2);

        try {
            InventoryScreen.renderEntityInInventory(
                    graphics,
                    centerX,
                    centerY,
                    scale,
                    translation,
                    rotation,
                    new Quaternionf(),
                    preview
            );
        } finally {
            graphics.disableScissor();
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (rowCount <= pageRows) {
            return;
        }

        int trackHeight = pageRows * ROW_HEIGHT;
        int thumbHeight = Math.max(12, trackHeight * pageRows / rowCount);

        int thumbY = listTop
                + (trackHeight - thumbHeight) * scroll
                / Math.max(1, rowCount - pageRows);

        graphics.fill(width - 5, listTop, width - 3,
                listTop + trackHeight, 0xFF37404D);
        graphics.fill(width - 5, thumbY, width - 3,
                thumbY + thumbHeight, 0xFF9CB7D7);
    }

    private List<Component> tooltip(String id) {
        List<Component> lines = new ArrayList<>();
        lines.add(attributeName(id));
        lines.add(Component.literal(id));

        Holder.Reference<Attribute> holder = AttributeBookData.attribute(id);

        if (holder != null) {
            Attribute attribute = holder.value();
            String key = attribute.getDescriptionId();

            for (String suffix : List.of(".desc", ".description", ".tooltip")) {
                String descriptionKey = key + suffix;
                if (I18n.exists(descriptionKey)) {
                    for (String line : I18n.get(descriptionKey).split("\n")) {
                        lines.add(Component.literal(line));
                    }
                    break;
                }
            }

            if (attribute instanceof RangedAttribute ranged) {
                lines.add(Component.literal(
                        "范围：" + number(ranged.getMinValue())
                                + " ～ " + number(ranged.getMaxValue())
                ));
            }

            lines.add(Component.literal(
                    "注册默认值：" + number(attribute.getDefaultValue())
            ));
        } else {
            lines.add(Component.literal("该属性当前未注册"));
        }

        AttributeBookData.EntityEntry entry = table.entities.get(entityId);
        if (entry != null && entry.attributes.containsKey(id)) {
            lines.add(Component.literal("当前来源：实体覆盖值"));
        } else {
            lines.add(Component.literal("当前来源：实体默认供应器"));
        }

        return lines;
    }

    private boolean inPreview(double mouseX, double mouseY) {
        return mouseX >= 10
                && mouseX < split - 10
                && mouseY >= 80
                && mouseY < height - 68;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inPreview(mouseX, mouseY)) {
            setFocused(null);
            modelDragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button,
            double dragX, double dragY
    ) {
        if (modelDragging) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                yaw += (float) dragX * 0.8F;
                pitch = Mth.clamp(
                        pitch + (float) dragY * 0.6F,
                        -80.0F, 80.0F
                );
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                panX += (float) dragX;
                panY += (float) dragY;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (modelDragging) {
            modelDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY,
            double scrollX, double scrollY
    ) {
        if (inPreview(mouseX, mouseY)) {
            zoom = Mth.clamp(
                    zoom * (float) Math.pow(1.12, scrollY),
                    0.01F, 30.0F
            );
            return true;
        }

        if (mouseX >= listX && mouseY >= listTop && mouseY <= listBottom) {
            scroll -= (int) Math.signum(scrollY) * 3;
            rebuildRows();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        modelDragging = false;
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static Component attributeName(String id) {
        Holder.Reference<Attribute> holder = AttributeBookData.attribute(id);
        return holder == null
                ? Component.literal(id)
                : Component.translatable(holder.value().getDescriptionId());
    }

    private static Component entityName(String id) {
        if (id == null) {
            return Component.literal("没有实体");
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Component.literal(id);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(location)
                .map(EntityType::getDescription)
                .orElseGet(() -> Component.literal(id));
    }

    /*
     * 实体选择器：每个条目带“排除/恢复”按钮。
     * 记忆搜索词、页码，以及当前选中实体所在页并高亮它。
     */
    private static final class EntityPicker extends Screen {
        private final AttributeBookScreen parent;
        private final List<Button> entityButtons = new ArrayList<>();

        private String query;
        private int page;
        private int perPage;
        private List<String> matching = List.of();

        private Button previous;
        private Button next;

        private EntityPicker(AttributeBookScreen parent) {
            super(Component.literal("选择实体"));
            this.parent = parent;
            this.query = parent.pickerQuery;
            this.page = parent.pickerPage;
        }

        @Override
        protected void init() {
            entityButtons.clear();
            perPage = Math.max(1, (height - 100) / 24);

            EditBox search = new EditBox(
                    font,
                    20,
                    30,
                    Math.max(100, width - 40),
                    20,
                    Component.literal("搜索实体")
            );
            search.setMaxLength(256);
            search.setHint(Component.literal("实体名称、ID 或模组 ID"));
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                parent.pickerQuery = value;
                page = 0;
                parent.pickerPage = 0;
                rebuildList();
            });
            addRenderableWidget(search);

            previous = addRenderableWidget(Button.builder(
                    Component.literal("上一页"),
                    button -> {
                        page--;
                        parent.pickerPage = page;
                        rebuildList();
                    }
            ).bounds(20, height - 30, 60, 20).build());

            next = addRenderableWidget(Button.builder(
                    Component.literal("下一页"),
                    button -> {
                        page++;
                        parent.pickerPage = page;
                        rebuildList();
                    }
            ).bounds(85, height - 30, 60, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal("返回"),
                    button -> minecraft.setScreen(parent)
            ).bounds(width - 80, height - 30, 60, 20).build());

            rebuildList();

            /*
             * 首次进入且无搜索词时，跳到当前选中实体所在页，
             * 让它一进来就在视野内。
             */
            if (query.isBlank() && parent.entityId != null) {
                int index = matching.indexOf(parent.entityId);
                if (index >= 0) {
                    int targetPage = index / perPage;
                    if (targetPage != page) {
                        page = targetPage;
                        parent.pickerPage = page;
                        rebuildList();
                    }
                }
            }

            setInitialFocus(search);
        }

        private void rebuildList() {
            for (Button button : entityButtons) {
                removeWidget(button);
            }
            entityButtons.clear();

            String normalized = query.toLowerCase(Locale.ROOT);

            matching = parent.table.entities.keySet().stream()
                    .filter(id -> normalized.isBlank()
                            || id.toLowerCase(Locale.ROOT).contains(normalized)
                            || entityName(id).getString()
                            .toLowerCase(Locale.ROOT).contains(normalized))
                    .toList();

            int pageCount = Math.max(
                    1,
                    (matching.size() + perPage - 1) / perPage
            );
            page = Mth.clamp(page, 0, pageCount - 1);
            parent.pickerPage = page;

            previous.active = page > 0;
            next.active = page + 1 < pageCount;

            for (int i = 0; i < perPage; i++) {
                int index = page * perPage + i;
                if (index >= matching.size()) {
                    break;
                }

                String id = matching.get(index);
                boolean excluded = AttributeBookData.isExcluded(id);
                boolean current = id.equals(parent.entityId);

                String base = entityName(id).getString() + "  [" + id + "]";
                String label;
                if (excluded) {
                    label = "✖ " + base;
                } else if (current) {
                    label = "▶ " + base;
                } else {
                    label = base;
                }

                int y = 58 + i * 24;
                int exclWidth = 44;
                int mainWidth = Math.max(60, width - 40 - exclWidth - 4);

                Button main = Button.builder(
                        Component.literal(font.plainSubstrByWidth(
                                label, Math.max(30, mainWidth - 8))),
                        clicked -> {
                            if (excluded) {
                                parent.toggleExcluded(id);
                                rebuildList();
                            } else {
                                parent.selectEntity(id);
                                minecraft.setScreen(parent);
                            }
                        }
                ).bounds(20, y, mainWidth, 20).build();

                // 当前实体的按钮加边框色，视觉上更明显
                if (current) {
                    main.setFGColor(0xFFD080);
                }

                Button excl = Button.builder(
                        Component.literal(excluded ? "恢复" : "排除"),
                        clicked -> {
                            parent.toggleExcluded(id);
                            rebuildList();
                        }
                ).bounds(20 + mainWidth + 4, y, exclWidth, 20).build();

                entityButtons.add(main);
                entityButtons.add(excl);
                addRenderableWidget(main);
                addRenderableWidget(excl);
            }
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            graphics.fill(0, 0, width, height, 0xF0181B22);

            graphics.drawString(
                    font,
                    "选择实体 · 共 " + matching.size() + " 项",
                    20,
                    12,
                    0xFFFFFF,
                    false
            );

            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseScrolled(
                double mouseX, double mouseY,
                double scrollX, double scrollY
        ) {
            page -= (int) Math.signum(scrollY);
            parent.pickerPage = page;
            rebuildList();
            return true;
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}