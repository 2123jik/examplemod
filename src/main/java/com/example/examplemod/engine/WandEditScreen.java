package com.example.examplemod.engine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class WandEditScreen extends AbstractContainerScreen<WandEditMenu> {

    private Slot dragStartSlot = null;

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(WandEditMenu.MENU_TYPE.get(), WandEditScreen::new);
    }

    public WandEditScreen(WandEditMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 174;
    }

    @Override
    protected void init() {
        super.init();
        String[] tabLabels = {"主动", "增强", "被动", "身上"};
        for (int i = 0; i < 4; i++) {
            final int tabId = i;
            addRenderableWidget(Button.builder(Component.literal(tabLabels[i]), btn -> {
                this.menu.clickMenuButton(this.minecraft.player, tabId);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tabId);
            }).bounds(this.leftPos + 8 + i * 31, this.topPos + 75, 29, 15).build());
        }

        // 一键把身上背包里的全部卷轴/词条吸进分类法术背包
        addRenderableWidget(Button.builder(Component.literal("收纳"), btn ->
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 10)
        ).bounds(this.leftPos + 136, this.topPos + 75, 32, 15).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xEC161320);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xFF7A52A8);

        // 1. 顶部法杖槽 (9格)
        int[] cap = this.menu.getCapacityStats();
        int running = 0;
        for (int i = 0; i < WandEditMenu.WAND_SLOTS; i++) {
            Slot s = this.menu.getSlot(i);
            int cost = 1;
            if (s.hasItem()) {
                var spec = TestWandItem.getGemSpec(s.getItem());
                cost = (spec != null) ? spec.slotCost() : 1;
            }
            boolean overflow = (running + cost) > cap[1];
            if (s.hasItem()) running += cost;

            int sx = x + 7 + i * 18;
            int sy = y + 19;
            g.fill(sx, sy, sx + 18, sy + 18, overflow ? 0xFF3A1818 : 0xFF28223C);
            g.renderOutline(sx, sy, 18, 18, overflow ? 0xFFAA3333 : 0xFF6B5294);
        }

        // 2. 中部 Temp 暂存区 (9格)
        for (int i = 0; i < WandEditMenu.TEMP_SLOTS; i++) {
            int sx = x + 7 + i * 18;
            int sy = y + 53;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF1F2833);
            g.renderOutline(sx, sy, 18, 18, 0xFF3D5E7A);
        }

        // 3. 底部法术背包网格 (4×9 = 36格)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18;
                int sy = y + 93 + row * 18;
                g.fill(sx, sy, sx + 18, sy + 18, 0xFF1E1C26);
                g.renderOutline(sx, sy, 18, 18, 0xFF3C384D);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "法杖编排 →", 8, 7, 0xE6D8FF, false);
        int[] cap = this.menu.getCapacityStats();
        int capColor = (cap[0] > cap[1]) ? 0xFF5555 : 0x66FFAA;
        String capText = "容量 " + cap[0] + "/" + cap[1];
        g.drawString(this.font, capText, this.imageWidth - this.font.width(capText) - 8, 7, capColor, false);

        g.drawString(this.font, "Temp 暂存区", 8, 42, 0x88BBEE, false);

        // 在当前激活标签按钮下方画一条高亮线
        int tabX = 8 + this.menu.currentTab * 31;
        g.fill(tabX, 90, tabX + 29, 92, 0xFFAA77FF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // ==================== 按住左键拖拽 → 松手即放入 / 互换 ====================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.getCarried().isEmpty()) {
            Slot hovered = this.getSlotUnderMouse();
            if (hovered != null && hovered.isActive() && hovered.hasItem() && hovered.mayPickup(this.minecraft.player)) {
                this.dragStartSlot = hovered;
            }
        } else {
            this.dragStartSlot = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.dragStartSlot != null) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragStartSlot != null) {
            Slot start = this.dragStartSlot;
            this.dragStartSlot = null;

            Slot target = this.getSlotUnderMouse();
            if (target != null && target.isActive() && target != start
                    && !this.menu.getCarried().isEmpty() && target.mayPlace(this.menu.getCarried())) {
                boolean targetWasEmpty = !target.hasItem();
                this.slotClicked(target, target.index, 0, ClickType.PICKUP);
                if (!targetWasEmpty && !this.menu.getCarried().isEmpty() && start.mayPlace(this.menu.getCarried())) {
                    this.slotClicked(start, start.index, 0, ClickType.PICKUP);
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}