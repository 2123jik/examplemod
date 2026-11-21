package com.example.examplemod.client.screen;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = AffixCodexScreen.MOD_ID, value = Dist.CLIENT)
public class AffixCodexScreen extends Screen {

    public static final String MOD_ID = "examplemod";

    // =========================================================================
    // 1. ESC 菜单右上角注入按钮
    // =========================================================================

    @SubscribeEvent
    public static void onPauseScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)) {
            return;
        }
        int btnW = 104;
        int btnH = 20;
        int btnX = pauseScreen.width - btnW - 8;
        int btnY = 8;

        event.addListener(new CodexEntryButton(btnX, btnY, btnW, btnH, pauseScreen));
    }

    private static final class CodexEntryButton extends Button {
        private final ItemStack icon = new ItemStack(Items.NETHERITE_SWORD);

        private CodexEntryButton(int x, int y, int width, int height, Screen parent) {
            super(
                    x, y, width, height,
                    Component.literal("词缀图鉴"),
                    btn -> Minecraft.getInstance().setScreen(new AffixCodexScreen(parent)),
                    DEFAULT_NARRATION
            );
            this.setTooltip(Tooltip.create(Component.literal("查看所有战利品分类、词缀池、法术指南与效果详情")));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();
            int bg = hovered ? 0xE8242038 : 0xD8161722;
            int border = hovered ? 0xFFFFB74D : 0xFF6B5B95;

            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            g.fill(this.getX() + 2, this.getY() + 2, this.getX() + 18, this.getY() + 18, 0x557E57C2);
            g.renderFakeItem(this.icon, this.getX() + 2, this.getY() + 2);

            int textColor = hovered ? 0xFFFFE082 : 0xFFEAEAF5;
            int textX = this.getX() + 22 + (this.width - 24 - Minecraft.getInstance().font.width(this.getMessage())) / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            g.drawString(Minecraft.getInstance().font, this.getMessage(), textX, textY, textColor, true);
        }
    }

    // =========================================================================
    // 2. 数据模型
    // =========================================================================

    private record RarityText(
            LootRarity rarity,
            String rarityId,
            int colorRgb,
            Component augmentingText,
            Component minText,
            Component maxText
    ) {}

    private record CategoryInfo(
            @Nullable LootCategory category,
            ResourceLocation id,
            Component displayName,
            ItemStack icon,
            boolean isAll
    ) {}

    private record AffixVisual(
            @Nullable Holder<MobEffect> mobEffect,
            @Nullable ResourceLocation spellIconTex,
            ItemStack fallbackItem
    ) {}

    private record WorldTierWeight(
            String tierKey,
            String shortLabel,
            int colorRgb,
            int weight,
            float quality
    ) {}

    private record SpellDetails(
            ResourceLocation spellId,
            Component spellName,
            Component schoolName,
            int schoolColorRgb,
            String guideText,
            String castTypeLabel,
            int minLevel,
            int maxLevel,
            String manaCostSummary,
            String cooldownSummary,
            String castTimeSummary,
            List<Component> uniqueStats
    ) {}

    private record MobEffectDetails(
            ResourceLocation effectId,
            Component effectName,
            MobEffectCategory category,
            boolean instantaneous,
            String description,
            List<Component> attributeModifiers,
            @Nullable String triggerTargetLabel,
            int stackingLimit
    ) {}

    private record AttributeDetails(
            ResourceLocation attributeId,
            Component attributeName,
            @Nullable String operationLabel,
            String description
    ) {}

    private record AffixEntry(
            ResourceLocation id,
            Affix affix,
            AffixType type,
            Component title,
            String modName,
            List<CategoryInfo> categories,
            Map<LootRarity, RarityText> rarityTexts,
            int baseWeight,
            float baseQuality,
            List<WorldTierWeight> tierWeights,
            List<String> exclusiveSet,
            AffixVisual visual,
            @Nullable SpellDetails spellDetails,
            @Nullable MobEffectDetails mobEffectDetails,
            @Nullable AttributeDetails attributeDetails,
            @Nullable String abilityTriggerLabel,
            String searchBlob
    ) {
        @Nullable
        RarityText textFor(@Nullable LootRarity filterRarity) {
            if (filterRarity != null) {
                return rarityTexts.get(filterRarity);
            }
            RarityText best = null;
            for (RarityText rt : rarityTexts.values()) {
                best = rt;
            }
            return best;
        }
    }

    private static volatile List<CategoryInfo> cachedRealCategories;
    private static volatile LootCategory cachedNoneCategory;

    private final Screen parent;
    private final List<LootRarity> allRarities = new ArrayList<>();
    private final List<CategoryInfo> allCategories = new ArrayList<>();
    private final List<AffixEntry> allAffixes = new ArrayList<>();
    private final Map<ResourceLocation, Component> titleById = new LinkedHashMap<>();
    private final List<AffixEntry> filteredAffixes = new ArrayList<>();

    private EditBox searchBox;
    private Button copyIdButton;

    @Nullable
    private CategoryInfo selectedCategory;
    @Nullable
    private LootRarity selectedRarity;
    @Nullable
    private AffixType selectedType; // null = 全部类型
    @Nullable
    private AffixEntry selectedAffix;
    @Nullable
    private AffixEntry hoveredAffix;

    private double categoryScroll = 0;
    private double affixScroll = 0;
    private double detailScroll = 0;
    private int lastDetailContentHeight = 0;
    private boolean draggingAffixScrollbar = false;
    private boolean draggingDetailScrollbar = false;

    private long copyToastUntilMs = 0;

    private int headerBottom;
    private int leftX, leftW;
    private int centerX, centerW;
    private int rightX, rightW;
    private int bodyTop, bodyBottom;
    private boolean showRightPanel;

    private static final int CAT_ROW_H = 24;
    private static final int AFFIX_CARD_H = 48;
    private static final int AFFIX_CARD_GAP = 4;

    public AffixCodexScreen(Screen parent) {
        super(Component.literal("神化词缀池图鉴"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (this.allAffixes.isEmpty()) {
            loadCodexData();
        }

        this.headerBottom = 52;
        this.bodyTop = this.headerBottom + 4;
        this.bodyBottom = this.height - 10;

        this.leftX = 8;
        this.leftW = 132;

        this.showRightPanel = this.width >= 540;
        this.rightW = this.showRightPanel ? Math.min(250, Math.max(195, (int) (this.width * 0.35f))) : 0;
        this.rightX = this.width - this.rightW - 8;

        this.centerX = this.leftX + this.leftW + 6;
        this.centerW = this.showRightPanel
                ? (this.rightX - 6 - this.centerX)
                : (this.width - 8 - this.centerX);

        // 顶栏第一行：搜索框 + 词缀类型筛选 + 关闭按钮
        int closeBtnW = 54;
        int typePillsW = this.width >= 520 ? 132 : 0;
        int searchX = Math.max(118, this.leftX + this.leftW + 6);
        int searchMaxRight = this.width - closeBtnW - 14 - typePillsW;
        int searchW = Math.max(96, Math.min(220, searchMaxRight - searchX));

        String prevQuery = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.font, searchX, 8, searchW, 18, Component.literal("搜索词缀..."));
        this.searchBox.setHint(Component.literal("搜名称/法术指南/药水描述/ID...").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setMaxLength(80);
        this.searchBox.setValue(prevQuery);
        this.searchBox.setResponder(s -> {
            this.affixScroll = 0;
            applyFilters();
        });
        this.addRenderableWidget(this.searchBox);

        if (typePillsW > 0) {
            int tx = searchX + searchW + 6;
            this.addRenderableWidget(new TypeFilterButton(tx, 9, 30, 16, null, "全类", 0xE0E0E0));
            this.addRenderableWidget(new TypeFilterButton(tx + 32, 9, 30, 16, AffixType.STAT, "属性", 0x55FFFF));
            this.addRenderableWidget(new TypeFilterButton(tx + 64, 9, 30, 16, AffixType.BASIC_EFFECT, "特效", 0x55FF55));
            this.addRenderableWidget(new TypeFilterButton(tx + 96, 9, 30, 16, AffixType.ABILITY, "能力", 0xFF77FF));
        }

        this.addRenderableWidget(Button.builder(Component.literal("× 返回"), btn -> this.onClose())
                .bounds(this.width - closeBtnW - 8, 8, closeBtnW, 18)
                .build());

        // 顶栏第二行：稀有度筛选按钮
        int rx = this.leftX + this.leftW + 6;
        int ry = 30;
        this.addRenderableWidget(new RarityFilterButton(rx, ry, 38, 16, null, "全部"));
        rx += 41;
        for (LootRarity rarity : this.allRarities) {
            String label = getRarityShortLabel(rarity);
            int bw = Math.max(36, this.font.width(label) + 12);
            if (rx + bw < this.width - 8) {
                this.addRenderableWidget(new RarityFilterButton(rx, ry, bw, 16, rarity, label));
                rx += bw + 3;
            }
        }

        if (this.showRightPanel) {
            this.copyIdButton = Button.builder(Component.literal("复制 ID"), btn -> {
                if (this.selectedAffix != null) {
                    copyAffixId(this.selectedAffix);
                }
            }).bounds(this.rightX + this.rightW - 58, this.bodyTop + 34, 50, 15).build();
            this.addRenderableWidget(this.copyIdButton);
        } else {
            this.copyIdButton = null;
        }

        applyFilters();
    }

    private final class TypeFilterButton extends Button {
        @Nullable
        private final AffixType type;
        private final int rgb;

        private TypeFilterButton(int x, int y, int w, int h, @Nullable AffixType type, String label, int rgb) {
            super(x, y, w, h, Component.literal(label), btn -> {
                AffixCodexScreen.this.selectedType = type;
                AffixCodexScreen.this.affixScroll = 0;
                AffixCodexScreen.this.applyFilters();
            }, DEFAULT_NARRATION);
            this.type = type;
            this.rgb = rgb;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = AffixCodexScreen.this.selectedType == this.type;
            boolean hovered = this.isHoveredOrFocused();
            int bg = selected ? 0xCC292740 : (hovered ? 0xCC202330 : 0xAA141720);
            int border = selected ? (0xFF000000 | this.rgb) : (hovered ? 0xFF7B8098 : 0xFF34384A);
            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            int textColor = selected ? (0xFF000000 | this.rgb) : (0xBB000000 | this.rgb);
            int tx = this.getX() + (this.width - AffixCodexScreen.this.font.width(this.getMessage())) / 2;
            int ty = this.getY() + (this.height - 8) / 2;
            g.drawString(AffixCodexScreen.this.font, this.getMessage(), tx, ty, textColor, true);
        }
    }

    private final class RarityFilterButton extends Button {
        @Nullable
        private final LootRarity rarity;

        private RarityFilterButton(int x, int y, int w, int h, @Nullable LootRarity rarity, String label) {
            super(x, y, w, h, Component.literal(label), btn -> {
                AffixCodexScreen.this.selectedRarity = rarity;
                AffixCodexScreen.this.affixScroll = 0;
                AffixCodexScreen.this.applyFilters();
            }, DEFAULT_NARRATION);
            this.rarity = rarity;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = AffixCodexScreen.this.selectedRarity == this.rarity;
            boolean hovered = this.isHoveredOrFocused();
            int rarityRgb = this.rarity != null ? getRarityColorRgb(this.rarity) : 0xE0E0E0;

            int bg = selected ? (0xCC000000 | (rarityRgb & 0x3F3F3F) + 0x222222) : (hovered ? 0xCC252836 : 0xAA161822);
            int border = selected ? (0xFF000000 | rarityRgb) : (hovered ? 0xFF7B8098 : 0xFF3A3E52);

            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            int textColor = selected ? (0xFF000000 | rarityRgb) : (0xCC000000 | rarityRgb);
            int tx = this.getX() + (this.width - AffixCodexScreen.this.font.width(this.getMessage())) / 2;
            int ty = this.getY() + (this.height - 8) / 2;
            g.drawString(AffixCodexScreen.this.font, this.getMessage(), tx, ty, textColor, true);
        }
    }

    // =========================================================================
    // 3. 加载运行时词缀池与关联百科详情
    // =========================================================================

    private void loadCodexData() {
        this.allRarities.clear();
        this.allRarities.addAll(RarityRegistry.INSTANCE.getValues());
        this.allRarities.sort(Comparator.comparingInt(AffixCodexScreen::getRaritySortOrder));

        List<CategoryInfo> realCats = getOrScanCategories();
        this.allCategories.clear();
        CategoryInfo allCat = new CategoryInfo(
                null,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "all"),
                Component.literal("全部装备"),
                new ItemStack(Items.NETHER_STAR),
                true
        );
        this.allCategories.add(allCat);
        this.allCategories.addAll(realCats);
        if (this.selectedCategory == null) {
            this.selectedCategory = allCat;
        }

        Minecraft mc = Minecraft.getInstance();
        Item.TooltipContext itemCtx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        AttributeTooltipContext tooltipCtx = AttributeTooltipContext.of(mc.player, itemCtx, TooltipFlag.NORMAL);

        Map<LootCategory, CategoryInfo> catByObj = new LinkedHashMap<>();
        for (CategoryInfo ci : realCats) {
            if (ci.category() != null) {
                catByObj.put(ci.category(), ci);
            }
        }

        this.allAffixes.clear();
        this.titleById.clear();

        for (Affix affix : AffixRegistry.INSTANCE.getValues()) {
            ResourceLocation affixId = AffixRegistry.INSTANCE.getKey(affix);
            if (affixId == null) continue;

            Map<LootRarity, RarityText> rarityTexts = new LinkedHashMap<>();
            Set<CategoryInfo> catSet = new LinkedHashSet<>();

            for (LootRarity rarity : this.allRarities) {
                ResourceLocation rId = RarityRegistry.INSTANCE.getKey(rarity);
                String rIdStr = rId != null ? rId.toString() : "unknown";

                ItemStack anySupportedStack = ItemStack.EMPTY;
                for (CategoryInfo ci : realCats) {
                    if (ci.category() == null) continue;
                    if (safeCanApplyTo(affix, ci.icon(), ci.category(), rarity)) {
                        catSet.add(ci);
                        if (anySupportedStack.isEmpty()) {
                            anySupportedStack = ci.icon();
                        }
                    }
                }

                if (catSet.isEmpty()) {
                    for (LootCategory declared : extractDeclaredCategories(affix)) {
                        CategoryInfo ci = catByObj.get(declared);
                        if (ci != null) {
                            catSet.add(ci);
                            if (anySupportedStack.isEmpty()) {
                                anySupportedStack = ci.icon();
                            }
                        }
                    }
                }

                boolean raritySupported = !anySupportedStack.isEmpty();
                if (!raritySupported && !catSet.isEmpty()) {
                    CategoryInfo firstCi = catSet.iterator().next();
                    raritySupported = safeCanApplyTo(affix, firstCi.icon(), firstCi.category(), rarity)
                            || hasConfiguredRarityValue(affix, rarity);
                }
                if (!raritySupported && catSet.isEmpty()) {
                    raritySupported = hasConfiguredRarityValue(affix, rarity);
                }

                if (raritySupported) {
                    ItemStack stackForText = anySupportedStack.isEmpty() ? new ItemStack(Items.NETHERITE_SWORD) : anySupportedStack;
                    Component aug = safeGetAugmentingText(affix, stackForText, rarity, 1.0f, tooltipCtx);
                    Component min = safeGetDescription(affix, stackForText, rarity, 0.0f, tooltipCtx);
                    Component max = safeGetDescription(affix, stackForText, rarity, 1.0f, tooltipCtx);

                    String augStr = aug.getString().trim();
                    String maxStr = max.getString().trim();
                    if (!augStr.isEmpty() || !maxStr.isEmpty()) {
                        Component primary = !augStr.isEmpty() ? aug : max;
                        rarityTexts.put(rarity, new RarityText(rarity, rIdStr, getRarityColorRgb(rarity), primary, min, max));
                    }
                }
            }

            List<CategoryInfo> matchedCategories = new ArrayList<>(catSet);

            if (rarityTexts.isEmpty() && !this.allRarities.isEmpty()) {
                LootRarity topRarity = this.allRarities.get(this.allRarities.size() - 1);
                ItemStack probe = matchedCategories.isEmpty() ? new ItemStack(Items.NETHERITE_SWORD) : matchedCategories.get(0).icon();
                Component aug = safeGetAugmentingText(affix, probe, topRarity, 1.0f, tooltipCtx);
                Component max = safeGetDescription(affix, probe, topRarity, 1.0f, tooltipCtx);
                Component primary = !aug.getString().isBlank() ? aug : max;
                if (!primary.getString().isBlank()) {
                    ResourceLocation rId = RarityRegistry.INSTANCE.getKey(topRarity);
                    rarityTexts.put(topRarity, new RarityText(
                            topRarity,
                            rId != null ? rId.toString() : "mythic",
                            getRarityColorRgb(topRarity),
                            primary, max, max
                    ));
                }
            }

            AffixDefinition def = affix.definition();
            AffixType type = def.type();
            Component title = safeGetAffixTitle(affix, affixId);
            this.titleById.put(affixId, title);

            String modName = getModDisplayName(affixId.getNamespace());
            int baseWeight = extractBaseWeight(def);
            float baseQuality = extractBaseQuality(def);
            List<WorldTierWeight> tierWeights = extractWorldTierWeights(def);
            List<String> exclusive = extractExclusiveSet(def);
            AffixVisual visual = resolveVisual(affix, affixId, type);

            // 深度百科详情解析（法术 / 药水效果 / 实体属性）
            SpellDetails spellDetails = resolveSpellDetails(affix, affixId, mc.player);
            MobEffectDetails mobEffectDetails = resolveMobEffectDetails(affix);
            AttributeDetails attributeDetails = resolveAttributeDetails(affix);
            String abilityTriggerLabel = resolveTriggerTargetLabel(affix);

            StringBuilder blob = new StringBuilder();
            blob.append(title.getString()).append(' ')
                    .append(affixId).append(' ')
                    .append(modName).append(' ')
                    .append(type.name()).append(' ');
            for (RarityText rt : rarityTexts.values()) {
                blob.append(rt.augmentingText().getString()).append(' ');
            }
            for (CategoryInfo ci : matchedCategories) {
                blob.append(ci.displayName().getString()).append(' ');
            }
            if (spellDetails != null) {
                blob.append(spellDetails.spellId()).append(' ')
                        .append(spellDetails.spellName().getString()).append(' ')
                        .append(spellDetails.schoolName().getString()).append(' ')
                        .append(spellDetails.guideText()).append(' ');
                for (Component c : spellDetails.uniqueStats()) {
                    blob.append(c.getString()).append(' ');
                }
            }
            if (mobEffectDetails != null) {
                blob.append(mobEffectDetails.effectId()).append(' ')
                        .append(mobEffectDetails.effectName().getString()).append(' ')
                        .append(mobEffectDetails.description()).append(' ');
                for (Component c : mobEffectDetails.attributeModifiers()) {
                    blob.append(c.getString()).append(' ');
                }
            }
            if (attributeDetails != null) {
                blob.append(attributeDetails.attributeId()).append(' ')
                        .append(attributeDetails.attributeName().getString()).append(' ')
                        .append(attributeDetails.description()).append(' ');
            }

            this.allAffixes.add(new AffixEntry(
                    affixId,
                    affix,
                    type,
                    title,
                    modName,
                    matchedCategories,
                    rarityTexts,
                    baseWeight,
                    baseQuality,
                    tierWeights,
                    exclusive,
                    visual,
                    spellDetails,
                    mobEffectDetails,
                    attributeDetails,
                    abilityTriggerLabel,
                    blob.toString().toLowerCase(Locale.ROOT)
            ));
        }

        this.allAffixes.sort(Comparator
                .comparing((AffixEntry e) -> e.type().ordinal())
                .thenComparing(e -> e.id().toString()));
    }

    private void applyFilters() {
        String query = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        String[] terms = query.isEmpty() ? new String[0] : query.split("\\s+");

        this.filteredAffixes.clear();
        for (AffixEntry entry : this.allAffixes) {
            if (this.selectedCategory != null && !this.selectedCategory.isAll()) {
                if (!entry.categories().contains(this.selectedCategory)) {
                    continue;
                }
            }
            if (this.selectedType != null && entry.type() != this.selectedType) {
                continue;
            }
            if (this.selectedRarity != null && !entry.rarityTexts().containsKey(this.selectedRarity)) {
                continue;
            }
            if (terms.length > 0) {
                boolean matched = true;
                for (String term : terms) {
                    if (!entry.searchBlob().contains(term)) {
                        matched = false;
                        break;
                    }
                }
                if (!matched) continue;
            }
            this.filteredAffixes.add(entry);
        }

        if (this.selectedAffix != null && !this.filteredAffixes.contains(this.selectedAffix)) {
            this.selectedAffix = this.filteredAffixes.isEmpty() ? null : this.filteredAffixes.get(0);
            this.detailScroll = 0;
        } else if (this.selectedAffix == null && !this.filteredAffixes.isEmpty()) {
            this.selectedAffix = this.filteredAffixes.get(0);
        }

        clampScrolls();
    }

    private int countAffixesForCategory(CategoryInfo catInfo) {
        int count = 0;
        for (AffixEntry entry : this.allAffixes) {
            if (this.selectedType != null && entry.type() != this.selectedType) {
                continue;
            }
            if (this.selectedRarity != null && !entry.rarityTexts().containsKey(this.selectedRarity)) {
                continue;
            }
            if (catInfo.isAll() || entry.categories().contains(catInfo)) {
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // 4. 界面渲染
    // =========================================================================

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xDF0E1018, 0xEC090A10);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(this.font, Component.literal("✦ 神化词缀池图鉴").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 10, 12, 0xFFFFFFFF, true);

        String catLabel = this.selectedCategory != null ? this.selectedCategory.displayName().getString() : "全部";
        String countStr = catLabel + " · " + this.filteredAffixes.size() + " / " + this.allAffixes.size() + " 条";
        g.drawString(this.font, countStr, 10, 34, 0xFF9EA4BA, false);

        if (System.currentTimeMillis() < this.copyToastUntilMs) {
            Component toast = Component.literal("✔ 已复制词缀 ID").withStyle(ChatFormatting.GREEN);
            g.drawString(this.font, toast, this.width - 165, 34, 0xFFFFFFFF, true);
        }

        renderCategorySidebar(g, mouseX, mouseY);
        renderAffixPoolList(g, mouseX, mouseY);
        if (this.showRightPanel) {
            renderRightDetailPanel(g, mouseX, mouseY);
        }

        renderHoverTooltips(g, mouseX, mouseY);
    }

    private void renderCategorySidebar(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(this.leftX, this.bodyTop, this.leftX + this.leftW, this.bodyBottom, 0x9912141E);
        g.renderOutline(this.leftX, this.bodyTop, this.leftW, this.bodyBottom - this.bodyTop, 0xFF33384D);

        g.enableScissor(this.leftX + 1, this.bodyTop + 1, this.leftX + this.leftW - 1, this.bodyBottom - 1);

        int y = (int) (this.bodyTop + 3 - this.categoryScroll);
        for (CategoryInfo cat : this.allCategories) {
            int rowX = this.leftX + 3;
            int rowW = this.leftW - 6;

            if (y + CAT_ROW_H >= this.bodyTop && y <= this.bodyBottom) {
                boolean selected = cat.equals(this.selectedCategory);
                boolean hovered = mouseX >= rowX && mouseX < rowX + rowW
                        && mouseY >= Math.max(y, this.bodyTop) && mouseY < Math.min(y + CAT_ROW_H, this.bodyBottom);

                int bg = selected ? 0xCC2C2545 : (hovered ? 0xAA202433 : 0x55181B26);
                int border = selected ? 0xFF9E7BFF : (hovered ? 0xFF4D5470 : 0x332D3245);

                g.fill(rowX, y, rowX + rowW, y + CAT_ROW_H - 2, bg);
                g.renderOutline(rowX, y, rowW, CAT_ROW_H - 2, border);

                g.renderFakeItem(cat.icon(), rowX + 3, y + 3);

                int count = countAffixesForCategory(cat);
                String countText = String.valueOf(count);
                int countW = this.font.width(countText);
                int maxNameW = rowW - 24 - countW - 8;
                String nameStr = trimToWidth(cat.displayName().getString(), maxNameW);

                g.drawString(this.font, nameStr, rowX + 22, y + 7, selected ? 0xFFFFE599 : 0xFFE2E6F3, true);
                g.drawString(this.font, countText, rowX + rowW - countW - 5, y + 7, selected ? 0xFFB899FF : 0xFF838AA3, false);
            }
            y += CAT_ROW_H;
        }

        g.disableScissor();
    }

    private void renderAffixPoolList(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(this.centerX, this.bodyTop, this.centerX + this.centerW, this.bodyBottom, 0x8810121B);
        g.renderOutline(this.centerX, this.bodyTop, this.centerW, this.bodyBottom - this.bodyTop, 0xFF33384D);

        this.hoveredAffix = null;

        if (this.filteredAffixes.isEmpty()) {
            Component empty = Component.literal("当前筛选条件下没有匹配的词缀").withStyle(ChatFormatting.GRAY);
            int tx = this.centerX + (this.centerW - this.font.width(empty)) / 2;
            int ty = this.bodyTop + (this.bodyBottom - this.bodyTop) / 2 - 4;
            g.drawString(this.font, empty, tx, ty, 0xFFFFFFFF, false);
            return;
        }

        boolean needsScrollbar = getAffixContentHeight() > (this.bodyBottom - this.bodyTop - 6);
        int cardW = this.centerW - (needsScrollbar ? 14 : 8);
        int cardX = this.centerX + 4;

        g.enableScissor(this.centerX + 1, this.bodyTop + 1, this.centerX + this.centerW - 1, this.bodyBottom - 1);

        int y = (int) (this.bodyTop + 4 - this.affixScroll);
        for (AffixEntry entry : this.filteredAffixes) {
            if (y + AFFIX_CARD_H >= this.bodyTop && y <= this.bodyBottom) {
                boolean selected = entry.equals(this.selectedAffix);
                boolean hovered = mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= Math.max(y, this.bodyTop) && mouseY < Math.min(y + AFFIX_CARD_H, this.bodyBottom);
                if (hovered) {
                    this.hoveredAffix = entry;
                }

                RarityText preview = entry.textFor(this.selectedRarity);
                int accentColor = preview != null ? preview.colorRgb() : 0xAAAAAA;

                int bg = selected ? 0xD8232036 : (hovered ? 0xCC1E2230 : 0xAA151822);
                int border = selected ? (0xFF000000 | accentColor) : (hovered ? 0xFF6B7394 : 0xFF2B3042);

                g.fill(cardX, y, cardX + cardW, y + AFFIX_CARD_H, bg);
                g.renderOutline(cardX, y, cardW, AFFIX_CARD_H, border);
                g.fill(cardX + 1, y + 1, cardX + 3, y + AFFIX_CARD_H - 1, 0xFF000000 | accentColor);

                int iconBoxX = cardX + 7;
                int iconBoxY = y + 6;
                g.fill(iconBoxX, iconBoxY, iconBoxX + 24, iconBoxY + 24, 0x880B0D14);
                g.renderOutline(iconBoxX, iconBoxY, 24, 24, 0x5558607A);
                renderVisualIcon(g, entry.visual(), iconBoxX + 4, iconBoxY + 4, 16);

                int textX = iconBoxX + 29;
                int rightLimit = cardX + cardW - 6;

                Component typeBadge = getTypeBadge(entry.type());
                int typeBadgeW = this.font.width(typeBadge);
                int modW = this.font.width(entry.modName());
                int dotsW = this.allRarities.size() * 6 + 4;

                int maxTitleW = Math.max(40, rightLimit - textX - typeBadgeW - modW - dotsW - 14);
                String titleStr = trimToWidth(entry.title().getString(), maxTitleW);
                g.drawString(this.font, titleStr, textX, y + 5, 0xFF000000 | accentColor, true);

                int badgeX = textX + this.font.width(titleStr) + 5;
                g.drawString(this.font, typeBadge, badgeX, y + 5, 0xFFFFFFFF, false);

                // 若含法术/药水说明，加一个小角标提示
                int extraTagX = badgeX + typeBadgeW + 3;
                if (entry.spellDetails() != null && !entry.spellDetails().guideText().isEmpty()) {
                    Component tag = Component.literal("✦").withStyle(ChatFormatting.LIGHT_PURPLE);
                    if (extraTagX + 8 < rightLimit - modW - dotsW) {
                        g.drawString(this.font, tag, extraTagX, y + 5, 0xFFFFFFFF, false);
                    }
                } else if (entry.mobEffectDetails() != null && !entry.mobEffectDetails().description().isEmpty()) {
                    Component tag = Component.literal("✦").withStyle(ChatFormatting.YELLOW);
                    if (extraTagX + 8 < rightLimit - modW - dotsW) {
                        g.drawString(this.font, tag, extraTagX, y + 5, 0xFFFFFFFF, false);
                    }
                }

                int modX = rightLimit - modW;
                g.drawString(this.font, entry.modName(), modX, y + 5, 0xFF7D859E, false);

                int dotStartX = modX - dotsW - 4;
                if (dotStartX > extraTagX + 8) {
                    int dx = dotStartX;
                    for (LootRarity r : this.allRarities) {
                        boolean has = entry.rarityTexts().containsKey(r);
                        int c = has ? (0xFF000000 | getRarityColorRgb(r)) : 0x33404558;
                        g.fill(dx, y + 7, dx + 4, y + 11, c);
                        dx += 6;
                    }
                }

                String descText = preview != null ? preview.augmentingText().getString() : entry.id().toString();
                String trimmedDesc = trimToWidth(descText, rightLimit - textX);
                g.drawString(this.font, trimmedDesc, textX, y + 18, 0xFFE6E9F2, false);

                StringBuilder meta = new StringBuilder();
                if (entry.spellDetails() != null) {
                    meta.append("[").append(entry.spellDetails().schoolName().getString()).append("] ");
                } else if (entry.mobEffectDetails() != null) {
                    meta.append(entry.mobEffectDetails().category() == MobEffectCategory.HARMFUL ? "[减益] " : "[增益] ");
                }
                meta.append("权重 ").append(entry.baseWeight());
                if (!entry.categories().isEmpty()) {
                    meta.append(" · ");
                    for (int i = 0; i < Math.min(2, entry.categories().size()); i++) {
                        if (i > 0) meta.append("/");
                        meta.append(entry.categories().get(i).displayName().getString());
                    }
                    if (entry.categories().size() > 2) {
                        meta.append("+").append(entry.categories().size() - 2);
                    }
                }
                meta.append(" · ").append(entry.id());
                String metaStr = trimToWidth(meta.toString(), rightLimit - textX);
                g.drawString(this.font, metaStr, textX, y + 33, 0xFF787E96, false);
            }
            y += AFFIX_CARD_H + AFFIX_CARD_GAP;
        }

        g.disableScissor();

        if (needsScrollbar) {
            int trackX = this.centerX + this.centerW - 7;
            int trackTop = this.bodyTop + 3;
            int trackBottom = this.bodyBottom - 3;
            int trackH = trackBottom - trackTop;
            g.fill(trackX, trackTop, trackX + 4, trackBottom, 0x55222638);

            int viewH = this.bodyBottom - this.bodyTop - 6;
            int contentH = getAffixContentHeight();
            int thumbH = Math.max(18, (int) ((double) viewH / contentH * trackH));
            double ratio = this.affixScroll / Math.max(1, contentH - viewH);
            int thumbY = trackTop + (int) (ratio * (trackH - thumbH));
            g.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xAA8E7CE6);
        }
    }

    private void renderRightDetailPanel(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(this.rightX, this.bodyTop, this.rightX + this.rightW, this.bodyBottom, 0x9912141E);
        g.renderOutline(this.rightX, this.bodyTop, this.rightW, this.bodyBottom - this.bodyTop, 0xFF33384D);

        AffixEntry target = this.selectedAffix != null ? this.selectedAffix : this.hoveredAffix;
        if (this.copyIdButton != null) {
            this.copyIdButton.visible = target != null;
        }
        if (target == null) {
            g.drawString(this.font, "点击左侧任意词缀查看详情", this.rightX + 10, this.bodyTop + 14, 0xFF888FA6, false);
            return;
        }

        int padX = this.rightX + 8;
        int maxW = this.rightW - 20; // 右侧留 6px 给滚动条

        RarityText topText = target.textFor(this.selectedRarity);
        int accent = topText != null ? topText.colorRgb() : 0xFFD54F;

        g.fill(padX, this.bodyTop + 6, padX + 26, this.bodyTop + 32, 0xAA0B0D14);
        g.renderOutline(padX, this.bodyTop + 6, 26, 26, 0xFF000000 | accent);
        renderVisualIcon(g, target.visual(), padX + 5, this.bodyTop + 11, 16);

        String titleTrim = trimToWidth(target.title().getString(), maxW - 30);
        g.drawString(this.font, titleTrim, padX + 32, this.bodyTop + 8, 0xFF000000 | accent, true);
        g.drawString(this.font, getTypeBadge(target.type()), padX + 32, this.bodyTop + 21, 0xFFFFFFFF, false);
        int modW = this.font.width(target.modName());
        g.drawString(this.font, target.modName(), this.rightX + this.rightW - modW - 8, this.bodyTop + 21, 0xFF9EA4BA, false);

        String idTrim = trimToWidth(target.id().toString(), maxW - 54);
        g.drawString(this.font, idTrim, padX, this.bodyTop + 38, 0xFF7D849E, false);

        int contentTop = this.bodyTop + 53;
        g.fill(padX, contentTop - 2, this.rightX + this.rightW - 8, contentTop - 1, 0x554B526E);

        g.enableScissor(this.rightX + 1, contentTop, this.rightX + this.rightW - 1, this.bodyBottom - 1);
        int startY = (int) (contentTop + 4 - this.detailScroll);
        int y = startY;

        // ---- A. 适用战利品图标排 ----
        g.drawString(this.font, "适用战利品:", padX, y, 0xFFB0B7CE, false);
        y += 12;
        if (target.categories().isEmpty()) {
            g.drawString(this.font, "动态条件判定 / 特殊物品", padX, y, 0xFF7D849E, false);
            y += 14;
        } else {
            int ix = padX;
            for (CategoryInfo ci : target.categories()) {
                if (ix + 18 > padX + maxW) {
                    ix = padX;
                    y += 20;
                }
                g.fill(ix, y, ix + 18, y + 18, 0x661C2030);
                g.renderOutline(ix, y, 18, 18, 0x44565E7A);
                g.renderFakeItem(ci.icon(), ix + 1, y + 1);
                ix += 20;
            }
            y += 24;
        }

        // ---- B. 触发方式（能力/药水/法术触发） ----
        String trigger = target.mobEffectDetails() != null && target.mobEffectDetails().triggerTargetLabel() != null
                ? target.mobEffectDetails().triggerTargetLabel()
                : target.abilityTriggerLabel();
        if (trigger != null && !trigger.isBlank()) {
            y = drawInfoCard(g, padX, y, maxW, 0xFF64B5F6, List.of(
                    Component.literal("触发机制: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(trigger).withStyle(ChatFormatting.WHITE))
            ));
        }

        // ---- C. 具体法术百科（JEI Guide + 学派 + 蓝耗/冷却/吟唱/独有数值） ----
        if (target.spellDetails() != null) {
            SpellDetails sd = target.spellDetails();
            List<Component> spellLines = new ArrayList<>();
            spellLines.add(Component.literal("✦ 法术图鉴: ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(sd.spellName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  [").withStyle(ChatFormatting.DARK_GRAY))
                    .append(sd.schoolName().copy().withStyle(s -> s.withColor(TextColor.fromRgb(sd.schoolColorRgb()))))
                    .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY)));

            if (!sd.guideText().isBlank()) {
                spellLines.add(Component.literal(sd.guideText()).withStyle(ChatFormatting.GRAY));
            }
            spellLines.add(Component.literal("施法: " + sd.castTypeLabel() + "  |  等级: 1~" + sd.maxLevel())
                    .withStyle(ChatFormatting.AQUA));
            spellLines.add(Component.literal("法力: " + sd.manaCostSummary() + "  |  冷却: " + sd.cooldownSummary())
                    .withStyle(ChatFormatting.BLUE));
            if (!sd.castTimeSummary().isBlank()) {
                spellLines.add(Component.literal("吟唱: " + sd.castTimeSummary()).withStyle(ChatFormatting.YELLOW));
            }
            for (Component u : sd.uniqueStats()) {
                spellLines.add(Component.literal("• ").withStyle(ChatFormatting.DARK_PURPLE)
                        .append(u.copy().withStyle(ChatFormatting.GRAY)));
            }
            y = drawInfoCard(g, padX, y, maxW, sd.schoolColorRgb(), spellLines);
        }

        // ---- D. 药水效果详情（JEED 描述 + 属性影响 + 叠加规则） ----
        if (target.mobEffectDetails() != null) {
            MobEffectDetails med = target.mobEffectDetails();
            int catColor = switch (med.category()) {
                case BENEFICIAL -> 0x4CAF50;
                case HARMFUL -> 0xEF5350;
                case NEUTRAL -> 0x9E9E9E;
            };
            String catTag = switch (med.category()) {
                case BENEFICIAL -> "[增益]";
                case HARMFUL -> "[减益]";
                case NEUTRAL -> "[中性]";
            };

            List<Component> fxLines = new ArrayList<>();
            MutableComponent header = Component.literal("✦ 效果详情: ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(med.effectName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" " + catTag).withStyle(s -> s.withColor(TextColor.fromRgb(catColor))));
            if (med.instantaneous()) {
                header.append(Component.literal(" [瞬时]").withStyle(ChatFormatting.AQUA));
            }
            fxLines.add(header);

            if (!med.description().isBlank()) {
                fxLines.add(Component.literal(med.description()).withStyle(ChatFormatting.GRAY));
            } else {
                fxLines.add(Component.literal("效果 ID: " + med.effectId()).withStyle(ChatFormatting.DARK_GRAY));
            }

            if (med.stackingLimit() > 0) {
                fxLines.add(Component.literal("✦ 重复触发可提升效果等级（上限 " + med.stackingLimit() + " 层）")
                        .withStyle(ChatFormatting.GOLD));
            }

            if (!med.attributeModifiers().isEmpty()) {
                fxLines.add(Component.literal("附带属性变化 (I级):").withStyle(ChatFormatting.AQUA));
                for (Component modLine : med.attributeModifiers()) {
                    fxLines.add(Component.literal("  • ").withStyle(ChatFormatting.GRAY).append(modLine));
                }
            }
            y = drawInfoCard(g, padX, y, maxW, catColor, fxLines);
        }

        // ---- E. 属性计算与说明（Apothic Attributes .desc） ----
        if (target.attributeDetails() != null) {
            AttributeDetails ad = target.attributeDetails();
            List<Component> attrLines = new ArrayList<>();
            attrLines.add(Component.literal("✦ 属性机制: ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                    .append(ad.attributeName().copy().withStyle(ChatFormatting.WHITE)));
            if (ad.operationLabel() != null) {
                attrLines.add(Component.literal("运算区: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(ad.operationLabel()).withStyle(ChatFormatting.YELLOW)));
            }
            if (!ad.description().isBlank()) {
                attrLines.add(Component.literal(ad.description()).withStyle(ChatFormatting.GRAY));
            }
            y = drawInfoCard(g, padX, y, maxW, 0x4DD0E1, attrLines);
        }

        // ---- F. 全稀有度数值阶梯 ----
        g.drawString(this.font, "各稀有度数值:", padX, y, 0xFFB0B7CE, false);
        y += 12;
        for (RarityText rt : target.rarityTexts().values()) {
            Component rName = Component.literal("[" + getRarityShortLabel(rt.rarity()) + "] ")
                    .withStyle(style -> style.withColor(TextColor.fromRgb(rt.colorRgb())).withBold(true));
            MutableComponent fullLine = rName.copy().append(rt.augmentingText().copy().withStyle(ChatFormatting.WHITE));

            String augS = rt.augmentingText().getString().trim();
            String minS = rt.minText().getString().trim();
            String maxS = rt.maxText().getString().trim();
            if (!minS.isEmpty() && !maxS.isEmpty() && !minS.equals(maxS) && augS.equals(maxS)) {
                fullLine.append(Component.literal("\n  区间: " + minS + " ~ " + maxS).withStyle(ChatFormatting.GRAY));
            }

            List<FormattedCharSequence> wrapped = this.font.split(fullLine, maxW - 8);
            int boxH = wrapped.size() * 10 + 6;
            g.fill(padX, y, padX + maxW, y + boxH, 0x66181C2B);
            g.fill(padX, y, padX + 2, y + boxH, 0xFF000000 | rt.colorRgb());
            int ly = y + 3;
            for (FormattedCharSequence line : wrapped) {
                g.drawString(this.font, line, padX + 5, ly, 0xFFFFFFFF, false);
                ly += 10;
            }
            y += boxH + 3;
        }

        // ---- G. 世界层级 (WorldTier) 权重分布 ----
        y += 3;
        g.drawString(this.font, "世界层级掉落权重 (Ctrl+T):", padX, y, 0xFFB0B7CE, false);
        y += 12;
        if (!target.tierWeights().isEmpty()) {
            int cols = target.tierWeights().size();
            int gap = 2;
            int cellW = Math.max(26, (maxW - gap * (cols - 1)) / cols);
            int cx = padX;
            for (WorldTierWeight tw : target.tierWeights()) {
                boolean active = tw.weight() > 0;
                int bg = active ? 0x881C2234 : 0x5512141C;
                int border = active ? (0xAA000000 | tw.colorRgb()) : 0x44353A4D;
                g.fill(cx, y, cx + cellW, y + 22, bg);
                g.renderOutline(cx, y, cellW, 22, border);

                int labelColor = active ? (0xFF000000 | tw.colorRgb()) : 0xFF555B70;
                int lw = this.font.width(tw.shortLabel());
                g.drawString(this.font, tw.shortLabel(), cx + (cellW - lw) / 2, y + 2, labelColor, false);

                String val = active ? String.valueOf(tw.weight()) : "×";
                int vw = this.font.width(val);
                g.drawString(this.font, val, cx + (cellW - vw) / 2, y + 12, active ? 0xFFEAEAF5 : 0xFF4F5568, false);
                cx += cellW + gap;
            }
            y += 26;
        } else {
            g.drawString(this.font, "权重: " + target.baseWeight() + "  |  品质: " + target.baseQuality(), padX, y, 0xFF9AA1BA, false);
            y += 12;
        }

        // ---- H. 互斥词缀组 ----
        if (!target.exclusiveSet().isEmpty()) {
            y += 2;
            g.drawString(this.font, "互斥词缀 / 互斥组:", padX, y, 0xFFE57373, false);
            y += 11;
            for (String ex : target.exclusiveSet()) {
                String displayEx = formatExclusiveName(ex);
                for (FormattedCharSequence line : this.font.split(Component.literal("• " + displayEx), maxW - 4)) {
                    g.drawString(this.font, line, padX + 2, y, 0xFFCF8A8A, false);
                    y += 10;
                }
            }
        }

        this.lastDetailContentHeight = Math.max(0, y - startY + 8);
        g.disableScissor();

        // 右侧详情面板滚动条
        int detailViewH = this.bodyBottom - contentTop - 4;
        if (this.lastDetailContentHeight > detailViewH) {
            int trackX = this.rightX + this.rightW - 6;
            int trackTop = contentTop + 2;
            int trackBottom = this.bodyBottom - 3;
            int trackH = trackBottom - trackTop;
            g.fill(trackX, trackTop, trackX + 3, trackBottom, 0x55222638);

            int thumbH = Math.max(16, (int) ((double) detailViewH / this.lastDetailContentHeight * trackH));
            double ratio = this.detailScroll / Math.max(1, this.lastDetailContentHeight - detailViewH);
            int thumbY = trackTop + (int) (ratio * (trackH - thumbH));
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xAA8E7CE6);
        }
    }

    private int drawInfoCard(GuiGraphics g, int x, int y, int maxW, int accentRgb, List<Component> lines) {
        List<FormattedCharSequence> wrappedAll = new ArrayList<>();
        for (Component c : lines) {
            wrappedAll.addAll(this.font.split(c, maxW - 10));
        }
        int boxH = wrappedAll.size() * 10 + 7;
        g.fill(x, y, x + maxW, y + boxH, 0x77161B29);
        g.renderOutline(x, y, maxW, boxH, 0x44454E6B);
        g.fill(x, y, x + 2, y + boxH, 0xFF000000 | accentRgb);

        int ly = y + 4;
        for (FormattedCharSequence line : wrappedAll) {
            g.drawString(this.font, line, x + 5, ly, 0xFFFFFFFF, false);
            ly += 10;
        }
        return y + boxH + 5;
    }

    private void renderHoverTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX >= this.leftX && mouseX < this.leftX + this.leftW && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            int idx = (int) ((mouseY - (this.bodyTop + 3) + this.categoryScroll) / CAT_ROW_H);
            if (idx >= 0 && idx < this.allCategories.size()) {
                CategoryInfo ci = this.allCategories.get(idx);
                List<Component> tips = new ArrayList<>();
                tips.add(ci.displayName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                tips.add(Component.literal("ID: " + ci.id()).withStyle(ChatFormatting.DARK_GRAY));
                tips.add(Component.literal("包含词缀: " + countAffixesForCategory(ci) + " 条").withStyle(ChatFormatting.GRAY));
                g.renderTooltip(this.font, tips, Optional.empty(), mouseX, mouseY);
                return;
            }
        }

        if (this.showRightPanel && this.selectedAffix != null && !this.selectedAffix.categories().isEmpty()) {
            int padX = this.rightX + 8;
            int maxW = this.rightW - 20;
            int contentTop = this.bodyTop + 53;
            int iconStartY = (int) (contentTop + 4 - this.detailScroll) + 12;
            if (mouseY >= Math.max(contentTop, iconStartY) && mouseY < this.bodyBottom) {
                int ix = padX;
                int iy = iconStartY;
                for (CategoryInfo ci : this.selectedAffix.categories()) {
                    if (ix + 18 > padX + maxW) {
                        ix = padX;
                        iy += 20;
                    }
                    if (mouseX >= ix && mouseX < ix + 18 && mouseY >= iy && mouseY < iy + 18) {
                        g.renderTooltip(this.font, List.of(
                                ci.displayName().copy().withStyle(ChatFormatting.AQUA),
                                Component.literal(ci.id().toString()).withStyle(ChatFormatting.DARK_GRAY)
                        ), Optional.empty(), mouseX, mouseY);
                        return;
                    }
                    ix += 20;
                }
            }
        }

        if (this.hoveredAffix != null) {
            List<Component> tips = new ArrayList<>();
            RarityText active = this.hoveredAffix.textFor(this.selectedRarity);
            int color = active != null ? active.colorRgb() : 0xFFFFFF;
            tips.add(this.hoveredAffix.title().copy().withStyle(s -> s.withColor(TextColor.fromRgb(color)).withBold(true)));
            tips.add(Component.literal("ID: " + this.hoveredAffix.id()).withStyle(ChatFormatting.DARK_GRAY));

            if (this.hoveredAffix.spellDetails() != null) {
                SpellDetails sd = this.hoveredAffix.spellDetails();
                tips.add(Component.literal("法术: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(sd.spellName()).append(" (" + sd.schoolName().getString() + ")"));
                if (!sd.guideText().isBlank()) {
                    for (FormattedCharSequence line : this.font.split(Component.literal(sd.guideText()).withStyle(ChatFormatting.GRAY), 220)) {
                        tips.add(Component.literal(toPlainString(line)).withStyle(ChatFormatting.GRAY));
                    }
                }
            } else if (this.hoveredAffix.mobEffectDetails() != null) {
                MobEffectDetails med = this.hoveredAffix.mobEffectDetails();
                if (!med.description().isBlank()) {
                    for (FormattedCharSequence line : this.font.split(Component.literal(med.description()).withStyle(ChatFormatting.GRAY), 220)) {
                        tips.add(Component.literal(toPlainString(line)).withStyle(ChatFormatting.GRAY));
                    }
                }
            } else if (this.hoveredAffix.attributeDetails() != null && !this.hoveredAffix.attributeDetails().description().isBlank()) {
                for (FormattedCharSequence line : this.font.split(Component.literal(this.hoveredAffix.attributeDetails().description()).withStyle(ChatFormatting.GRAY), 220)) {
                    tips.add(Component.literal(toPlainString(line)).withStyle(ChatFormatting.GRAY));
                }
            }

            for (RarityText rt : this.hoveredAffix.rarityTexts().values()) {
                tips.add(Component.literal("[" + getRarityShortLabel(rt.rarity()) + "] ")
                        .withStyle(s -> s.withColor(TextColor.fromRgb(rt.colorRgb())))
                        .append(rt.augmentingText().copy().withStyle(ChatFormatting.WHITE)));
            }
            tips.add(Component.literal("点击查看完整百科 · Ctrl+点击复制 ID").withStyle(ChatFormatting.YELLOW));
            g.renderTooltip(this.font, tips, Optional.empty(), mouseX, mouseY);
        }
    }

    // =========================================================================
    // 5. 鼠标与键盘交互
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && mouseX >= this.leftX && mouseX < this.leftX + this.leftW && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            int idx = (int) ((mouseY - (this.bodyTop + 3) + this.categoryScroll) / CAT_ROW_H);
            if (idx >= 0 && idx < this.allCategories.size()) {
                this.selectedCategory = this.allCategories.get(idx);
                this.affixScroll = 0;
                this.detailScroll = 0;
                applyFilters();
                return true;
            }
        }

        if (button == 0 && mouseX >= this.centerX + this.centerW - 10 && mouseX <= this.centerX + this.centerW
                && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            this.draggingAffixScrollbar = true;
            updateAffixScrollFromMouse(mouseY);
            return true;
        }

        int contentTop = this.bodyTop + 53;
        if (button == 0 && this.showRightPanel && mouseX >= this.rightX + this.rightW - 10 && mouseX <= this.rightX + this.rightW
                && mouseY >= contentTop && mouseY < this.bodyBottom) {
            this.draggingDetailScrollbar = true;
            updateDetailScrollFromMouse(mouseY);
            return true;
        }

        if (button == 0 && this.hoveredAffix != null) {
            this.selectedAffix = this.hoveredAffix;
            this.detailScroll = 0;
            if (Screen.hasControlDown()) {
                copyAffixId(this.hoveredAffix);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingAffixScrollbar && button == 0) {
            updateAffixScrollFromMouse(mouseY);
            return true;
        }
        if (this.draggingDetailScrollbar && button == 0) {
            updateDetailScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingAffixScrollbar = false;
        this.draggingDetailScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = -scrollY * 22.0;
        if (mouseX >= this.leftX && mouseX < this.leftX + this.leftW && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            this.categoryScroll += delta;
            clampScrolls();
            return true;
        }
        if (mouseX >= this.centerX && mouseX < this.centerX + this.centerW && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            this.affixScroll += delta;
            clampScrolls();
            return true;
        }
        if (this.showRightPanel && mouseX >= this.rightX && mouseX < this.rightX + this.rightW && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
            this.detailScroll += delta;
            clampScrolls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) {
                this.searchBox.setFocused(false);
                return true;
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void updateAffixScrollFromMouse(double mouseY) {
        int trackTop = this.bodyTop + 3;
        int trackBottom = this.bodyBottom - 3;
        int viewH = this.bodyBottom - this.bodyTop - 6;
        int maxScroll = Math.max(0, getAffixContentHeight() - viewH);
        double pct = Mth.clamp((mouseY - trackTop) / Math.max(1.0, trackBottom - trackTop), 0.0, 1.0);
        this.affixScroll = pct * maxScroll;
    }

    private void updateDetailScrollFromMouse(double mouseY) {
        int contentTop = this.bodyTop + 53;
        int trackTop = contentTop + 2;
        int trackBottom = this.bodyBottom - 3;
        int viewH = this.bodyBottom - contentTop - 4;
        int maxScroll = Math.max(0, this.lastDetailContentHeight - viewH);
        double pct = Mth.clamp((mouseY - trackTop) / Math.max(1.0, trackBottom - trackTop), 0.0, 1.0);
        this.detailScroll = pct * maxScroll;
    }

    private int getAffixContentHeight() {
        return Math.max(0, this.filteredAffixes.size() * (AFFIX_CARD_H + AFFIX_CARD_GAP) - AFFIX_CARD_GAP);
    }

    private void clampScrolls() {
        int catViewH = this.bodyBottom - this.bodyTop - 6;
        int catMax = Math.max(0, this.allCategories.size() * CAT_ROW_H - catViewH);
        this.categoryScroll = Mth.clamp(this.categoryScroll, 0, catMax);

        int affixViewH = this.bodyBottom - this.bodyTop - 6;
        int affixMax = Math.max(0, getAffixContentHeight() - affixViewH);
        this.affixScroll = Mth.clamp(this.affixScroll, 0, affixMax);

        int contentTop = this.bodyTop + 53;
        int detailViewH = Math.max(1, this.bodyBottom - contentTop - 4);
        int detailMax = Math.max(0, this.lastDetailContentHeight - detailViewH);
        this.detailScroll = Mth.clamp(this.detailScroll, 0, detailMax);
    }

    private void copyAffixId(AffixEntry entry) {
        Minecraft.getInstance().keyboardHandler.setClipboard(entry.id().toString());
        this.copyToastUntilMs = System.currentTimeMillis() + 2000L;
    }

    // =========================================================================
    // 6. 百科详情提取（法术 Guide / JEED 药水详情 / 属性机制 / 世界层级）
    // =========================================================================

    @Nullable
    private static SpellDetails resolveSpellDetails(Affix affix, ResourceLocation affixId, @Nullable Player player) {
        ResourceLocation spellId = extractSpellId(affix, affixId);
        if (spellId == null) {
            return null;
        }

        Component spellName = translateFirst(
                "spell." + spellId.getNamespace() + "." + spellId.getPath(),
                prettify(spellId.getPath())
        );
        String guideText = readTranslationOrEmpty(
                "spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide",
                "spell." + spellId.getNamespace() + "." + spellId.getPath() + ".description"
        );

        Component schoolName = Component.literal("法术");
        int schoolColor = 0xCE93D8;
        String castTypeLabel = "未知";
        int minLevel = 1;
        int maxLevel = 10;
        String manaSummary = "-";
        String cdSummary = "-";
        String castTimeSummary = "";
        List<Component> uniqueStats = new ArrayList<>();

        try {
            Class<?> regClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Object spellObj = null;
            try {
                Method getSpell = regClass.getMethod("getSpell", ResourceLocation.class);
                spellObj = getSpell.invoke(null, spellId);
            } catch (NoSuchMethodException ignored) {
                try {
                    Method getSpellStr = regClass.getMethod("getSpell", String.class);
                    spellObj = getSpellStr.invoke(null, spellId.toString());
                } catch (NoSuchMethodException ignored2) {}
            }

            if (spellObj != null) {
                // 法术名称
                try {
                    Object res = spellObj.getClass().getMethod("getDisplayName", Player.class).invoke(spellObj, player);
                    if (res instanceof Component c && !c.getString().isBlank()) {
                        spellName = c;
                    }
                } catch (Throwable ignored) {}

                // 所属学派与学派颜色
                try {
                    Object school = spellObj.getClass().getMethod("getSchoolType").invoke(spellObj);
                    if (school != null) {
                        Object sName = school.getClass().getMethod("getDisplayName").invoke(school);
                        if (sName instanceof Component c) {
                            schoolName = c;
                            TextColor tc = c.getStyle().getColor();
                            if (tc != null) {
                                schoolColor = tc.getValue();
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                // 等级范围
                try {
                    minLevel = ((Number) spellObj.getClass().getMethod("getMinLevel").invoke(spellObj)).intValue();
                    maxLevel = ((Number) spellObj.getClass().getMethod("getMaxLevel").invoke(spellObj)).intValue();
                } catch (Throwable ignored) {}

                // 施法类型
                try {
                    Object ct = spellObj.getClass().getMethod("getCastType").invoke(spellObj);
                    if (ct != null) {
                        castTypeLabel = switch (ct.toString().toUpperCase(Locale.ROOT)) {
                            case "INSTANT" -> "瞬发";
                            case "LONG" -> "长按吟唱";
                            case "CONTINUOUS" -> "持续引导";
                            default -> ct.toString();
                        };
                    }
                } catch (Throwable ignored) {}

                // 蓝耗 (1级 ~ 满级)
                try {
                    Method mMana = spellObj.getClass().getMethod("getManaCost", int.class);
                    int m1 = ((Number) mMana.invoke(spellObj, minLevel)).intValue();
                    int mMax = ((Number) mMana.invoke(spellObj, maxLevel)).intValue();
                    manaSummary = m1 == mMax ? String.valueOf(m1) : (m1 + "~" + mMax);
                } catch (Throwable ignored) {}

                // 冷却 (ticks -> s)
                try {
                    int cdTicks = ((Number) spellObj.getClass().getMethod("getSpellCooldown").invoke(spellObj)).intValue();
                    cdSummary = String.format(Locale.ROOT, "%.1fs", cdTicks / 20.0);
                } catch (Throwable ignored) {}

                // 吟唱时间
                try {
                    Method mCt = spellObj.getClass().getMethod("getCastTime", int.class);
                    int ct1 = ((Number) mCt.invoke(spellObj, minLevel)).intValue();
                    int ctMax = ((Number) mCt.invoke(spellObj, maxLevel)).intValue();
                    if (ct1 > 0 || ctMax > 0) {
                        castTimeSummary = ct1 == ctMax
                                ? String.format(Locale.ROOT, "%.1fs", ct1 / 20.0)
                                : String.format(Locale.ROOT, "%.1fs~%.1fs", ct1 / 20.0, ctMax / 20.0);
                    }
                } catch (Throwable ignored) {}

                // 法术独有属性列表 (getUniqueInfo)
                try {
                    Method mUnique = spellObj.getClass().getMethod("getUniqueInfo", int.class, LivingEntity.class);
                    Object listObj = mUnique.invoke(spellObj, minLevel, player);
                    if (listObj instanceof Collection<?> col) {
                        for (Object o : col) {
                            if (o instanceof Component c && !c.getString().isBlank()) {
                                uniqueStats.add(c);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        return new SpellDetails(
                spellId,
                spellName,
                schoolName,
                schoolColor,
                guideText,
                castTypeLabel,
                minLevel,
                maxLevel,
                manaSummary,
                cdSummary,
                castTimeSummary,
                uniqueStats
        );
    }

    @Nullable
    private static MobEffectDetails resolveMobEffectDetails(Affix affix) {
        Holder<MobEffect> holder = extractMobEffectHolder(affix);
        if (holder == null) {
            return null;
        }
        MobEffect effect = holder.value();
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (effectId == null) {
            effectId = holder.unwrapKey().map(k -> k.location()).orElse(ResourceLocation.parse("minecraft:empty"));
        }

        // JEED / 原版 / 模组语言键效果说明
        String descKey = effect.getDescriptionId();
        String description = readTranslationOrEmpty(
                descKey + ".description",
                descKey + ".desc",
                "effect." + effectId.getNamespace() + "." + effectId.getPath() + ".description",
                "mob_effect." + effectId.getNamespace() + "." + effectId.getPath() + ".description"
        );

        // 读取效果自身带来的属性加成 (I级)
        List<Component> attrMods = new ArrayList<>();
        try {
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer = (attrHolder, mod) -> {
                String attrName = Component.translatable(attrHolder.value().getDescriptionId()).getString();
                double amt = mod.amount();
                String valStr;
                if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    valStr = String.format(Locale.ROOT, "%s%.2f", amt >= 0 ? "+" : "", amt);
                    if (valStr.endsWith(".00")) valStr = valStr.substring(0, valStr.length() - 3);
                } else {
                    valStr = String.format(Locale.ROOT, "%s%.0f%%", amt >= 0 ? "+" : "", amt * 100.0);
                }
                ChatFormatting fmt = amt >= 0 ? ChatFormatting.BLUE : ChatFormatting.RED;
                attrMods.add(Component.literal(attrName + " " + valStr).withStyle(fmt));
            };
            effect.createModifiers(0, consumer);
        } catch (Throwable ignored) {}

        int stackingLimit = 0;
        try {
            boolean stackOnReapply = false;
            Class<?> cls = affix.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    String fn = f.getName().toLowerCase(Locale.ROOT);
                    if (fn.contains("stackonreapply")) {
                        f.setAccessible(true);
                        Object v = f.get(affix);
                        if (v instanceof Boolean b) stackOnReapply = b;
                    } else if (fn.contains("stackinglimit")) {
                        f.setAccessible(true);
                        Object v = f.get(affix);
                        if (v instanceof Number n) stackingLimit = n.intValue();
                    }
                }
                cls = cls.getSuperclass();
            }
            if (!stackOnReapply) {
                stackingLimit = 0;
            }
        } catch (Throwable ignored) {}

        return new MobEffectDetails(
                effectId,
                effect.getDisplayName(),
                effect.getCategory(),
                effect.isInstantenous(),
                description,
                attrMods,
                resolveTriggerTargetLabel(affix),
                stackingLimit
        );
    }

    @Nullable
    private static AttributeDetails resolveAttributeDetails(Affix affix) {
        Holder<Attribute> holder = extractAttributeHolder(affix);
        if (holder == null) {
            return null;
        }
        Attribute attr = holder.value();
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
        if (id == null) {
            id = holder.unwrapKey().map(k -> k.location()).orElse(ResourceLocation.parse("minecraft:generic"));
        }

        String descKey = attr.getDescriptionId();
        String description = readTranslationOrEmpty(
                descKey + ".desc",
                descKey + ".description",
                "attribute.name." + id.getNamespace() + "." + id.getPath() + ".desc"
        );

        String opLabel = null;
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() == AttributeModifier.Operation.class) {
                    try {
                        f.setAccessible(true);
                        AttributeModifier.Operation op = (AttributeModifier.Operation) f.get(affix);
                        if (op != null) {
                            opLabel = switch (op) {
                                case ADD_VALUE -> "直接加算 (+固定值)";
                                case ADD_MULTIPLIED_BASE -> "基础倍率 (+基础值 × %)";
                                case ADD_MULTIPLIED_TOTAL -> "最终乘区 (+总值 × %)";
                            };
                        }
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }

        return new AttributeDetails(
                id,
                Component.translatable(descKey),
                opLabel,
                description
        );
    }

    @Nullable
    private static String resolveTriggerTargetLabel(Affix affix) {
        String rawTarget = null;
        String rawTrigger = null;

        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String fn = f.getName().toLowerCase(Locale.ROOT);
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val == null) continue;
                    if ("target".equals(fn)) {
                        rawTarget = val.toString();
                    } else if ("trigger".equals(fn)) {
                        rawTrigger = val.toString();
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }

        if (rawTrigger != null) {
            String tStr = switch (rawTrigger.toLowerCase(Locale.ROOT)) {
                case "hurt" -> "受到攻击时";
                case "melee_hit" -> "近战击中目标时";
                case "projectile_hit" -> "弹射物击中目标时";
                case "shield_block" -> "盾牌格挡时";
                case "spell_damage" -> "造成法术伤害时";
                case "spell_heal" -> "进行法术治疗时";
                default -> rawTrigger;
            };
            String targetStr = rawTarget != null ? switch (rawTarget.toLowerCase(Locale.ROOT)) {
                case "self" -> "对自身施放";
                case "target" -> "对目标施放";
                default -> rawTarget;
            } : "";
            return targetStr.isEmpty() ? tStr : (tStr + " → " + targetStr);
        }

        if (rawTarget != null) {
            return switch (rawTarget.toLowerCase(Locale.ROOT)) {
                case "attack_target" -> "近战攻击时 → 对目标施加";
                case "attack_self" -> "近战攻击时 → 对自身施加";
                case "hurt_attacker" -> "受到攻击时 → 对攻击者施加";
                case "hurt_self" -> "受到攻击时 → 对自身施加";
                case "arrow_target" -> "弹射物命中时 → 对目标施加";
                case "arrow_self" -> "弹射物命中时 → 对自身施加";
                case "block_attacker" -> "盾牌格挡时 → 对攻击者施加";
                case "block_self" -> "盾牌格挡时 → 对自身施加";
                case "break_self" -> "破坏方块时 → 对自身施加";
                case "spell_damage_target" -> "造成法术伤害时 → 对目标施加";
                case "spell_damage_self" -> "造成法术伤害时 → 对自身施加";
                case "spell_heal_target" -> "进行法术治疗时 → 对被治疗者施加";
                case "spell_heal_self" -> "进行法术治疗时 → 对自身施加";
                default -> rawTarget;
            };
        }
        return null;
    }

    private static List<WorldTierWeight> extractWorldTierWeights(AffixDefinition def) {
        List<WorldTierWeight> result = new ArrayList<>();
        try {
            Object tieredWeights = def.weights();
            Class<?> wtClass = null;
            for (Method m : tieredWeights.getClass().getMethods()) {
                if ("getWeight".equals(m.getName()) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isEnum()) {
                    wtClass = m.getParameterTypes()[0];
                    break;
                }
            }
            if (wtClass == null) {
                return result;
            }
            Method getWeight = tieredWeights.getClass().getMethod("getWeight", wtClass);
            Method getQuality = tieredWeights.getClass().getMethod("getQuality", wtClass);
            Object[] tiers = wtClass.getEnumConstants();
            if (tiers == null) {
                return result;
            }
            for (Object tier : tiers) {
                String name = tier.toString().toLowerCase(Locale.ROOT);
                int w = ((Number) getWeight.invoke(tieredWeights, tier)).intValue();
                float q = ((Number) getQuality.invoke(tieredWeights, tier)).floatValue();
                String shortLabel = switch (name) {
                    case "haven" -> "避风";
                    case "frontier" -> "拓荒";
                    case "ascent" -> "登峰";
                    case "summit" -> "极巅";
                    case "pinnacle" -> "绝顶";
                    default -> prettify(name);
                };
                int color = switch (name) {
                    case "haven" -> 0xA5D6A7;
                    case "frontier" -> 0x81D4FA;
                    case "ascent" -> 0xCE93D8;
                    case "summit" -> 0xFFB74D;
                    case "pinnacle" -> 0xFF7043;
                    default -> 0xE0E0E0;
                };
                result.add(new WorldTierWeight(name, shortLabel, color, w, q));
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private String formatExclusiveName(String raw) {
        try {
            ResourceLocation rl = ResourceLocation.parse(raw);
            Component c = this.titleById.get(rl);
            if (c != null) {
                return c.getString() + " (" + raw + ")";
            }
        } catch (Throwable ignored) {}
        if ("examplemod:damage_conversion".equals(raw)) {
            return "元素伤害转化互斥组 (" + raw + ")";
        }
        return raw;
    }

    // =========================================================================
    // 7. 图标解析与分类扫描
    // =========================================================================

    private static void renderVisualIcon(GuiGraphics g, AffixVisual visual, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (visual.mobEffect() != null) {
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(visual.mobEffect());
            g.blit(x, y, 0, size, size, sprite);
            return;
        }
        if (visual.spellIconTex() != null) {
            g.blit(visual.spellIconTex(), x, y, 0, 0, size, size, size, size);
            return;
        }
        g.renderFakeItem(visual.fallbackItem(), x, y);
    }

    private static AffixVisual resolveVisual(Affix affix, ResourceLocation id, AffixType type) {
        Holder<MobEffect> mobEffect = extractMobEffectHolder(affix);
        if (mobEffect != null) {
            return new AffixVisual(mobEffect, null, new ItemStack(Items.POTION));
        }

        ResourceLocation spellId = extractSpellId(affix, id);
        if (spellId != null) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                    spellId.getNamespace(),
                    "textures/gui/spell_icons/" + spellId.getPath() + ".png"
            );
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                return new AffixVisual(null, tex, new ItemStack(Items.ENCHANTED_BOOK));
            }
            return new AffixVisual(null, null, new ItemStack(Items.ENCHANTED_BOOK));
        }

        Holder<Attribute> attr = extractAttributeHolder(affix);
        String attrStr = attr != null ? attr.unwrapKey().map(Object::toString).orElse("") : "";
        String key = (id + " " + attrStr).toLowerCase(Locale.ROOT);
        Item iconItem;
        if (key.contains("armor_toughness") || key.contains("steel_touched")) iconItem = Items.NETHERITE_INGOT;
        else if (key.contains("armor_pierce") || key.contains("piercing")) iconItem = Items.POINTED_DRIPSTONE;
        else if (key.contains("armor_shred") || key.contains("shredding")) iconItem = Items.SHEARS;
        else if (key.contains("prot_shred") || key.contains("prot_pierce")) iconItem = Items.ENCHANTED_BOOK;
        else if (key.contains("armor") || key.contains("ironforged") || key.contains("adamantine")) iconItem = Items.DIAMOND_CHESTPLATE;
        else if (key.contains("max_health") || key.contains("blessed")) iconItem = Items.GOLDEN_APPLE;
        else if (key.contains("life_steal") || key.contains("overheal") || key.contains("vampiric") || key.contains("berserking")) iconItem = Items.REDSTONE;
        else if (key.contains("healing_received") || key.contains("spiritual")) iconItem = Items.GLISTERING_MELON_SLICE;
        else if (key.contains("creative_flight") || key.contains("unbound")) iconItem = Items.FEATHER;
        else if (key.contains("elytra") || key.contains("winged")) iconItem = Items.ELYTRA;
        else if (key.contains("swim")) iconItem = Items.HEART_OF_THE_SEA;
        else if (key.contains("movement_speed") || key.contains("windswept")) iconItem = Items.SUGAR;
        else if (key.contains("step_height") || key.contains("elastic")) iconItem = Items.RABBIT_FOOT;
        else if (key.contains("gravity")) iconItem = Items.SHULKER_SHELL;
        else if (key.contains("knockback_resistance") || key.contains("stalwart")) iconItem = Items.OBSIDIAN;
        else if (key.contains("attack_knockback") || key.contains("forceful")) iconItem = Items.PISTON;
        else if (key.contains("crit")) iconItem = Items.GOLDEN_SWORD;
        else if (key.contains("current_hp_damage") || key.contains("giant_slaying")) iconItem = Items.WITHER_SKELETON_SKULL;
        else if (key.contains("attack_damage") || key.contains("violent") || key.contains("murderous")) iconItem = Items.NETHERITE_SWORD;
        else if (key.contains("attack_speed") || key.contains("graceful")) iconItem = Items.FEATHER;
        else if (key.contains("draw_speed") || key.contains("arrow")) iconItem = Items.ARROW;
        else if (key.contains("luck") || key.contains("fortunate") || key.contains("prosperous")) iconItem = Items.EMERALD;
        else if (key.contains("experience") || key.contains("experienced")) iconItem = Items.EXPERIENCE_BOTTLE;
        else if (key.contains("mining") || key.contains("radial") || key.contains("supermassive") || key.contains("omnetic")) iconItem = Items.NETHERITE_PICKAXE;
        else if (key.contains("interaction_range") || key.contains("lengthy")) iconItem = Items.SPYGLASS;
        else if (key.contains("max_mana") || key.contains("mana_regen") || key.contains("mana_cost")) iconItem = Items.LAPIS_LAZULI;
        else if (key.contains("cooldown") || key.contains("cast_time")) iconItem = Items.CLOCK;
        else if (key.contains("fire") || key.contains("infernal") || key.contains("burning")) iconItem = Items.BLAZE_POWDER;
        else if (key.contains("ice") || key.contains("cold") || key.contains("glacial")) iconItem = Items.BLUE_ICE;
        else if (key.contains("lightning") || key.contains("thunder")) iconItem = Items.LIGHTNING_ROD;
        else if (key.contains("blood")) iconItem = Items.REDSTONE;
        else if (key.contains("ender") || key.contains("telepathic")) iconItem = Items.ENDER_PEARL;
        else if (key.contains("eldritch")) iconItem = Items.ECHO_SHARD;
        else if (key.contains("evocation")) iconItem = Items.TOTEM_OF_UNDYING;
        else if (key.contains("holy")) iconItem = Items.GOLD_INGOT;
        else if (key.contains("nature")) iconItem = Items.VINE;
        else if (key.contains("spirit")) iconItem = Items.SOUL_LANTERN;
        else if (key.contains("dune")) iconItem = Items.SAND;
        else if (key.contains("symmetry")) iconItem = Items.COMPASS;
        else if (key.contains("melody") || key.contains("sound")) iconItem = Items.NOTE_BLOCK;
        else if (key.contains("spell")) iconItem = Items.AMETHYST_SHARD;
        else if (key.contains("executing")) iconItem = Items.NETHERITE_AXE;
        else if (key.contains("cleaving")) iconItem = Items.IRON_SWORD;
        else if (key.contains("festive")) iconItem = Items.CHEST;
        else if (key.contains("enlightened")) iconItem = Items.TORCH;
        else if (key.contains("stoneforming")) iconItem = Items.STONE;
        else if (key.contains("sandforming")) iconItem = Items.SAND;
        else if (key.contains("gardening")) iconItem = Items.POPPY;
        else if (key.contains("leafforming")) iconItem = Items.OAK_LEAVES;
        else if (type == AffixType.ABILITY) iconItem = Items.NETHER_STAR;
        else if (type == AffixType.BASIC_EFFECT) iconItem = Items.GLOWSTONE_DUST;
        else iconItem = Items.AMETHYST_SHARD;

        return new AffixVisual(null, null, new ItemStack(iconItem));
    }

    private static List<CategoryInfo> getOrScanCategories() {
        List<CategoryInfo> local = cachedRealCategories;
        if (local != null) {
            return local;
        }
        synchronized (AffixCodexScreen.class) {
            if (cachedRealCategories != null) {
                return cachedRealCategories;
            }
            LootCategory noneCat = LootCategory.forItem(ItemStack.EMPTY);
            cachedNoneCategory = noneCat;

            Map<LootCategory, ItemStack> discovered = new LinkedHashMap<>();
            Map<Item, Integer> preferredItems = Map.ofEntries(
                    Map.entry(Items.NETHERITE_HELMET, 100),
                    Map.entry(Items.NETHERITE_CHESTPLATE, 100),
                    Map.entry(Items.NETHERITE_LEGGINGS, 100),
                    Map.entry(Items.NETHERITE_BOOTS, 100),
                    Map.entry(Items.NETHERITE_SWORD, 100),
                    Map.entry(Items.BOW, 100),
                    Map.entry(Items.TRIDENT, 100),
                    Map.entry(Items.NETHERITE_PICKAXE, 100),
                    Map.entry(Items.SHEARS, 100),
                    Map.entry(Items.SHIELD, 100)
            );

            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                try {
                    ItemStack stack = new ItemStack(item);
                    LootCategory cat = LootCategory.forItem(stack);
                    if (cat == null || cat.equals(noneCat)) continue;

                    ItemStack existing = discovered.get(cat);
                    if (existing == null || preferredItems.getOrDefault(item, 0) > preferredItems.getOrDefault(existing.getItem(), 0)) {
                        discovered.put(cat, stack);
                    }
                } catch (Throwable ignored) {}
            }

            List<CategoryInfo> result = new ArrayList<>();
            for (Map.Entry<LootCategory, ItemStack> e : discovered.entrySet()) {
                LootCategory cat = e.getKey();
                ResourceLocation catId = resolveLootCategoryId(cat);
                Component name = resolveLootCategoryName(cat, catId);
                result.add(new CategoryInfo(cat, catId, name, e.getValue(), false));
            }

            result.sort(Comparator.comparingInt(AffixCodexScreen::categorySortPriority)
                    .thenComparing(c -> c.id().toString()));
            cachedRealCategories = result;
            return result;
        }
    }

    private static int categorySortPriority(CategoryInfo c) {
        return switch (c.id().toString()) {
            case "apotheosis:helmet" -> 1;
            case "apotheosis:chestplate" -> 2;
            case "apotheosis:leggings" -> 3;
            case "apotheosis:boots" -> 4;
            case "apotheosis:melee_weapon" -> 5;
            case "apotheosis:bow" -> 6;
            case "apotheosis:trident" -> 7;
            case "irons_apothic:staff" -> 8;
            case "irons_apothic:spellbook" -> 9;
            case "apotheosis:breaker" -> 10;
            case "apotheosis:shears" -> 11;
            case "apotheosis:shield" -> 12;
            default -> 50;
        };
    }

    // =========================================================================
    // 8. 反射提取与翻译辅助方法
    // =========================================================================

    private static boolean safeCanApplyTo(Affix affix, ItemStack stack, LootCategory cat, LootRarity rarity) {
        try {
            return affix.canApplyTo(stack, cat, rarity);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Component safeGetAffixTitle(Affix affix, ResourceLocation id) {
        try {
            Component c = affix.getName(true);
            if (c != null && !c.getString().isBlank() && !c.getString().startsWith("affix.")) {
                return c;
            }
        } catch (Throwable ignored) {}

        String key = "affix." + id;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String tail = slash >= 0 ? path.substring(slash + 1) : path;
        return Component.literal(prettify(tail));
    }

    private static Component safeGetAugmentingText(Affix affix, ItemStack stack, LootRarity rarity, float level, AttributeTooltipContext ctx) {
        try {
            AffixInstance inst = buildAffixInstance(affix, stack, rarity, level);
            if (inst != null) {
                for (Method m : Affix.class.getMethods()) {
                    if ("getAugmentingText".equals(m.getName()) && m.getParameterCount() == 2) {
                        Object res = m.invoke(affix, inst, ctx);
                        if (res instanceof Component c) return c;
                    }
                }
                for (Method m : AffixInstance.class.getMethods()) {
                    if ("getAugmentingText".equals(m.getName()) && m.getParameterCount() == 1) {
                        Object res = m.invoke(inst, ctx);
                        if (res instanceof Component c) return c;
                    }
                }
            }
            for (Method m : Affix.class.getMethods()) {
                if ("getAugmentingText".equals(m.getName()) && m.getParameterCount() == 3) {
                    Object res = m.invoke(affix, stack, rarity, level);
                    if (res instanceof Component c) return c;
                }
            }
        } catch (Throwable ignored) {}
        return safeGetDescription(affix, stack, rarity, level, ctx);
    }

    private static Component safeGetDescription(Affix affix, ItemStack stack, LootRarity rarity, float level, AttributeTooltipContext ctx) {
        try {
            AffixInstance inst = buildAffixInstance(affix, stack, rarity, level);
            if (inst != null) {
                return inst.getDescription(ctx);
            }
        } catch (Throwable ignored) {}
        return Component.empty();
    }

    @Nullable
    private static AffixInstance buildAffixInstance(Affix affix, ItemStack stack, LootRarity rarity, float level) {
        try {
            DynamicHolder<Affix> afxHolder = AffixRegistry.INSTANCE.holder(affix);
            DynamicHolder<LootRarity> rarHolder = RarityRegistry.INSTANCE.holder(rarity);
            for (Constructor<?> ctor : AffixInstance.class.getConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == 4) {
                    Object[] args = new Object[4];
                    boolean usedAfx = false;
                    for (int i = 0; i < 4; i++) {
                        if (p[i] == DynamicHolder.class) {
                            if (!usedAfx) {
                                args[i] = afxHolder;
                                usedAfx = true;
                            } else {
                                args[i] = rarHolder;
                            }
                        } else if (p[i] == ItemStack.class) {
                            args[i] = stack;
                        } else if (p[i] == float.class || p[i] == Float.class) {
                            args[i] = level;
                        }
                    }
                    return (AffixInstance) ctor.newInstance(args);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<LootCategory> extractDeclaredCategories(Affix affix) {
        List<LootCategory> out = new ArrayList<>();
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val instanceof HolderSet<?> hs) {
                        for (Object o : hs) {
                            if (o instanceof Holder<?> h && h.value() instanceof LootCategory lc && !lc.equals(cachedNoneCategory)) {
                                out.add(lc);
                            }
                        }
                    } else if (val instanceof Collection<?> col) {
                        for (Object o : col) {
                            if (o instanceof LootCategory lc && !lc.equals(cachedNoneCategory)) {
                                out.add(lc);
                            } else if (o instanceof Holder<?> h && h.value() instanceof LootCategory lc && !lc.equals(cachedNoneCategory)) {
                                out.add(lc);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return out;
    }

    private static boolean hasConfiguredRarityValue(Affix affix, LootRarity rarity) {
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val instanceof Map<?, ?> map && map.containsKey(rarity)) return true;
                    if (val instanceof Collection<?> col && col.contains(rarity)) return true;
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Holder<MobEffect> extractMobEffectHolder(Affix affix) {
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val instanceof Holder<?> h && h.value() instanceof MobEffect) {
                        return (Holder<MobEffect>) h;
                    }
                    if (val instanceof MobEffect me) {
                        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(me);
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Holder<Attribute> extractAttributeHolder(Affix affix) {
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val instanceof Holder<?> h && h.value() instanceof Attribute) {
                        return (Holder<Attribute>) h;
                    }
                    if (val instanceof Attribute attr) {
                        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr);
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static ResourceLocation extractSpellId(Affix affix, ResourceLocation affixId) {
        Class<?> cls = affix.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (!f.getName().toLowerCase(Locale.ROOT).contains("spell")) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(affix);
                    if (val instanceof ResourceLocation rl) return rl;
                    if (val != null) {
                        for (String method : List.of("getSpellId", "getSpellResource", "spellId", "id")) {
                            try {
                                Method m = val.getClass().getMethod(method);
                                Object res = m.invoke(val);
                                if (res instanceof ResourceLocation rl) return rl;
                                if (res instanceof String s && s.contains(":")) return ResourceLocation.parse(s);
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }

        String path = affixId.getPath();
        if (path.startsWith("spell_level/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return ResourceLocation.fromNamespaceAndPath(parts[1], parts[2]);
            }
        }
        return null;
    }

    private static int extractBaseWeight(AffixDefinition def) {
        List<WorldTierWeight> list = extractWorldTierWeights(def);
        int maxW = 0;
        for (WorldTierWeight tw : list) {
            if (tw.weight() > maxW) maxW = tw.weight();
        }
        if (maxW > 0) return maxW;
        try {
            Object weights = def.weights();
            for (Field f : weights.getClass().getDeclaredFields()) {
                if (f.getName().toLowerCase(Locale.ROOT).contains("weight") && (f.getType() == int.class || f.getType() == Integer.class)) {
                    f.setAccessible(true);
                    return ((Number) f.get(weights)).intValue();
                }
            }
        } catch (Throwable ignored) {}
        return 25;
    }

    private static float extractBaseQuality(AffixDefinition def) {
        List<WorldTierWeight> list = extractWorldTierWeights(def);
        float maxQ = 0;
        for (WorldTierWeight tw : list) {
            if (tw.quality() > maxQ) maxQ = tw.quality();
        }
        return maxQ;
    }

    private static List<String> extractExclusiveSet(AffixDefinition def) {
        List<String> list = new ArrayList<>();
        try {
            for (Object o : def.exclusiveSet()) {
                if (o instanceof DynamicHolder<?> dh) {
                    list.add(dh.getId().toString());
                } else {
                    list.add(String.valueOf(o));
                }
            }
        } catch (Throwable ignored) {}
        return list;
    }

    private static ResourceLocation resolveLootCategoryId(LootCategory cat) {
        try {
            Class<?> builtInRegs = Class.forName("dev.shadowsoffire.apotheosis.Apoth$BuiltInRegs");
            for (Field f : builtInRegs.getFields()) {
                Object reg = f.get(null);
                if (reg instanceof net.minecraft.core.Registry<?> r && f.getName().contains("LOOT_CATEGORY")) {
                    @SuppressWarnings("unchecked")
                    ResourceLocation rl = ((net.minecraft.core.Registry<LootCategory>) r).getKey(cat);
                    if (rl != null) return rl;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Method m = cat.getClass().getMethod("getName");
            Object res = m.invoke(cat);
            if (res instanceof String s && s.contains(":")) return ResourceLocation.parse(s);
        } catch (Throwable ignored) {}

        return ResourceLocation.fromNamespaceAndPath("apotheosis", cat.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
    }

    private static Component resolveLootCategoryName(LootCategory cat, ResourceLocation id) {
        String key1 = "loot_category." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(key1)) return Component.translatable(key1);
        String key2 = "apotheosis.loot_category." + id.getPath();
        if (I18n.exists(key2)) return Component.translatable(key2);

        return Component.literal(switch (id.toString()) {
            case "apotheosis:helmet" -> "头盔";
            case "apotheosis:chestplate" -> "胸甲";
            case "apotheosis:leggings" -> "护腿";
            case "apotheosis:boots" -> "靴子";
            case "apotheosis:melee_weapon" -> "近战武器";
            case "apotheosis:bow" -> "弓";
            case "apotheosis:trident" -> "三叉戟";
            case "apotheosis:breaker" -> "挖掘工具";
            case "apotheosis:shears" -> "剪刀";
            case "apotheosis:shield" -> "盾牌";
            case "irons_apothic:staff" -> "法杖";
            case "irons_apothic:spellbook" -> "法术书";
            default -> prettify(id.getPath());
        });
    }

    private static String readTranslationOrEmpty(String... keys) {
        for (String key : keys) {
            if (I18n.exists(key)) {
                String val = I18n.get(key);
                if (!val.isBlank() && !val.equals(key)) {
                    return val;
                }
            }
        }
        return "";
    }

    private static Component translateFirst(String key, String fallback) {
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    private static String toPlainString(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((idx, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private static int getRarityColorRgb(LootRarity rarity) {
        try {
            return rarity.color().getValue();
        } catch (Throwable t) {
            return 0xFFFFFF;
        }
    }

    private static int getRaritySortOrder(LootRarity rarity) {
        for (String name : List.of("sortIndex", "ordinal")) {
            try {
                Method m = rarity.getClass().getMethod(name);
                return ((Number) m.invoke(rarity)).intValue();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static String getRarityShortLabel(LootRarity rarity) {
        ResourceLocation id = RarityRegistry.INSTANCE.getKey(rarity);
        if (id != null) {
            return switch (id.getPath()) {
                case "common" -> "普通";
                case "uncommon" -> "罕见";
                case "rare" -> "稀有";
                case "epic" -> "史诗";
                case "mythic" -> "神话";
                case "ancient" -> "远古";
                default -> prettify(id.getPath());
            };
        }
        return "未知";
    }

    private static Component getTypeBadge(AffixType type) {
        return switch (type) {
            case STAT -> Component.literal("[属性]").withStyle(ChatFormatting.AQUA);
            case BASIC_EFFECT -> Component.literal("[特效]").withStyle(ChatFormatting.GREEN);
            case ABILITY -> Component.literal("[能力]").withStyle(ChatFormatting.LIGHT_PURPLE);
        };
    }

    private static String getModDisplayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(prettify(modId));
    }

    private String trimToWidth(String text, int maxW) {
        if (maxW <= 8) return "";
        if (this.font.width(text) <= maxW) return text;
        String ellipsis = "...";
        int target = Math.max(0, maxW - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, target) + ellipsis;
    }

    private static String prettify(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
