package com.example.examplemod.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

import java.util.List;

public final class VisualComponents {

    private VisualComponents() {
    }

    public static void registerFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MapPreview.class, ClientMapPreview::new);
        event.register(DynamicEntityPreview.class, ClientDynamicEntityPreview::new);
        event.register(BlockGridPreview.class, ClientBlockGridPreview::new);
        event.register(TrimPreview.class, ClientTrimPreview::new);
        event.register(FoodComponent.class, ClientFoodComponent::new);
        event.register(DurabilityBar.class, ClientDurabilityBar::new);
        event.register(PotionEffects.class, ClientPotionEffects::new);
        event.register(FuelGauge.class, ClientFuelGauge::new);
        event.register(ContainerGrid.class, ClientContainerGrid::new);
        event.register(OminousBottleBadge.class, ClientOminousBottleBadge::new);
        event.register(CooldownGauge.class, ClientCooldownGauge::new);
        event.register(JukeboxCard.class, ClientJukeboxCard::new);
        event.register(ColorSwatch.class, ClientColorSwatch::new);
        event.register(LodestoneCard.class, ClientLodestoneCard::new);
        event.register(BookSummary.class, ClientBookSummary::new);
    }


    public record MapPreview(MapId mapId) implements TooltipComponent {
    }

    public static class ClientMapPreview implements ClientTooltipComponent {
        private final MapPreview data;

        public ClientMapPreview(MapPreview data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 68;
        }

        @Override
        public int getHeight() {
            return 68;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            MapItemSavedData mapData = mc.level.getMapData(data.mapId());
            graphics.fill(x, y, x + 68, y + 68, 0xFF2A2A2A);
            graphics.renderOutline(x, y, 68, 68, 0xFF8A6A42);
            if (mapData != null) {
                var pose = graphics.pose();
                pose.pushPose();
                try {
                    pose.translate(x + 2, y + 2, 0.0);
                    pose.scale(0.5F, 0.5F, 1.0F);

                    var buffer = graphics.bufferSource();
                    try {
                        mc.gameRenderer.getMapRenderer().render(
                                pose, buffer, data.mapId(), mapData, false, 0xF000F0
                        );
                    } finally {
                        buffer.endBatch();
                    }
                } finally {
                    pose.popPose();
                }
            } else {
                graphics.drawCenteredString(
                        font, "未探索地图", x + 34, y + 30, 0xFFAAAAAA
                );
            }

        }
    }

    private static String ellipsize(Font font, String text, int width) {
        if (font.width(text) <= width) return text;

        String suffix = "...";
        int remaining = width - font.width(suffix);
        return remaining <= 0
                ? font.plainSubstrByWidth(suffix, Math.max(0, width))
                : font.plainSubstrByWidth(text, remaining) + suffix;
    }


    public record EntityEntry(String entityId, int timeInHive, int minTime) {
    }

    public record DynamicEntityPreview(
            List<EntityEntry> entities,
            String title,
            ItemStack fallbackIcon
    ) implements TooltipComponent {
        public DynamicEntityPreview {
            entities = List.copyOf(entities);
            fallbackIcon = fallbackIcon == null ? ItemStack.EMPTY : fallbackIcon.copy();
        }

        public DynamicEntityPreview(List<EntityEntry> entities, String title) {
            this(entities, title, ItemStack.EMPTY);
        }
    }

    public static class ClientDynamicEntityPreview implements ClientTooltipComponent {
        private final DynamicEntityPreview data;

        public ClientDynamicEntityPreview(DynamicEntityPreview data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 140;
        }

        @Override
        public int getHeight() {
            return 14 + data.entities().size() * 18;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.drawString(font, data.title() + " (" + data.entities().size() + ")", x, y + 2, 0xFFFFCC00, true);
            int curY = y + 12;
            for (EntityEntry entry : data.entities()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.entityId());
                EntityType<?> type = (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
                        ? BuiltInRegistries.ENTITY_TYPE.get(id) : null;
                ItemStack icon = ItemStack.EMPTY;

                if (type != null) {
                    SpawnEggItem egg = SpawnEggItem.byId(type);
                    if (egg != null) {
                        icon = new ItemStack(egg);
                    } else if (id != null) {
                        ResourceLocation eggId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
                        if (BuiltInRegistries.ITEM.containsKey(eggId)) {
                            icon = new ItemStack(BuiltInRegistries.ITEM.get(eggId));
                        }
                    }
                }

                if (icon.isEmpty() && data.fallbackIcon() != null && !data.fallbackIcon().isEmpty()) {
                    icon = data.fallbackIcon();
                }

                if (icon.isEmpty()) {
                    icon = new ItemStack(Items.LEAD);
                }

                graphics.renderFakeItem(icon, x, curY);
                String name = type != null ? type.getDescription().getString() : entry.entityId();
                graphics.drawString(font, name, x + 20, curY + 1, 0xFFFFFFFF, false);
                if (entry.minTime() > 0) {
                    int pct = Math.min(100, Math.round(((float) entry.timeInHive() / entry.minTime()) * 100.0F));
                    graphics.drawString(font, "进度: " + pct + "%", x + 20, curY + 9, 0xFF88FF88, false);
                }
                curY += 18;
            }
        }
    }


    public record BlockGridPreview(
            List<ItemStack> blocks,
            String label,
            int color
    ) implements TooltipComponent {
        public BlockGridPreview {
            blocks = TooltipUtil.copyStacks(blocks);
        }
    }

    public static class ClientBlockGridPreview implements ClientTooltipComponent {
        private final BlockGridPreview data;
        private static final int MAX_COLS = 8;

        public ClientBlockGridPreview(BlockGridPreview data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return Math.max(font.width(data.label()), Math.min(MAX_COLS, data.blocks().size()) * 18);
        }

        @Override
        public int getHeight() {
            return 12 + ((int) Math.ceil((double) data.blocks().size() / MAX_COLS)) * 18;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.drawString(font, data.label(), x, y + 1, data.color(), true);
            int startY = y + 11;
            for (int i = 0; i < data.blocks().size(); i++) {
                int col = i % MAX_COLS, row = i / MAX_COLS;
                int bx = x + col * 18, by = startY + row * 18;
                graphics.fill(bx, by, bx + 16, by + 16, 0x33FFFFFF);
                graphics.renderFakeItem(data.blocks().get(i), bx, by);
            }
        }
    }


    public record TrimPreview(ArmorTrim trim) implements TooltipComponent {
    }

    public static class ClientTrimPreview implements ClientTooltipComponent {
        private final TrimPreview data;

        public ClientTrimPreview(TrimPreview data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 130;
        }

        @Override
        public int getHeight() {
            return 14;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int trimColor = data.trim().material().value().description().getStyle().getColor() != null
                    ? data.trim().material().value().description().getStyle().getColor().getValue() : 0xAAAAAA;
            graphics.fill(x, y + 2, x + 8, y + 10, 0xFF000000 | trimColor);
            graphics.renderOutline(x, y + 2, 8, 8, 0xFFFFFFFF);
            String pattern = data.trim().pattern().value().description().getString();
            String mat = data.trim().material().value().description().getString();
            graphics.drawString(font, pattern + " (" + mat + ")", x + 12, y + 2, 0xFFDDDDDD, true);
        }
    }





    public record FoodComponent(FoodProperties food) implements TooltipComponent {
    }

    public static class ClientFoodComponent implements ClientTooltipComponent {
        private static final ResourceLocation FOOD_FULL = ResourceLocation.withDefaultNamespace("hud/food_full");
        private static final ResourceLocation FOOD_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");
        private final FoodComponent data;

        public ClientFoodComponent(FoodComponent data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            int hunger = data.food().nutrition();
            int drumsticks = (int) Math.ceil(hunger / 2.0);
            return Math.min(drumsticks, 10) * 9 + font.width("+" + hunger + " (+" + String.format("%.1f", data.food().saturation()) + ")") + 10;
        }

        @Override
        public int getHeight() {
            return 11;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int hunger = data.food().nutrition();
            int curX = x;
            int fullDrums = hunger / 2;
            boolean half = (hunger % 2) != 0;
            int drawn = 0;
            for (int i = 0; i < fullDrums && drawn < 10; i++, drawn++) {
                graphics.blitSprite(FOOD_FULL, curX, y + 1, 9, 9);
                curX += 9;
            }
            if (half && drawn < 10) {
                graphics.blitSprite(FOOD_HALF, curX, y + 1, 9, 9);
                curX += 9;
            }
            curX += 4;
            String text = "+" + hunger;
            graphics.drawString(font, text, curX, y + 2, 0xFFE67E22, true);
            curX += font.width(text) + 3;
            graphics.drawString(font, "(+" + String.format("%.1f", data.food().saturation()) + ")", curX, y + 2, 0xFFFFCC00, true);
        }
    }


    public record DurabilityBar(int current, int max) implements TooltipComponent {
    }

    public static class ClientDurabilityBar implements ClientTooltipComponent {
        private final DurabilityBar data;

        public ClientDurabilityBar(DurabilityBar data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 110;
        }

        @Override
        public int getHeight() {
            return 13;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            float ratio = TooltipUtil.ratio(data.current(), data.max());
            int barWidth = Math.round(108 * ratio);

            int color = ratio > 0.5F
                    ? TooltipUtil.lerpColor(0xFFFFB800, 0xFF34C759, (ratio - 0.5F) * 2.0F)
                    : TooltipUtil.lerpColor(0xFFFF3B30, 0xFFFFB800, ratio * 2.0F);


            graphics.fill(x, y, x + 110, y + 11, 0xAA111111);
            graphics.renderOutline(x, y, 110, 11, 0xFF3A3A3C);
            if (barWidth > 0) graphics.fill(x + 1, y + 1, x + 1 + barWidth, y + 10, color | 0xFF000000);
            String text = data.current() + " / " + data.max() + " (" + Math.round(ratio * 100.0F) + "%)";
            graphics.drawString(font, text, x + (110 - font.width(text)) / 2, y + 2, 0xFFFFFFFF, true);
        }

        private static int lerpColor(int from, int to, float factor) {
            int r = (int) Mth.lerp(factor, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
            int g = (int) Mth.lerp(factor, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
            int b = (int) Mth.lerp(factor, from & 0xFF, to & 0xFF);
            return (r << 16) | (g << 8) | b;
        }
    }


    public record PotionEffects(
            List<MobEffectInstance> effects,
            float tickRate
    ) implements TooltipComponent {
        public PotionEffects {
            effects = effects.stream().map(MobEffectInstance::new).toList();
            tickRate = Float.isFinite(tickRate) && tickRate > 0.0F ? tickRate : 20.0F;
        }
    }

    public static class ClientPotionEffects implements ClientTooltipComponent {
        private final PotionEffects data;

        public ClientPotionEffects(PotionEffects data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 140;
        }

        @Override
        public int getHeight() {
            return data.effects().isEmpty() ? 0 : data.effects().size() * 19;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            var tm = Minecraft.getInstance().getMobEffectTextures();
            int curY = y;
            for (MobEffectInstance effect : data.effects()) {
                TextureAtlasSprite sprite = tm.get(effect.getEffect());
                RenderSystem.enableBlend();
                graphics.blit(x, curY + 1, 0, 16, 16, sprite);
                String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
                graphics.drawString(font, effect.getEffect().value().getDisplayName().getString() + level, x + 20, curY + 1, 0xFFFFFFFF, true);
                graphics.drawString(font, MobEffectUtil.formatDuration(effect, 1.0F, data.tickRate()).getString(), x + 20, curY + 9, 0xFFAAAAAA, true);
                curY += 19;
            }
        }
    }


    public record FuelGauge(int burnTicks) implements TooltipComponent {
    }

    public static class ClientFuelGauge implements ClientTooltipComponent {
        private static final ItemStack COAL_ICON = new ItemStack(Items.COAL);
        private final FuelGauge data;

        public ClientFuelGauge(FuelGauge data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 120;
        }

        @Override
        public int getHeight() {
            return 18;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.renderFakeItem(COAL_ICON, x, y);
            graphics.drawString(font, "可烧炼:", x + 18, y + 1, 0xFFAAAAAA, false);
            graphics.drawString(font, String.format("%.1f 份 (%.1fs)", data.burnTicks() / 200.0F, data.burnTicks() / 20.0F), x + 18, y + 9, 0xFFFF793F, true);
        }
    }


    public record ContainerGrid(List<ItemStack> items) implements TooltipComponent {
        public ContainerGrid {
            items = TooltipUtil.copyStacks(items);
        }
    }

    public static class ClientContainerGrid implements ClientTooltipComponent {
        private static final int SLOT_SIZE = 18;
        private static final int COLS = 9;
        private final ContainerGrid data;

        public ClientContainerGrid(ContainerGrid data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return Math.min(COLS, Math.max(data.items().size(), 1)) * SLOT_SIZE + 2;
        }

        @Override
        public int getHeight() {
            return ((int) Math.ceil((double) Math.max(1, data.items().size()) / COLS)) * SLOT_SIZE + 2;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            for (int i = 0; i < data.items().size(); i++) {
                int col = i % COLS, row = i / COLS;
                int sx = x + col * SLOT_SIZE, sy = y + row * SLOT_SIZE;
                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0x44000000);
                graphics.renderOutline(sx, sy, SLOT_SIZE, SLOT_SIZE, 0x66FFFFFF);
                ItemStack st = data.items().get(i);
                if (!st.isEmpty()) {
                    graphics.renderItem(st, sx + 1, sy + 1);
                    graphics.renderItemDecorations(font, st, sx + 1, sy + 1);
                }
            }
        }
    }


    public record OminousBottleBadge(int amplifier) implements TooltipComponent {
    }

    public static class ClientOminousBottleBadge implements ClientTooltipComponent {
        private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};
        private final OminousBottleBadge data;

        public ClientOminousBottleBadge(OminousBottleBadge data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 80;
        }

        @Override
        public int getHeight() {
            return 14;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.fill(x, y, x + 75, y + 12, 0x882B003B);
            graphics.renderOutline(x, y, 75, 12, 0xFFAA00AA);
            String label = "☠ 灾厄等级 " + ROMAN[Mth.clamp(data.amplifier(), 0, 4)];
            graphics.drawString(font, label, x + 4, y + 2, 0xFFFF55FF, true);
        }
    }


    public record CooldownGauge(float percent) implements TooltipComponent {
    }

    public static class ClientCooldownGauge implements ClientTooltipComponent {
        private final CooldownGauge data;

        public ClientCooldownGauge(CooldownGauge data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 110;
        }

        @Override
        public int getHeight() {
            return 12;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.fill(x, y, x + 110, y + 10, 0x88220000);
            graphics.renderOutline(x, y, 110, 10, 0xFFFF5555);
            float percent = TooltipUtil.unit(data.percent());
            int fillW = Math.round(108 * (1.0F - percent));

            if (fillW > 0) graphics.fill(x + 1, y + 1, x + 1 + fillW, y + 9, 0xFFE74C3C);
            String cd = "冷却中: " + Math.round(percent * 100.0F) + "%";
            graphics.drawString(font, cd, x + (110 - font.width(cd)) / 2, y + 1, 0xFFFFFFFF, true);
        }
    }


    public record JukeboxCard(Component title, int lengthSeconds, int comparatorOutput) implements TooltipComponent {
    }

    public static class ClientJukeboxCard implements ClientTooltipComponent {
        private static final ItemStack RECORD_ICON = new ItemStack(Items.MUSIC_DISC_CAT);
        private final JukeboxCard data;

        public ClientJukeboxCard(JukeboxCard data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return Math.max(140, font.width(data.title()) + 28);
        }

        @Override
        public int getHeight() {
            return 28;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int w = getWidth(font);
            graphics.fill(x, y, x + w, y + 26, 0x55000000);
            graphics.renderOutline(x, y, w, 26, 0x4455FFFF);
            graphics.renderFakeItem(RECORD_ICON, x + 3, y + 5);
            graphics.drawString(font, data.title(), x + 24, y + 4, 0xFF55FFFF, true);
            String timeStr = String.format("⏱ %02d:%02d", data.lengthSeconds() / 60, data.lengthSeconds() % 60);
            graphics.drawString(font, timeStr, x + 24, y + 14, 0xFFAAAAAA, false);
            if (data.comparatorOutput() > 0) {
                graphics.drawString(font, "⚡ 信号 " + data.comparatorOutput(), x + 32 + font.width(timeStr), y + 14, 0xFFFF5555, false);
            }
        }
    }


    public record ColorSwatch(int rgb) implements TooltipComponent {
    }

    public static class ClientColorSwatch implements ClientTooltipComponent {
        private final ColorSwatch data;

        public ClientColorSwatch(ColorSwatch data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 85;
        }

        @Override
        public int getHeight() {
            return 12;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.fill(x, y + 1, x + 10, y + 11, 0xFF000000 | data.rgb());
            graphics.renderOutline(x, y + 1, 10, 10, 0xFFFFFFFF);
            graphics.drawString(font, String.format("染色: #%06X", (0xFFFFFF & data.rgb())), x + 14, y + 2, 0xFFDDDDDD, true);
        }
    }


    public record LodestoneCard(ResourceLocation dim, BlockPos pos, double distance) implements TooltipComponent {
    }

    public static class ClientLodestoneCard implements ClientTooltipComponent {
        private static final ItemStack COMPASS = new ItemStack(Items.COMPASS);
        private final LodestoneCard data;

        public ClientLodestoneCard(LodestoneCard data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 150;
        }

        @Override
        public int getHeight() {
            return 22;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.renderFakeItem(COMPASS, x, y + 3);
            graphics.drawString(font, String.format("X:%d Y:%d Z:%d", data.pos().getX(), data.pos().getY(), data.pos().getZ()), x + 18, y + 2, 0xFF55FFFF, false);
            String dist = data.distance() >= 0 ? String.format("距离: %.0fm (%s)", data.distance(), data.dim().getPath()) : data.dim().getPath();
            graphics.drawString(font, dist, x + 18, y + 11, 0xFFAAAAAA, false);
        }
    }


    public record BookSummary(String author, int pages, String snippet) implements TooltipComponent {
    }

    public static class ClientBookSummary implements ClientTooltipComponent {
        private static final ItemStack BOOK = new ItemStack(Items.WRITTEN_BOOK);
        private final BookSummary data;

        public ClientBookSummary(BookSummary data) {
            this.data = data;
        }

        @Override
        public int getWidth(Font font) {
            return 145;
        }

        @Override
        public int getHeight() {
            return 28;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            graphics.renderFakeItem(BOOK, x, y + 5);

            int available = getWidth(font) - 18;
            String heading = "页数: " + data.pages()
                    + (data.author().isEmpty() ? "" : " | " + data.author());
            String snippet = data.snippet()
                    .replace('\n', ' ')
                    .replace('\r', ' ');

            graphics.drawString(
                    font, ellipsize(font, heading, available),
                    x + 18, y + 3, 0xFFFFAA00, false
            );
            graphics.drawString(
                    font, ellipsize(font, "“" + snippet + "”", available),
                    x + 18, y + 14, 0xFFAAAAAA, false
            );
        }

    }
}