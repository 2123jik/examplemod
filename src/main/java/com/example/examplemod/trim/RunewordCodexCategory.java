package com.example.examplemod.trim;

import com.example.examplemod.registry.ModDataComponents;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;

public class RunewordCodexCategory implements IRecipeCategory<RunewordDisplayEntry> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MODID, "runeword_codex");
    public static final RecipeType<RunewordDisplayEntry> TYPE = RecipeType.create(MODID, "runeword_codex", RunewordDisplayEntry.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public RunewordCodexCategory(IGuiHelper helper) {
        // 创建一个宽 150, 高 60 的整洁纯色卡片背景
        this.background = helper.createBlankDrawable(160, 50);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.NETHER_STAR));
        this.localizedName = Component.literal("纹饰之语 · 共鸣图鉴");
    }

    @Override
    public RecipeType<RunewordDisplayEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return this.localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RunewordDisplayEntry entry, IFocusGroup focuses) {
        RunewordDefinition def = entry.definition();

        // 构造一件激活好的极品产物
        ItemStack stack = entry.previewOutput().copy();
        stack.set(ModDataComponents.ACTIVE_RUNEWORD.get(), def.id());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l" + def.nameCn() + " · §r" + stack.getHoverName().getString())
                .withStyle(ChatFormatting.GOLD));

        // 产物展示槽 (位于右侧)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 135, 16)
                .addItemStack(stack);
    }

    @Override
    public void draw(RunewordDisplayEntry entry, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        RunewordDefinition def = entry.definition();

        // 1. 标题：金色中文名 + 英文名
        guiGraphics.drawString(font, "§6§l" + def.nameCn() + " §7(" + def.nameEn() + ")", 5, 5, 0xFFFFFF, false);

        // 2. 刻印时序
        StringBuilder seq = new StringBuilder("§e时序: ");
        for (int i = 0; i < def.sequence().size(); i++) {
            String p = def.sequence().get(i).pattern();
            String name = p.contains(":") ? p.substring(p.indexOf(':') + 1) : p;
            seq.append(name);
            if (i < def.sequence().size() - 1) seq.append(" ➔ ");
        }
        guiGraphics.drawString(font, seq.toString(), 5, 20, 0xAAAAAA, false);

        // 3. 适用底板
        guiGraphics.drawString(font, "§8底板孔数: §b" + def.socketsRequired() + " 孔", 5, 35, 0x888888, false);
    }
}