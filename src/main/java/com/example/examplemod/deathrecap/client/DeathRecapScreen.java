package com.example.examplemod.deathrecap.client;

import com.example.examplemod.deathrecap.DamageEventSnapshot;
import com.example.examplemod.deathrecap.EntitySnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathRecapScreen extends Screen {

    public enum Tab {
        OVERVIEW("§f概览与装备"),
        STACK_TRACE("§c案发代码堆栈"),
        RAW_NBT("§e底层NBT数据");

        public final String title;
        Tab(String title) { this.title = title; }
    }

    private final List<DamageEventSnapshot> records;
    private final Screen previousScreen;
    private int selectedIndex = 0;
    private Tab currentTab = Tab.OVERVIEW;
    private LivingEntity clientDummyEntity = null;

    // 滚动系统
    private int overviewScrollY = 0;
    private int maxOverviewScroll = 0;
    private int textScrollY = 0;
    private int maxTextScroll = 0;

    // 折行缓存
    private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();
    private final List<String> rawLinesForCopy = new ArrayList<>();
    private String copyNotification = null;
    private int copyNotificationTimer = 0;

    private static final String[] SLOT_NAMES = {"主手", "副手", "脚部", "腿部", "胸部", "头部"};

    public DeathRecapScreen(List<DamageEventSnapshot> records) {
        this(records, null);
    }

    public DeathRecapScreen(List<DamageEventSnapshot> records, Screen previousScreen) {
        super(Component.literal("死亡快照与伤害回放浏览器"));
        this.records = records;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        updateSelectedRecord();
    }

    private void updateSelectedRecord() {
        if (records.isEmpty()) return;
        this.overviewScrollY = 0;
        this.textScrollY = 0;
        updateDummyEntity();
        rebuildTextContent();
    }

    private void updateDummyEntity() {
        DamageEventSnapshot event = records.get(selectedIndex);
        EntitySnapshot snapshot = (event.attacker != null) ? event.attacker : event.victim;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if ("minecraft:player".equals(snapshot.entityTypeId) && mc.player != null) {
            this.clientDummyEntity = mc.player;
            this.clientDummyEntity.deathTime = 0;
            this.clientDummyEntity.hurtTime = 0;
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(snapshot.entityTypeId));
        if (type != null) {
            var entity = type.create(mc.level);
            if (entity instanceof LivingEntity living) {
                try {
                    living.load(snapshot.entityNbt);
                } catch (Exception ignored) {}

                EquipmentSlot[] slots = EquipmentSlot.values();
                for (int i = 0; i < Math.min(slots.length, snapshot.equipment.size()); i++) {
                    living.setItemSlot(slots[i], snapshot.equipment.get(i).copy());
                }
                living.deathTime = 0;
                this.clientDummyEntity = living;
            }
        }
    }

    // 根据当前 Tab 与分配的可用宽度，进行原生字符级自适应折行计算
    private void rebuildTextContent() {
        this.wrappedLines.clear();
        this.rawLinesForCopy.clear();
        if (records.isEmpty()) return;

        DamageEventSnapshot event = records.get(selectedIndex);
        // 代码和 NBT 享有超宽视口（从 x = 145 到 width - 25）
        int availableWidth = Math.max(100, this.width - 170);

        if (currentTab == Tab.STACK_TRACE) {
            List<String> stack = event.callStack;
            if (stack != null && !stack.isEmpty()) {
                for (int i = 0; i < stack.size(); i++) {
                    String line = stack.get(i);
                    rawLinesForCopy.add(line);
                    boolean isMod = !line.contains("net.minecraft.") && !line.contains("java.lang.");
                    String formatted = String.format("%s%02d. %s", isMod ? "§6" : "§8", i, line);

                    // 使用 font.split 按视口宽度自动切分换行，永不截断
                    List<FormattedCharSequence> split = this.font.split(Component.literal(formatted), availableWidth);
                    for (int s = 0; s < split.size(); s++) {
                        if (s > 0) {
                            // 换行添加缩进指示符
                            this.wrappedLines.add(this.font.split(Component.literal("    §7↳ §f"), availableWidth).get(0));
                        }
                        this.wrappedLines.add(split.get(s));
                    }
                }
            } else {
                wrappedLines.addAll(this.font.split(Component.literal("§8(未捕获到方法调用栈)"), availableWidth));
            }
        } else if (currentTab == Tab.RAW_NBT) {
            EntitySnapshot target = (event.attacker != null) ? event.attacker : event.victim;
            CompoundTag nbt = target.entityNbt;
            if (nbt != null && !nbt.isEmpty()) {
                for (String key : nbt.getAllKeys()) {
                    var tag = nbt.get(key);
                    String val = (tag != null) ? tag.getAsString() : "null";
                    rawLinesForCopy.add(key + ": " + val);
                    String formatted = "§6" + key + "§7: §f" + val;

                    List<FormattedCharSequence> split = this.font.split(Component.literal(formatted), availableWidth);
                    this.wrappedLines.addAll(split);
                }
            } else {
                wrappedLines.addAll(this.font.split(Component.literal("§8(此实体无可用 NBT)"), availableWidth));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        if (records.isEmpty()) {
            graphics.drawCenteredString(this.font, "无可用死亡快照", this.width / 2, this.height / 2, 0xFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        DamageEventSnapshot currentEvent = records.get(selectedIndex);
        EntitySnapshot target = (currentEvent.attacker != null) ? currentEvent.attacker : currentEvent.victim;

        // ================= 1. 左栏：受击回溯列表 (固定宽度 130) =================
        int leftX = 10;
        int listTopY = 22;
        graphics.drawString(this.font, "§e§l【受击回溯列表】", leftX, listTopY, 0xFFFFFF);
        for (int i = 0; i < records.size(); i++) {
            DamageEventSnapshot r = records.get(i);
            int itemY = listTopY + 14 + i * 20;

            if (i == selectedIndex) {
                graphics.fill(leftX - 2, itemY - 2, leftX + 125, itemY + 16, 0x55FFAA00);
            } else if (mouseX >= leftX - 2 && mouseX <= leftX + 125 && mouseY >= itemY - 2 && mouseY <= itemY + 16) {
                graphics.fill(leftX - 2, itemY - 2, leftX + 125, itemY + 16, 0x33FFFFFF);
            }

            String prefix = (i == selectedIndex) ? "§6▶ " : "  ";
            boolean isFatal = (i == records.size() - 1);
            String title = String.format("%s%s-%.1f HP §7[%s]",
                    prefix, (isFatal ? "§4§l" : "§c"), r.damage, (r.attacker != null ? r.attacker.entityName : "环境"));
            graphics.drawString(this.font, title, leftX, itemY + 2, 0xFFFFFF);
        }

        // ================= 2. 顶部 Tab 标签栏 =================
        int tabStartX = (currentTab == Tab.OVERVIEW) ? 250 : 145;
        int tabY = 8;
        Tab[] tabs = Tab.values();
        for (int t = 0; t < tabs.length; t++) {
            Tab tab = tabs[t];
            int tabX = tabStartX + t * 76;
            boolean isCurrent = (this.currentTab == tab);
            int bgColor = isCurrent ? 0x88FFAA00 : 0x55000000;
            if (mouseX >= tabX && mouseX <= tabX + 72 && mouseY >= tabY && mouseY <= tabY + 15) {
                bgColor = isCurrent ? 0xAAFFAA00 : 0x77FFFFFF;
            }
            graphics.fill(tabX, tabY, tabX + 72, tabY + 15, bgColor);
            graphics.drawCenteredString(this.font, tab.title, tabX + 36, tabY + 3, 0xFFFFFF);
        }

        // ================= 3. 核心区域：自适应排版 =================
        int viewTop = 28;
        int viewBottom = this.height - 35;

        if (this.currentTab == Tab.OVERVIEW) {
            // 【概览模式】：展示 3D 假人与右侧独立装备滚动面板
            int midCenterX = 185;
            int midCenterY = this.height / 2 + 35;
            if (this.clientDummyEntity != null) {
                graphics.fill(midCenterX - 45, midCenterY - 120, midCenterX + 45, midCenterY + 10, 0x33000000);
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics,
                        midCenterX - 40, midCenterY - 115,
                        midCenterX + 40, midCenterY + 5,
                        36, 0.0625f, mouseX, mouseY, this.clientDummyEntity
                );
            }
            graphics.drawCenteredString(this.font, "§a" + target.entityName, midCenterX, midCenterY - 135, 0xFFFFFF);
            graphics.drawCenteredString(this.font, String.format("§dHP: %.1f / %.1f", target.health, target.maxHealth), midCenterX, midCenterY + 14, 0xFFFFFF);

            int rightAreaX = 250;
            int viewRight = this.width - 10;
            graphics.fill(rightAreaX - 4, viewTop, viewRight, viewBottom, 0x44000000);
            renderOverviewScrollable(graphics, target, currentEvent, rightAreaX, viewTop, viewBottom, viewRight - rightAreaX, mouseX, mouseY);

        } else {
            // 【代码与 NBT 模式】：彻底隐去假人，全屏宽视口（x: 145 ~ width - 10），宽度直接拉满！
            int consoleX = 145;
            int consoleRight = this.width - 10;
            graphics.fill(consoleX - 4, viewTop, consoleRight, viewBottom, 0x55000000);

            // 绘制“一键复制全部”按钮
            int copyBtnX = consoleRight - 85;
            int copyBtnY = tabY;
            boolean copyHover = mouseX >= copyBtnX && mouseX <= copyBtnX + 80 && mouseY >= copyBtnY && mouseY <= copyBtnY + 15;
            graphics.fill(copyBtnX, copyBtnY, copyBtnX + 80, copyBtnY + 15, copyHover ? 0xAA00AAFF : 0x660055AA);
            graphics.drawCenteredString(this.font, "§b[📋 复制全部]", copyBtnX + 40, copyBtnY + 3, 0xFFFFFF);

            // 绘制自动折行代码流
            renderWrappedTextConsole(graphics, consoleX, viewTop, viewBottom, consoleRight - consoleX);
        }

        // ================= 4. 底部状态与返回按钮 =================
        if (copyNotificationTimer > 0) {
            copyNotificationTimer--;
            graphics.drawCenteredString(this.font, copyNotification, this.width / 2, this.height - 20, 0x55FF55);
        }

        int btnWidth = 80;
        int btnHeight = 18;
        int btnX = this.width - btnWidth - 12;
        int btnY = this.height - btnHeight - 8;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnHover ? 0xAAFF3333 : 0x66FF0000);
        String btnText = (this.previousScreen instanceof DeathScreen) ? "§f返回死亡界面" : "§f关闭界面";
        graphics.drawCenteredString(this.font, btnText, btnX + btnWidth / 2, btnY + 5, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // 全宽代码控制台渲染（完全基于 font.split 结果，绝对不超行宽）
    private void renderWrappedTextConsole(GuiGraphics graphics, int viewX, int viewTop, int viewBottom, int viewWidth) {
        graphics.enableScissor(viewX - 4, viewTop, viewX + viewWidth, viewBottom);

        int lineHeight = 11;
        int totalContentHeight = this.wrappedLines.size() * lineHeight + 10;
        int viewHeight = viewBottom - viewTop;
        this.maxTextScroll = Math.max(0, totalContentHeight - viewHeight);

        for (int i = 0; i < this.wrappedLines.size(); i++) {
            int lineY = viewTop + 6 + i * lineHeight - this.textScrollY;
            if (lineY >= viewTop - lineHeight && lineY <= viewBottom) {
                graphics.drawString(this.font, this.wrappedLines.get(i), viewX, lineY, 0xFFFFFF);
            }
        }

        graphics.disableScissor();

        // 垂直滚动指示条
        if (this.maxTextScroll > 0) {
            int scrollbarX = viewX + viewWidth - 4;
            int barHeight = Math.max(15, (int) ((float) viewHeight / totalContentHeight * viewHeight));
            int barY = viewTop + (int) ((float) this.textScrollY / this.maxTextScroll * (viewHeight - barHeight));
            graphics.fill(scrollbarX, barY, scrollbarX + 3, barY + barHeight, 0xAAFFAA00);
        }
    }

    private void renderOverviewScrollable(GuiGraphics graphics, EntitySnapshot target, DamageEventSnapshot event,
                                          int viewX, int viewTop, int viewBottom, int viewWidth, int mouseX, int mouseY) {
        ItemStack hoveredStack = ItemStack.EMPTY;
        graphics.enableScissor(viewX - 4, viewTop, viewX + viewWidth, viewBottom);

        int contentY = viewTop + 6 - this.overviewScrollY;

        graphics.drawString(this.font, "§c伤害源ID: §e" + event.damageTypeId, viewX, contentY, 0xFFFFFF);
        contentY += 16;

        graphics.drawString(this.font, "§b【全身防具与主副手】:", viewX, contentY, 0xFFFFFF);
        contentY += 12;

        for (int i = 0; i < 6 && i < target.equipment.size(); i++) {
            ItemStack stack = target.equipment.get(i);
            int slotX = viewX + i * 22;
            int slotY = contentY;

            graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x66000000);
            graphics.drawCenteredString(this.font, "§7" + SLOT_NAMES[i], slotX + 8, slotY + 18, 0xAAAAAA);

            if (!stack.isEmpty()) {
                graphics.renderItem(stack, slotX, slotY);
                graphics.renderItemDecorations(this.font, stack, slotX, slotY);
                if (isMouseInScissor(mouseX, mouseY, slotX, slotY, 16, 16, viewTop, viewBottom)) {
                    hoveredStack = stack;
                }
            }
        }
        contentY += 32;

        graphics.drawString(this.font, "§d【佩戴饰品 (" + target.curios.size() + "件 - 滚轮滑动)】:", viewX, contentY, 0xFFFFFF);
        contentY += 14;

        if (target.curios.isEmpty()) {
            graphics.drawString(this.font, "§8(无佩戴饰品)", viewX, contentY, 0x888888);
            contentY += 14;
        } else {
            int itemsPerRow = Math.max(6, (viewWidth - 10) / 22);
            for (int c = 0; c < target.curios.size(); c++) {
                ItemStack curio = target.curios.get(c);
                int cX = viewX + (c % itemsPerRow) * 22;
                int cY = contentY + (c / itemsPerRow) * 22;

                graphics.fill(cX - 1, cY - 1, cX + 17, cY + 17, 0x55330055);

                if (!curio.isEmpty()) {
                    graphics.renderItem(curio, cX, cY);
                    graphics.renderItemDecorations(this.font, curio, cX, cY);
                    if (isMouseInScissor(mouseX, mouseY, cX, cY, 16, 16, viewTop, viewBottom)) {
                        hoveredStack = curio;
                    }
                }
            }
            int curioRows = (int) Math.ceil(target.curios.size() / (double) itemsPerRow);
            contentY += curioRows * 22 + 8;
        }

        graphics.drawString(this.font, "§9【当时携带状态效果】:", viewX, contentY, 0xFFFFFF);
        contentY += 13;

        if (target.activeEffects.isEmpty()) {
            graphics.drawString(this.font, "§8(无状态效果)", viewX, contentY, 0x888888);
            contentY += 12;
        } else {
            for (String effectStr : target.activeEffects) {
                graphics.drawString(this.font, "§7• " + effectStr, viewX, contentY, 0xDDDDDD);
                contentY += 11;
            }
        }

        int totalContentHeight = (contentY + this.overviewScrollY) - viewTop + 10;
        int viewHeight = viewBottom - viewTop;
        this.maxOverviewScroll = Math.max(0, totalContentHeight - viewHeight);

        graphics.disableScissor();

        if (this.maxOverviewScroll > 0) {
            int scrollbarX = viewX + viewWidth - 4;
            int barHeight = Math.max(15, (int) ((float) viewHeight / totalContentHeight * viewHeight));
            int barY = viewTop + (int) ((float) this.overviewScrollY / this.maxOverviewScroll * (viewHeight - barHeight));
            graphics.fill(scrollbarX, barY, scrollbarX + 3, barY + barHeight, 0xAAFFAA00);
        }

        if (!hoveredStack.isEmpty()) {
            graphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private boolean isMouseInScissor(int mouseX, int mouseY, int x, int y, int w, int h, int clipTop, int clipBottom) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h && mouseY >= clipTop && mouseY <= clipBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. 点击左侧受击列表
        int leftX = 10;
        int listTopY = 22;
        for (int i = 0; i < records.size(); i++) {
            int itemY = listTopY + 14 + i * 20;
            if (mouseX >= leftX - 2 && mouseX <= leftX + 125 && mouseY >= itemY - 2 && mouseY <= itemY + 16) {
                this.selectedIndex = i;
                updateSelectedRecord();
                return true;
            }
        }

        // 2. 点击 Tab 标签
        int tabStartX = (currentTab == Tab.OVERVIEW) ? 250 : 145;
        int tabY = 8;
        Tab[] tabs = Tab.values();
        for (int t = 0; t < tabs.length; t++) {
            int tabX = tabStartX + t * 76;
            if (mouseX >= tabX && mouseX <= tabX + 72 && mouseY >= tabY && mouseY <= tabY + 15) {
                this.currentTab = tabs[t];
                this.overviewScrollY = 0;
                this.textScrollY = 0;
                rebuildTextContent();
                return true;
            }
        }

        // 3. 点击【复制全部】按钮
        if (this.currentTab != Tab.OVERVIEW) {
            int copyBtnX = this.width - 95;
            if (mouseX >= copyBtnX && mouseX <= copyBtnX + 80 && mouseY >= tabY && mouseY <= tabY + 15) {
                StringBuilder sb = new StringBuilder();
                for (String raw : this.rawLinesForCopy) {
                    sb.append(raw).append("\n");
                }
                Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
                this.copyNotification = "§a✓ 已成功复制到剪贴板！";
                this.copyNotificationTimer = 40; // 提示显示 2 秒
                return true;
            }
        }

        // 4. 点击返回按钮
        int btnWidth = 80;
        int btnHeight = 18;
        int btnX = this.width - btnWidth - 12;
        int btnY = this.height - btnHeight - 8;
        if (mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.currentTab == Tab.OVERVIEW) {
            if (mouseX >= 240) {
                this.overviewScrollY = Mth.clamp(this.overviewScrollY - (int) (scrollY * 16), 0, this.maxOverviewScroll);
                return true;
            }
        } else {
            if (mouseX >= 140) {
                this.textScrollY = Mth.clamp(this.textScrollY - (int) (scrollY * 16), 0, this.maxTextScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}