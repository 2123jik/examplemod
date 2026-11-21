package com.example.examplemod.client.gui;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class PauseMenuOverlay {

    private static final DateTimeFormatter REAL_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full");

    private static final int PAUSE_CENTER_HALF_WIDTH = 102;
    private static final int CENTER_SAFE_GAP = 12;
    private static final int SCREEN_MARGIN = 6;
    private static final int CARD_PADDING = 5;
    private static final int CELL_HEIGHT = 13;
    private static final int CELL_GAP_X = 6;
    private static final int CELL_GAP_Y = 2;

    private static final List<Holder<Attribute>> CORE_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.SWEEPING_DAMAGE_RATIO,
            Attributes.MOVEMENT_SPEED,
            Attributes.SNEAKING_SPEED,
            Attributes.WATER_MOVEMENT_EFFICIENCY,
            Attributes.BLOCK_BREAK_SPEED,
            Attributes.MINING_EFFICIENCY,
            Attributes.SUBMERGED_MINING_SPEED,
            Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE,
            Attributes.SAFE_FALL_DISTANCE,
            Attributes.FALL_DAMAGE_MULTIPLIER,
            Attributes.GRAVITY,
            Attributes.STEP_HEIGHT,
            Attributes.JUMP_STRENGTH,
            Attributes.SCALE,
            Attributes.LUCK,
            Attributes.MAX_ABSORPTION,
            Attributes.OXYGEN_BONUS,
            Attributes.BURNING_TIME,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE
    );
    private static final Set<Holder<Attribute>> CORE_ATTRIBUTE_SET = Set.copyOf(CORE_ATTRIBUTES);

    private enum FilterMode {
        ALL("全部"),
        MODIFIED("已改"),
        NON_ZERO("非零"),
        CORE("核心");

        final String label;

        FilterMode(String label) {
            this.label = label;
        }
    }

    private enum SortMode {
        MODIFIED_FIRST("变动优先"),
        MODIFIER_COUNT("修饰符↓"),
        VALUE_DESC("数值↓"),
        NAME_ASC("名称 A-Z"),
        NAMESPACE("模组分组");

        final String label;

        SortMode(String label) {
            this.label = label;
        }

        SortMode next() {
            SortMode[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private static FilterMode currentFilter = FilterMode.ALL;
    private static SortMode currentSort = SortMode.MODIFIED_FIRST;
    private static int currentPage = 0;
    private static int totalPages = 1;
    private static double scrollAccum = 0.0;

    private static int panelLeft, panelTop, panelRight, panelBottom;
    private static final List<ClickZone> clickZones = new ArrayList<>();

    private PauseMenuOverlay() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen) {
            scrollAccum = 0.0;
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof PauseScreen) || event.getButton() != 0) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (ClickZone zone : clickZones) {
            if (zone.contains(mx, my)) {
                zone.action.run();
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        double mx = event.getMouseX();
        double my = event.getMouseY();
        if (mx < panelLeft || mx > panelRight || my < panelTop || my > panelBottom) return;
        if (totalPages <= 1) return;

        scrollAccum += event.getScrollDeltaY();
        if (scrollAccum >= 0.5) {
            currentPage = Math.max(0, currentPage - 1);
            scrollAccum = 0.0;
        } else if (scrollAccum <= -0.5) {
            currentPage = Math.min(totalPages - 1, currentPage + 1);
            scrollAccum = 0.0;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof PauseScreen) || totalPages <= 1) return;

        int key = event.getKeyCode();
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            currentPage = Math.max(0, currentPage - 1);
            event.setCanceled(true);
        } else if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            currentPage = Math.min(totalPages - 1, currentPage + 1);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        clickZones.clear();
        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        int leftEnd = (screen.width / 2) - PAUSE_CENTER_HALF_WIDTH - CENTER_SAFE_GAP;
        int leftWidth = leftEnd - SCREEN_MARGIN;

        int rightStart = (screen.width / 2) + PAUSE_CENTER_HALF_WIDTH + CENTER_SAFE_GAP;
        int rightWidth = (screen.width - SCREEN_MARGIN) - rightStart;

        AttributeEntry hoveredEntry = null;
        if (leftWidth >= 120) {
            hoveredEntry = renderAttributesPanel(
                    graphics, font, mc.player,
                    SCREEN_MARGIN, SCREEN_MARGIN, leftWidth, screen.height - SCREEN_MARGIN * 2,
                    mouseX, mouseY
            );
        } else {
            totalPages = 1;
            panelLeft = panelTop = panelRight = panelBottom = 0;
        }

        if (rightWidth >= 120) {
            renderBottomRightStatusPanel(
                    graphics, font, mc,
                    rightStart, rightWidth, screen.height - SCREEN_MARGIN
            );
        }

        if (hoveredEntry != null) {
            renderAttributeTooltip(graphics, font, mc, hoveredEntry, screen.width, screen.height, mouseX, mouseY);
        }
    }

    private static AttributeEntry renderAttributesPanel(
            GuiGraphics graphics, Font font, LocalPlayer player,
            int x, int y, int width, int maxHeight,
            int mouseX, int mouseY
    ) {
        Map<ModifierKey, ModifierSource> slotSourceMap = new HashMap<>();
        collectVanillaSlotSources(player, slotSourceMap);

        Map<String, ItemStack> curioItemsBySlot = new HashMap<>();
        collectCurioSources(player, slotSourceMap, curioItemsBySlot);

        Map<ResourceLocation, MobEffectInstance> activeEffectMap = collectEffectMap(player);

        List<AttributeEntry> allEntries = collectPlayerAttributes(player, slotSourceMap, curioItemsBySlot, activeEffectMap);
        int allCount = allEntries.size();
        int modifiedCount = 0;
        int nonZeroCount = 0;
        for (AttributeEntry e : allEntries) {
            if (e.isModified()) modifiedCount++;
            if (Math.abs(e.value()) >= 1e-5) nonZeroCount++;
        }

        List<AttributeEntry> filtered = new ArrayList<>();
        for (AttributeEntry e : allEntries) {
            boolean keep = switch (currentFilter) {
                case ALL -> true;
                case MODIFIED -> e.isModified();
                case NON_ZERO -> Math.abs(e.value()) >= 1e-5;
                case CORE -> CORE_ATTRIBUTE_SET.contains(e.holder());
            };
            if (keep) filtered.add(e);
        }

        sortEntries(filtered);

        int innerX = x + CARD_PADDING;
        int innerWidth = width - CARD_PADDING * 2;

        int cols = width >= 330 ? 3 : (width >= 195 ? 2 : 1);
        int cellWidth = (innerWidth - (cols - 1) * CELL_GAP_X) / cols;

        int topBarHeight = 14 + 14 + 14 + 5;
        int bottomBarHeight = 16;
        int gridAvailableHeight = Math.max(CELL_HEIGHT, maxHeight - CARD_PADDING * 2 - topBarHeight - bottomBarHeight);

        int rowsPerPage = Math.max(1, (gridAvailableHeight + CELL_GAP_Y) / (CELL_HEIGHT + CELL_GAP_Y));
        int itemsPerPage = rowsPerPage * cols;
        totalPages = Math.max(1, (filtered.size() + itemsPerPage - 1) / itemsPerPage);
        currentPage = Mth.clamp(currentPage, 0, totalPages - 1);

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(filtered.size(), startIdx + itemsPerPage);
        int currentItemCount = Math.max(0, endIdx - startIdx);
        int actualRows = currentItemCount == 0 ? 1 : (currentItemCount + cols - 1) / cols;
        int gridHeight = actualRows * CELL_HEIGHT + Math.max(0, actualRows - 1) * CELL_GAP_Y;

        int panelHeight = CARD_PADDING * 2 + topBarHeight + gridHeight + bottomBarHeight;
        panelLeft = x;
        panelTop = y;
        panelRight = x + width;
        panelBottom = y + panelHeight;

        drawCardBackground(graphics, x, y, width, panelHeight, 0xCC141418, 0x5560A5FA);

        int curY = y + CARD_PADDING;

        Component title = Component.literal("属性 (" + filtered.size() + ")")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        graphics.drawString(font, title, innerX, curY + 1, 0xFFFFFF, true);
        renderVanillaVitalBadges(graphics, font, player, innerX + font.width(title) + 6, curY, innerX + innerWidth);
        curY += 14;

        int tabX = innerX;
        for (FilterMode mode : FilterMode.values()) {
            int cnt = switch (mode) {
                case ALL -> allCount;
                case MODIFIED -> modifiedCount;
                case NON_ZERO -> nonZeroCount;
                case CORE -> CORE_ATTRIBUTES.size();
            };
            String label = mode.label + " " + cnt;
            int btnW = font.width(label) + 8;
            if (tabX + btnW > innerX + innerWidth) break;

            boolean active = (currentFilter == mode);
            boolean hover = mouseX >= tabX && mouseX < tabX + btnW && mouseY >= curY && mouseY < curY + 12;
            drawPillButton(graphics, font, tabX, curY, btnW, 12, label, active, hover);

            final FilterMode targetMode = mode;
            clickZones.add(new ClickZone(tabX, curY, btnW, 12, () -> {
                currentFilter = targetMode;
                currentPage = 0;
            }));
            tabX += btnW + 3;
        }
        curY += 14;

        String sortLabel = "排序: " + currentSort.label + " ▾";
        int sortW = Math.min(innerWidth, font.width(sortLabel) + 10);
        boolean sortHover = mouseX >= innerX && mouseX < innerX + sortW && mouseY >= curY && mouseY < curY + 12;
        drawPillButton(graphics, font, innerX, curY, sortW, 12, sortLabel, false, sortHover);
        clickZones.add(new ClickZone(innerX, curY, sortW, 12, () -> {
            currentSort = currentSort.next();
            currentPage = 0;
        }));
        curY += 14;

        graphics.fill(innerX, curY, innerX + innerWidth, curY + 1, 0x33FFFFFF);
        curY += 4;

        AttributeEntry hoveredEntry = null;
        if (filtered.isEmpty()) {
            graphics.drawString(font, Component.literal("当前筛选条件下无属性").withStyle(ChatFormatting.GRAY),
                    innerX + 4, curY + 2, 0xFFFFFF, false);
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                int localIdx = i - startIdx;
                int row = localIdx / cols;
                int col = localIdx % cols;

                int cx = innerX + col * (cellWidth + CELL_GAP_X);
                int cy = curY + row * (CELL_HEIGHT + CELL_GAP_Y);

                AttributeEntry entry = filtered.get(i);
                boolean hovered = mouseX >= cx && mouseX < cx + cellWidth && mouseY >= cy && mouseY < cy + CELL_HEIGHT;
                if (hovered) hoveredEntry = entry;

                renderAttributeCell(graphics, font, entry, cx, cy, cellWidth, CELL_HEIGHT, hovered);
            }
        }

        int footerY = y + panelHeight - CARD_PADDING - 12;
        renderPaginationBar(graphics, font, innerX, footerY, innerWidth, mouseX, mouseY);

        return hoveredEntry;
    }

    private static void renderVanillaVitalBadges(GuiGraphics graphics, Font font, LocalPlayer player,
                                                 int minX, int y, int maxX) {
        String hpText = String.format(Locale.ROOT, "%.0f/%.0f", player.getHealth(), player.getMaxHealth());
        String armorText = String.valueOf(player.getArmorValue());
        String foodText = String.valueOf(player.getFoodData().getFoodLevel());

        int wHp = 11 + font.width(hpText);
        int wArmor = 11 + font.width(armorText);
        int wFood = 11 + font.width(foodText);
        int gap = 6;

        int totalW = wHp + gap + wArmor + gap + wFood;
        int drawX = maxX - totalW;
        if (drawX < minX) {
            totalW = wHp + gap + wArmor;
            drawX = maxX - totalW;
            if (drawX < minX) return;
        }

        graphics.blitSprite(HEART_CONTAINER_SPRITE, drawX, y + 1, 9, 9);
        if (player.getHealth() > 0) {
            ResourceLocation heart = player.getHealth() < player.getMaxHealth() * 0.25f ? HEART_HALF_SPRITE : HEART_FULL_SPRITE;
            graphics.blitSprite(heart, drawX, y + 1, 9, 9);
        }
        graphics.drawString(font, hpText, drawX + 11, y + 2, 0xFFFF6B6B, true);
        drawX += wHp + gap;

        ResourceLocation armorSprite = player.getArmorValue() > 0 ? ARMOR_FULL_SPRITE : ARMOR_EMPTY_SPRITE;
        graphics.blitSprite(armorSprite, drawX, y + 1, 9, 9);
        graphics.drawString(font, armorText, drawX + 11, y + 2,
                player.getArmorValue() > 0 ? 0xFF7DD3FC : 0xFF9CA3AF, true);
        drawX += wArmor + gap;

        if (drawX + wFood <= maxX) {
            graphics.blitSprite(FOOD_EMPTY_SPRITE, drawX, y + 1, 9, 9);
            if (player.getFoodData().getFoodLevel() > 0) {
                graphics.blitSprite(FOOD_FULL_SPRITE, drawX, y + 1, 9, 9);
            }
            graphics.drawString(font, foodText, drawX + 11, y + 2, 0xFFFBBF24, true);
        }
    }

    private static void renderAttributeCell(GuiGraphics graphics, Font font, AttributeEntry entry,
                                            int x, int y, int width, int height, boolean hovered) {
        int bg = hovered ? 0x44FFFFFF : (entry.isModified() ? 0x22FFFFFF : 0x14000000);
        graphics.fill(x, y, x + width, y + height, bg);
        if (entry.isModified()) {
            graphics.fill(x, y, x + 2, y + height, getAccentArgb(entry));
        }
        if (hovered) {
            graphics.renderOutline(x, y, width, height, 0xAAFFFFFF);
        }

        int padLeft = entry.isModified() ? 4 : 3;
        int rightEdge = x + width - 4;

        String valStr = formatNumber(entry.value());
        Component valComp = Component.literal(valStr).withStyle(getValueStyle(entry));
        int valW = font.width(valComp);
        graphics.drawString(font, valComp, rightEdge - valW, y + 2, 0xFFFFFF, true);

        int nameMaxW = Math.max(16, rightEdge - valW - 6 - (x + padLeft));
        Component nameComp = Component.translatable(entry.attribute().getDescriptionId())
                .withStyle(entry.isModified() ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        graphics.drawString(font, fitText(font, nameComp, nameMaxW), x + padLeft, y + 2, 0xFFFFFF, true);
    }

    private static void renderPaginationBar(GuiGraphics graphics, Font font,
                                            int x, int y, int width, int mouseX, int mouseY) {
        int btnW = 46;
        int btnH = 12;

        boolean canPrev = currentPage > 0;
        boolean hoverPrev = canPrev && mouseX >= x && mouseX < x + btnW && mouseY >= y && mouseY < y + btnH;
        drawPillButton(graphics, font, x, y, btnW, btnH, "◀ 上一页", false, hoverPrev, canPrev);
        if (canPrev) {
            clickZones.add(new ClickZone(x, y, btnW, btnH, () -> currentPage = Math.max(0, currentPage - 1)));
        }

        int nextX = x + width - btnW;
        boolean canNext = currentPage < totalPages - 1;
        boolean hoverNext = canNext && mouseX >= nextX && mouseX < nextX + btnW && mouseY >= y && mouseY < y + btnH;
        drawPillButton(graphics, font, nextX, y, btnW, btnH, "下一页 ▶", false, hoverNext, canNext);
        if (canNext) {
            clickZones.add(new ClickZone(nextX, y, btnW, btnH, () -> currentPage = Math.min(totalPages - 1, currentPage + 1)));
        }

        String pageStr = String.format("%d / %d 页", currentPage + 1, totalPages);
        int pw = font.width(pageStr);
        graphics.drawString(font, pageStr, x + (width - pw) / 2, y + 2, 0xFFD4D4D8, false);
    }

    private static void renderAttributeTooltip(GuiGraphics graphics, Font font, Minecraft mc, AttributeEntry entry,
                                               int screenWidth, int screenHeight, int mouseX, int mouseY) {
        boolean detailMode = Screen.hasShiftDown();
        int maxBoxWidth = Math.min(detailMode ? 310 : 250, screenWidth - 16);
        int innerMaxW = maxBoxWidth - 12;
        Attribute attr = entry.attribute();

        double sumAdd = 0.0;
        double sumMultBase = 0.0;
        double prodMultTotal = 1.0;
        boolean hasAdd = false, hasMultBase = false, hasMultTotal = false;

        for (ResolvedModifier rm : entry.modifiers()) {
            AttributeModifier mod = rm.modifier();
            switch (mod.operation()) {
                case ADD_VALUE -> {
                    hasAdd = true;
                    sumAdd += mod.amount();
                }
                case ADD_MULTIPLIED_BASE -> {
                    hasMultBase = true;
                    sumMultBase += mod.amount();
                }
                case ADD_MULTIPLIED_TOTAL -> {
                    hasMultTotal = true;
                    prodMultTotal *= (1.0 + mod.amount());
                }
            }
        }

        double stage3Raw = (entry.baseValue() + sumAdd) * (1.0 + sumMultBase) * prodMultTotal;
        List<TooltipRow> rows = new ArrayList<>();

        Component titleComp = Component.translatable(attr.getDescriptionId())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        Component finalValComp = Component.literal(formatNumber(entry.value()))
                .withStyle(getValueStyle(entry), ChatFormatting.BOLD);
        rows.add(TooltipRow.header(titleComp, finalValComp));

        MutableComponent formula = Component.literal("基础 " + formatNumber(entry.baseValue()))
                .withStyle(ChatFormatting.GRAY);
        if (hasAdd) {
            formula.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("加值 " + formatSigned(sumAdd))
                            .withStyle(readableStyle(attr, sumAdd >= 0)));
        }
        if (hasMultBase) {
            formula.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("基倍 " + formatPercentSigned(sumMultBase))
                            .withStyle(readableStyle(attr, sumMultBase >= 0)));
        }
        if (hasMultTotal) {
            formula.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("总乘 ×" + formatMultiplier(prodMultTotal))
                            .withStyle(readableStyle(attr, prodMultTotal >= 1.0)));
        }
        rows.add(TooltipRow.text(fitText(font, formula, innerMaxW)));

        if (Math.abs(stage3Raw - entry.value()) > 1e-3 && !entry.modifiers().isEmpty()) {
            rows.add(TooltipRow.text(fitText(font,
                    Component.literal("受属性上下限限制: 原计算 " + formatNumber(stage3Raw) + " → " + formatNumber(entry.value()))
                            .withStyle(ChatFormatting.YELLOW), innerMaxW)));
        }

        if (!entry.modifiers().isEmpty()) {
            rows.add(TooltipRow.divider());

            if (!detailMode) {
                for (SourceSummary g : groupBySource(entry.modifiers())) {
                    Component left = Component.literal(g.source().compactLabel()).withStyle(ChatFormatting.WHITE);
                    Component right = g.buildRightSummary(attr);
                    rows.add(TooltipRow.sourceRow(g.source(), left, right, null));
                }
                rows.add(TooltipRow.text(Component.literal("按住 Shift 查看单条修饰符与 ID")
                        .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()));
            } else {
                if (entry.id() != null) {
                    rows.add(TooltipRow.text(fitText(font,
                            Component.literal("属性ID: " + entry.id()).withStyle(ChatFormatting.DARK_GRAY), innerMaxW)));
                }
                for (ResolvedModifier rm : entry.modifiers()) {
                    AttributeModifier mod = rm.modifier();
                    ModifierSource src = rm.source();

                    String opTag = switch (mod.operation()) {
                        case ADD_VALUE -> "加值";
                        case ADD_MULTIPLIED_BASE -> "基倍";
                        case ADD_MULTIPLIED_TOTAL -> "总乘";
                    };
                    ChatFormatting color = Math.abs(mod.amount()) < 1e-6
                            ? ChatFormatting.GRAY
                            : readableStyle(attr, mod.amount() >= 0);

                    Component left = Component.literal(src.compactLabel()).withStyle(ChatFormatting.WHITE);
                    Component right = Component.literal(opTag + " " + formatShortOpValue(mod)).withStyle(color);
                    Component sub = Component.literal(shortenResourceLocation(mod.id())).withStyle(ChatFormatting.DARK_GRAY);

                    rows.add(TooltipRow.sourceRow(src, left, right, sub));
                }
            }
        }

        int contentW = 0;
        int contentH = 0;
        for (TooltipRow r : rows) {
            contentW = Math.max(contentW, r.measureWidth(font));
            contentH += r.height(font);
        }
        int boxW = Math.min(maxBoxWidth, Math.max(135, contentW + 12));
        int boxH = contentH + 10;

        int tx = mouseX + 12;
        if (tx + boxW > screenWidth - 6) tx = Math.max(6, mouseX - boxW - 12);
        int ty = mouseY - 12;
        if (ty + boxH > screenHeight - 6) ty = Math.max(6, screenHeight - boxH - 6);
        if (ty < 6) ty = 6;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        graphics.fill(tx - 1, ty - 1, tx + boxW + 1, ty + boxH + 1, 0xF2121218);
        graphics.renderOutline(tx, ty, boxW, boxH, 0xBB60A5FA);

        int drawX = tx + 6;
        int drawY = ty + 5;
        int rowInnerW = boxW - 12;
        for (TooltipRow r : rows) {
            r.render(graphics, font, mc, drawX, drawY, rowInnerW);
            drawY += r.height(font);
        }

        graphics.pose().popPose();
    }

    private static List<SourceSummary> groupBySource(List<ResolvedModifier> modifiers) {
        Map<String, SourceSummary> map = new LinkedHashMap<>();
        for (ResolvedModifier rm : modifiers) {
            ModifierSource src = rm.source();
            map.computeIfAbsent(src.groupKey(), k -> new SourceSummary(src)).accept(rm.modifier());
        }
        return List.copyOf(map.values());
    }

    private static void renderBottomRightStatusPanel(GuiGraphics graphics, Font font, Minecraft mc,
                                                     int x, int width, int bottomY) {
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        List<Component> lines = new ArrayList<>();
        BlockPos pos = player.blockPosition();

        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMem = Math.max(1L, rt.maxMemory() / (1024 * 1024));
        int memPct = (int) Math.min(100, (usedMem * 100L) / maxMem);

        MutableComponent perfLine = Component.literal(mc.getFps() + " FPS").withStyle(
                mc.getFps() >= 60 ? ChatFormatting.GREEN : (mc.getFps() >= 30 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        if (mc.getSingleplayerServer() != null) {
            float mspt = mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
            perfLine.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.format(Locale.ROOT, "%.1fms", mspt))
                            .withStyle(mspt <= 40 ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null) {
                int ping = info.getLatency();
                perfLine.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(ping + "ms")
                                .withStyle(ping <= 80 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            }
        }
        perfLine.append(Component.literal(" · 内存 " + memPct + "%")
                .withStyle(memPct < 80 ? ChatFormatting.GRAY : ChatFormatting.RED));
        lines.add(perfLine);

        lines.add(Component.literal(String.format("%d, %d, %d · %s · %s",
                        pos.getX(), pos.getY(), pos.getZ(),
                        getHumanDirection(player.getDirection()),
                        level.dimension().location().getPath()))
                .withStyle(ChatFormatting.WHITE));

        Holder<Biome> biome = level.getBiome(pos);
        Component biomeName = biome.unwrapKey()
                .map(k -> (Component) Component.translatable(Util.makeDescriptionId("biome", k.location())))
                .orElse(Component.literal("未知群系"));
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        lines.add(biomeName.copy().withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" · 光照 " + blockLight + (blockLight == 0 ? " (可刷怪)" : " (安全)"))
                        .withStyle(blockLight == 0 ? ChatFormatting.RED : ChatFormatting.GRAY)));

        lines.add(getCompactTimeWeatherMoon(level));

        Component lookAt = getLookingAtDescription(mc, level);
        if (lookAt != null) lines.add(lookAt);

        lines.add(Component.literal("现实 " + LocalTime.now().format(REAL_TIME_FMT))
                .withStyle(ChatFormatting.DARK_GRAY));

        int lineH = font.lineHeight + 2;
        int panelH = CARD_PADDING * 2 + lines.size() * lineH - 2;
        int topY = Math.max(SCREEN_MARGIN, bottomY - panelH);

        drawCardBackground(graphics, x, topY, width, panelH, 0xCC141418, 0x35FFFFFF);

        int innerW = width - CARD_PADDING * 2;
        int cy = topY + CARD_PADDING;
        for (Component line : lines) {
            graphics.drawString(font, fitText(font, line, innerW), x + CARD_PADDING, cy, 0xFFFFFF, true);
            cy += lineH;
        }
    }

    private static void collectVanillaSlotSources(LocalPlayer player, Map<ModifierKey, ModifierSource> map) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.BODY) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            stack.forEachModifier(slot, (attr, mod) ->
                    map.putIfAbsent(new ModifierKey(attr, mod.id()), ModifierSource.fromEquipment(slot, stack))
            );
            EnchantmentHelper.forEachModifier(stack, slot, (attr, mod) ->
                    map.putIfAbsent(new ModifierKey(attr, mod.id()), ModifierSource.fromEquipment(slot, stack))
            );
        }
    }

    private static void collectCurioSources(LocalPlayer player,
                                            Map<ModifierKey, ModifierSource> map,
                                            Map<String, ItemStack> curioItemsBySlot) {
        if (!ModList.get().isLoaded("curios")) return;
        CuriosReflect.init();
        if (!CuriosReflect.available) return;

        try {
            Object optHandler = CuriosReflect.getCuriosInventory.invoke(null, player);
            if (!(optHandler instanceof Optional<?> opt) || opt.isEmpty()) return;
            Object handler = opt.get();

            Predicate<ItemStack> nonEmpty = stack -> !stack.isEmpty();
            Object listObj = CuriosReflect.findCurios.invoke(handler, nonEmpty);
            if (!(listObj instanceof List<?> results)) return;

            for (Object slotResult : results) {
                if (slotResult == null) continue;
                Object slotContext = CuriosReflect.slotResultContext.invoke(slotResult);
                Object stackObj = CuriosReflect.slotResultStack.invoke(slotResult);
                if (!(stackObj instanceof ItemStack stack) || stack.isEmpty() || slotContext == null) continue;

                String identifier = (String) CuriosReflect.slotContextIdentifier.invoke(slotContext);
                int index = (int) CuriosReflect.slotContextIndex.invoke(slotContext);
                ResourceLocation slotId = (ResourceLocation) CuriosReflect.getSlotId.invoke(null, slotContext);

                curioItemsBySlot.putIfAbsent(identifier + "_" + index, stack);
                curioItemsBySlot.putIfAbsent(identifier, stack);

                Object mmObj = CuriosReflect.getAttributeModifiers.invoke(null, slotContext, slotId, stack);
                if (mmObj instanceof Multimap<?, ?> multimap) {
                    for (Map.Entry<?, ?> e : multimap.entries()) {
                        if (e.getKey() instanceof Holder<?> rawHolder && e.getValue() instanceof AttributeModifier mod) {
                            @SuppressWarnings("unchecked")
                            Holder<Attribute> attrHolder = (Holder<Attribute>) rawHolder;
                            map.putIfAbsent(
                                    new ModifierKey(attrHolder, mod.id()),
                                    ModifierSource.fromCurio(identifier, index, stack)
                            );
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Map<ResourceLocation, MobEffectInstance> collectEffectMap(LocalPlayer player) {
        Map<ResourceLocation, MobEffectInstance> map = new HashMap<>();
        for (MobEffectInstance eff : player.getActiveEffects()) {
            ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(eff.getEffect().value());
            if (key != null) map.put(key, eff);
        }
        return map;
    }

    private static List<AttributeEntry> collectPlayerAttributes(
            LocalPlayer player,
            Map<ModifierKey, ModifierSource> slotSources,
            Map<String, ItemStack> curioItemsBySlot,
            Map<ResourceLocation, MobEffectInstance> activeEffects
    ) {
        Set<Holder<Attribute>> orderedHolders = new LinkedHashSet<>(CORE_ATTRIBUTES);
        BuiltInRegistries.ATTRIBUTE.holders().forEach(orderedHolders::add);

        List<AttributeEntry> result = new ArrayList<>();
        for (Holder<Attribute> holder : orderedHolders) {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) continue;

            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(holder.value());
            List<ResolvedModifier> resolved = new ArrayList<>();

            for (AttributeModifier mod : instance.getModifiers()) {
                ModifierSource source = resolveModifierSource(player, holder, mod, slotSources, curioItemsBySlot, activeEffects);
                resolved.add(new ResolvedModifier(mod, source));
            }

            resolved.sort(Comparator
                    .comparingInt((ResolvedModifier r) -> r.source().priority())
                    .thenComparingInt(r -> r.modifier().operation().id()));

            result.add(new AttributeEntry(
                    holder,
                    holder.value(),
                    attrId,
                    instance.getBaseValue(),
                    instance.getValue(),
                    resolved
            ));
        }
        return result;
    }

    private static ModifierSource resolveModifierSource(
            LocalPlayer player,
            Holder<Attribute> attr,
            AttributeModifier mod,
            Map<ModifierKey, ModifierSource> slotSources,
            Map<String, ItemStack> curioItemsBySlot,
            Map<ResourceLocation, MobEffectInstance> activeEffects
    ) {
        ModifierSource direct = slotSources.get(new ModifierKey(attr, mod.id()));
        if (direct != null) return direct;

        String path = mod.id().getPath();
        if (path.startsWith("effect.")) {
            String effectName = path.substring("effect.".length());
            ResourceLocation guessId = ResourceLocation.fromNamespaceAndPath(mod.id().getNamespace(), effectName);
            MobEffectInstance eff = activeEffects.get(guessId);
            if (eff != null) return ModifierSource.fromEffect(eff);
        }

        EquipmentSlot guessedSlot = guessSlotFromModifierId(path);
        if (guessedSlot != null) {
            ItemStack equipped = player.getItemBySlot(guessedSlot);
            if (!equipped.isEmpty()) return ModifierSource.fromEquipment(guessedSlot, equipped);
        }

        if (!curioItemsBySlot.isEmpty()) {
            String lower = path.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, ItemStack> entry : curioItemsBySlot.entrySet()) {
                if (lower.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                    String slotId = entry.getKey().contains("_")
                            ? entry.getKey().substring(0, entry.getKey().lastIndexOf('_'))
                            : entry.getKey();
                    return ModifierSource.fromCurio(slotId, 0, entry.getValue());
                }
            }
        }

        if ("minecraft".equals(mod.id().getNamespace())) {
            return ModifierSource.other("原版机制");
        }
        return ModifierSource.other("模组 " + mod.id().getNamespace());
    }

    private static EquipmentSlot guessSlotFromModifierId(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("mainhand")) return EquipmentSlot.MAINHAND;
        if (lower.contains("offhand")) return EquipmentSlot.OFFHAND;
        if (lower.contains("helmet") || lower.contains("_head")) return EquipmentSlot.HEAD;
        if (lower.contains("chestplate") || lower.contains("_chest")) return EquipmentSlot.CHEST;
        if (lower.contains("leggings") || lower.contains("_legs")) return EquipmentSlot.LEGS;
        if (lower.contains("boots") || lower.contains("_feet")) return EquipmentSlot.FEET;
        return null;
    }

    private static void sortEntries(List<AttributeEntry> list) {
        Comparator<AttributeEntry> comparator = switch (currentSort) {
            case MODIFIED_FIRST -> Comparator
                    .comparing((AttributeEntry e) -> !e.isModified())
                    .thenComparingInt(e -> -e.modifiers().size())
                    .thenComparing(e -> !CORE_ATTRIBUTE_SET.contains(e.holder()))
                    .thenComparing(e -> Math.abs(e.value()) < 1e-5)
                    .thenComparing(e -> Component.translatable(e.attribute().getDescriptionId()).getString());
            case MODIFIER_COUNT -> Comparator
                    .comparingInt((AttributeEntry e) -> -e.modifiers().size())
                    .thenComparing(e -> !e.isModified())
                    .thenComparing(e -> Component.translatable(e.attribute().getDescriptionId()).getString());
            case VALUE_DESC -> Comparator
                    .comparingDouble((AttributeEntry e) -> -Math.abs(e.value()))
                    .thenComparing(e -> Component.translatable(e.attribute().getDescriptionId()).getString());
            case NAME_ASC -> Comparator
                    .comparing(e -> Component.translatable(e.attribute().getDescriptionId()).getString());
            case NAMESPACE -> Comparator
                    .comparing((AttributeEntry e) -> e.id() != null ? e.id().getNamespace() : "zzz")
                    .thenComparing(e -> !e.isModified())
                    .thenComparing(e -> Component.translatable(e.attribute().getDescriptionId()).getString());
        };
        list.sort(comparator);
    }

    private static void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        float scale = size / 16.0f;
        graphics.pose().scale(scale, scale, scale);
        graphics.renderFakeItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private static void drawPillButton(GuiGraphics graphics, Font font, int x, int y, int w, int h,
                                       String text, boolean active, boolean hovered) {
        drawPillButton(graphics, font, x, y, w, h, text, active, hovered, true);
    }

    private static void drawPillButton(GuiGraphics graphics, Font font, int x, int y, int w, int h,
                                       String text, boolean active, boolean hovered, boolean enabled) {
        int bg = !enabled ? 0x22000000 : (active ? 0x663B82F6 : (hovered ? 0x44FFFFFF : 0x28FFFFFF));
        int border = !enabled ? 0x22FFFFFF : (active ? 0xCC60A5FA : (hovered ? 0xAAFFFFFF : 0x33FFFFFF));
        int textColor = !enabled ? 0xFF6B7280 : (active ? 0xFFFFFFFF : 0xFFE4E4E7);

        graphics.fill(x, y, x + w, y + h, bg);
        graphics.renderOutline(x, y, w, h, border);
        int tw = font.width(text);
        graphics.drawString(font, text, x + Math.max(2, (w - tw) / 2), y + (h - font.lineHeight) / 2 + 1, textColor, false);
    }

    private static void drawCardBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                           int bgColor, int topAccentColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + width, y + 1, topAccentColor);
    }

    private static ChatFormatting readableStyle(Attribute attr, boolean positiveDelta) {
        ChatFormatting vanilla = attr.getStyle(positiveDelta);
        if (vanilla == ChatFormatting.BLUE) return ChatFormatting.GREEN;
        if (vanilla == ChatFormatting.RED) return ChatFormatting.RED;
        return ChatFormatting.AQUA;
    }

    private static ChatFormatting getValueStyle(AttributeEntry entry) {
        double diff = entry.value() - entry.baseValue();
        if (Math.abs(diff) < 1e-5) return ChatFormatting.GRAY;
        return readableStyle(entry.attribute(), diff > 0);
    }

    private static int getAccentArgb(AttributeEntry entry) {
        Integer rgb = getValueStyle(entry).getColor();
        return rgb != null ? (0xFF000000 | rgb) : 0xFF55FF55;
    }

    private static String formatShortOpValue(AttributeModifier mod) {
        double amount = mod.amount();
        return switch (mod.operation()) {
            case ADD_VALUE -> formatSigned(amount);
            case ADD_MULTIPLIED_BASE -> formatPercentSigned(amount);
            case ADD_MULTIPLIED_TOTAL -> "×" + formatMultiplier(1.0 + amount);
        };
    }

    private static String formatSigned(double value) {
        return (value >= 0 ? "+" : "") + formatNumber(value);
    }

    private static String formatPercentSigned(double ratio) {
        double pct = ratio * 100.0;
        String sign = pct >= 0 ? "+" : "";
        if (Math.abs(pct - Math.rint(pct)) < 1e-3) {
            return String.format(Locale.ROOT, "%s%.0f%%", sign, pct);
        }
        return String.format(Locale.ROOT, "%s%.1f%%", sign, pct);
    }

    private static String formatMultiplier(double mult) {
        if (Math.abs(mult * 100 - Math.rint(mult * 100)) < 1e-3) {
            return String.format(Locale.ROOT, "%.2f", mult);
        }
        return String.format(Locale.ROOT, "%.3f", mult);
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-4) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (Math.abs(value * 10 - Math.rint(value * 10)) < 1e-3) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String shortenResourceLocation(ResourceLocation id) {
        String full = id.toString();
        if (full.length() <= 46) return full;
        return id.getNamespace() + ":..." + full.substring(full.length() - 28);
    }

    private static Component getCompactTimeWeatherMoon(Level level) {
        long dayTime = level.getDayTime();
        long day = Math.floorDiv(dayTime, 24000L) + 1;
        long tick = Math.floorMod(dayTime, 24000L);

        String period;
        if (tick < 1000) period = "黎明";
        else if (tick < 6000) period = "上午";
        else if (tick < 9000) period = "正午";
        else if (tick < 12542) period = level.isRaining() ? "黄昏(可睡)" : "傍晚";
        else if (tick < 23460) period = "夜晚(可睡)";
        else period = "清晨";

        String weather = level.isThundering() ? "雷暴" : (level.isRaining() ? "雨雪" : "晴");
        ChatFormatting wColor = level.isThundering() ? ChatFormatting.RED
                : (level.isRaining() ? ChatFormatting.BLUE : ChatFormatting.GREEN);

        String moon = switch (level.getMoonPhase()) {
            case 0 -> "满月";
            case 4 -> "新月";
            default -> "常规月";
        };

        return Component.literal("第" + day + "天 " + period + " · ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(weather).withStyle(wColor))
                .append(Component.literal(" · " + moon).withStyle(ChatFormatting.GRAY));
    }

    private static String getHumanDirection(Direction d) {
        return switch (d) {
            case NORTH -> "北(-Z)";
            case SOUTH -> "南(+Z)";
            case WEST -> "西(-X)";
            case EAST -> "东(+X)";
            default -> d.getName();
        };
    }

    private static Component getLookingAtDescription(Minecraft mc, Level level) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return null;

        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return Component.literal("注视: ").withStyle(ChatFormatting.YELLOW)
                    .append(level.getBlockState(bhr.getBlockPos()).getBlock().getName().withStyle(ChatFormatting.WHITE));
        }
        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof LivingEntity living) {
                return Component.literal("目标: ").withStyle(ChatFormatting.RED)
                        .append(e.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(String.format(Locale.ROOT, " (%.1f/%.1f)",
                                living.getHealth(), living.getMaxHealth())).withStyle(ChatFormatting.RED));
            }
            return Component.literal("目标: ").withStyle(ChatFormatting.RED)
                    .append(e.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        }
        return null;
    }

    private static String slotDisplayName(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "主手";
            case OFFHAND -> "副手";
            case HEAD -> "头部";
            case CHEST -> "胸部";
            case LEGS -> "腿部";
            case FEET -> "脚部";
            case BODY -> "身躯";
        };
    }

    private static String curioSlotDisplayName(String identifier) {
        String langKey = "curios.identifier." + identifier;
        return I18n.exists(langKey) ? I18n.get(langKey) : identifier;
    }

    private static FormattedCharSequence fitText(Font font, Component text, int maxWidth) {
        if (maxWidth <= 0) return FormattedCharSequence.EMPTY;
        if (font.width(text) <= maxWidth) return text.getVisualOrderText();

        int ellipsisWidth = font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return Language.getInstance().getVisualOrder(font.substrByWidth(text, maxWidth));
        }
        FormattedText clipped = font.substrByWidth(text, maxWidth - ellipsisWidth);
        return Language.getInstance().getVisualOrder(
                FormattedText.composite(clipped, Component.literal("...").withStyle(ChatFormatting.GRAY))
        );
    }

    private static final class CuriosReflect {
        static boolean initialized = false;
        static boolean available = false;

        static Method getCuriosInventory;
        static Method findCurios;
        static Method slotResultContext;
        static Method slotResultStack;
        static Method slotContextIdentifier;
        static Method slotContextIndex;
        static Method getSlotId;
        static Method getAttributeModifiers;

        static void init() {
            if (initialized) return;
            initialized = true;
            try {
                Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> iHandler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                Class<?> slotResult = Class.forName("top.theillusivec4.curios.api.SlotResult");
                Class<?> slotContext = Class.forName("top.theillusivec4.curios.api.SlotContext");

                getCuriosInventory = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
                findCurios = iHandler.getMethod("findCurios", Predicate.class);
                slotResultContext = findMethod(slotResult, "slotContext", "getSlotContext");
                slotResultStack = findMethod(slotResult, "stack", "getStack");
                slotContextIdentifier = findMethod(slotContext, "identifier", "getIdentifier");
                slotContextIndex = findMethod(slotContext, "index", "getIndex");
                getSlotId = curiosApi.getMethod("getSlotId", slotContext);
                getAttributeModifiers = curiosApi.getMethod("getAttributeModifiers", slotContext, ResourceLocation.class, ItemStack.class);

                available = true;
            } catch (Throwable ignored) {
                available = false;
            }
        }

        private static Method findMethod(Class<?> clazz, String primary, String fallback) throws NoSuchMethodException {
            try {
                return clazz.getMethod(primary);
            } catch (NoSuchMethodException e) {
                return clazz.getMethod(fallback);
            }
        }
    }

    private record ClickZone(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record ModifierKey(Holder<Attribute> attribute, ResourceLocation modifierId) {}

    private record ModifierSource(
            String groupKey,
            String slotLabel,
            ItemStack stack,
            MobEffectInstance effect,
            String fallbackLabel,
            int priority
    ) {
        static ModifierSource fromEquipment(EquipmentSlot slot, ItemStack stack) {
            return new ModifierSource("eq:" + slot.getName(), slotDisplayName(slot), stack, null, "", 0);
        }

        static ModifierSource fromCurio(String curioIdentifier, int index, ItemStack stack) {
            String slotName = curioSlotDisplayName(curioIdentifier);
            String label = index > 0 ? (slotName + "#" + (index + 1)) : slotName;
            return new ModifierSource("curio:" + curioIdentifier + ":" + index, label, stack, null, "", 0);
        }

        static ModifierSource fromEffect(MobEffectInstance effect) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            return new ModifierSource("eff:" + id, "效果", ItemStack.EMPTY, effect, "", 1);
        }

        static ModifierSource other(String label) {
            return new ModifierSource("other:" + label, "", ItemStack.EMPTY, null, label, 2);
        }

        String compactLabel() {
            if (!stack.isEmpty()) return slotLabel + " · " + stack.getHoverName().getString();
            if (effect != null) {
                String lvl = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
                return "效果 · " + effect.getEffect().value().getDisplayName().getString() + lvl;
            }
            return fallbackLabel;
        }
    }

    private record ResolvedModifier(AttributeModifier modifier, ModifierSource source) {}

    private static final class SourceSummary {
        private final ModifierSource source;
        private boolean hasAdd, hasMultBase, hasMultTotal;
        private double sumAdd = 0.0;
        private double sumMultBase = 0.0;
        private double prodMultTotal = 1.0;

        SourceSummary(ModifierSource source) {
            this.source = source;
        }

        void accept(AttributeModifier mod) {
            switch (mod.operation()) {
                case ADD_VALUE -> {
                    hasAdd = true;
                    sumAdd += mod.amount();
                }
                case ADD_MULTIPLIED_BASE -> {
                    hasMultBase = true;
                    sumMultBase += mod.amount();
                }
                case ADD_MULTIPLIED_TOTAL -> {
                    hasMultTotal = true;
                    prodMultTotal *= (1.0 + mod.amount());
                }
            }
        }

        ModifierSource source() {
            return source;
        }

        Component buildRightSummary(Attribute attr) {
            MutableComponent comp = Component.empty();
            boolean needSpace = false;

            if (hasAdd && Math.abs(sumAdd) >= 1e-6) {
                comp.append(Component.literal(formatSigned(sumAdd) + "加")
                        .withStyle(readableStyle(attr, sumAdd >= 0)));
                needSpace = true;
            }
            if (hasMultBase && Math.abs(sumMultBase) >= 1e-6) {
                if (needSpace) comp.append(" ");
                comp.append(Component.literal(formatPercentSigned(sumMultBase) + "基")
                        .withStyle(readableStyle(attr, sumMultBase >= 0)));
                needSpace = true;
            }
            if (hasMultTotal && Math.abs(prodMultTotal - 1.0) >= 1e-6) {
                if (needSpace) comp.append(" ");
                comp.append(Component.literal("×" + formatMultiplier(prodMultTotal) + "总")
                        .withStyle(readableStyle(attr, prodMultTotal >= 1.0)));
                needSpace = true;
            }
            if (!needSpace) {
                comp.append(Component.literal("+0").withStyle(ChatFormatting.GRAY));
            }
            return comp;
        }
    }

    private record AttributeEntry(
            Holder<Attribute> holder,
            Attribute attribute,
            ResourceLocation id,
            double baseValue,
            double value,
            List<ResolvedModifier> modifiers
    ) {
        boolean isModified() {
            return !modifiers.isEmpty() || Math.abs(value - baseValue) >= 1e-5;
        }
    }

    private static final class TooltipRow {
        private final boolean divider;
        private final Component headerLeft, headerRight;
        private final FormattedCharSequence plainLine;
        private final ModifierSource source;
        private final Component srcLeft, srcRight, subLine;

        private TooltipRow(boolean divider, Component headerLeft, Component headerRight,
                           FormattedCharSequence plainLine, ModifierSource source,
                           Component srcLeft, Component srcRight, Component subLine) {
            this.divider = divider;
            this.headerLeft = headerLeft;
            this.headerRight = headerRight;
            this.plainLine = plainLine;
            this.source = source;
            this.srcLeft = srcLeft;
            this.srcRight = srcRight;
            this.subLine = subLine;
        }

        static TooltipRow divider() {
            return new TooltipRow(true, null, null, null, null, null, null, null);
        }

        static TooltipRow header(Component left, Component right) {
            return new TooltipRow(false, left, right, null, null, null, null, null);
        }

        static TooltipRow text(FormattedCharSequence line) {
            return new TooltipRow(false, null, null, line, null, null, null, null);
        }

        static TooltipRow sourceRow(ModifierSource source, Component left, Component right, Component sub) {
            return new TooltipRow(false, null, null, null, source, left, right, sub);
        }

        int measureWidth(Font font) {
            if (divider) return 0;
            if (headerLeft != null) return font.width(headerLeft) + 16 + font.width(headerRight);
            if (source != null) {
                int topW = 15 + font.width(srcLeft) + 12 + font.width(srcRight);
                int subW = subLine != null ? 15 + font.width(subLine) : 0;
                return Math.max(topW, subW);
            }
            return font.width(plainLine);
        }

        int height(Font font) {
            if (divider) return 5;
            if (source != null) return subLine != null ? (font.lineHeight * 2 + 4) : 14;
            return font.lineHeight + 2;
        }

        void render(GuiGraphics graphics, Font font, Minecraft mc, int x, int y, int innerWidth) {
            if (divider) {
                graphics.fill(x, y + 2, x + innerWidth, y + 3, 0x33FFFFFF);
                return;
            }
            if (headerLeft != null) {
                int rw = font.width(headerRight);
                graphics.drawString(font, fitText(font, headerLeft, Math.max(20, innerWidth - rw - 6)), x, y, 0xFFFFFF, true);
                graphics.drawString(font, headerRight, x + innerWidth - rw, y, 0xFFFFFF, true);
                return;
            }
            if (source != null) {
                if (!source.stack().isEmpty()) {
                    renderScaledItem(graphics, source.stack(), x, y + 1, 12);
                } else if (source.effect() != null) {
                    TextureAtlasSprite sprite = mc.getMobEffectTextures().get(source.effect().getEffect());
                    graphics.blit(x, y + 1, 0, 12, 12, sprite);
                } else {
                    graphics.drawString(font, "•", x + 3, y + 2, 0xFF71717A, false);
                }

                int textStartX = x + 15;
                int rw = font.width(srcRight);
                int leftMaxW = Math.max(20, (x + innerWidth) - textStartX - rw - 6);
                graphics.drawString(font, fitText(font, srcLeft, leftMaxW), textStartX, y + 2, 0xFFFFFF, true);
                graphics.drawString(font, srcRight, x + innerWidth - rw, y + 2, 0xFFFFFF, true);

                if (subLine != null) {
                    graphics.drawString(font, fitText(font, subLine, Math.max(20, innerWidth - 15)),
                            textStartX, y + font.lineHeight + 3, 0xFFFFFF, false);
                }
                return;
            }
            graphics.drawString(font, plainLine, x, y, 0xFFFFFF, true);
        }
    }
}