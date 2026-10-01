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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;

public class RunewordSmithingCategory implements IRecipeCategory<RecipeHolder<RunewordSmithingRecipe>> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MODID, "runeword_smithing");
    public static final RecipeType<RecipeHolder<RunewordSmithingRecipe>> TYPE = RecipeType.create(
            MODID,
            "runeword_smithing",
            (Class<RecipeHolder<RunewordSmithingRecipe>>) (Class<?>) RecipeHolder.class
    );

    // 原版锻造台贴图路径
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/smithing.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public RunewordSmithingCategory(IGuiHelper helper) {
        // =========================================================================
        // 【像素级精确切片】
        // 原版锻造台贴图中：
        // 槽位起点在 X=7, Y=46，覆盖：
        // 3个输入槽 (宽 18*3=54) + 箭头区域 (宽约 26) + 输出槽 (宽 26)
        // 裁剪区域：X=7, Y=46, 宽度 120, 高度 20
        // =========================================================================
        this.background = helper.createDrawable(TEXTURE, 7, 46, 120, 20);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.SMITHING_TABLE));
        this.localizedName = Component.translatable("jei.examplemod.category.runeword_smithing");
    }

    @Override
    public RecipeType<RecipeHolder<RunewordSmithingRecipe>> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RunewordSmithingRecipe> holder, IFocusGroup focuses) {
        RunewordSmithingRecipe recipe = holder.value();

        // =========================================================================
        // 【槽位绝对居中坐标】
        // 原版槽位内部绘制起始偏移为 (1, 1)，每一个槽位步长严格递增 18 像素
        // Slot 0 (模板): X = 1,  Y = 1
        // Slot 1 (底板): X = 19, Y = 1  (1 + 18)
        // Slot 2 (材料): X = 37, Y = 1  (19 + 18)  <-- 金锭会完美落入第 3 个格子！
        // Slot 3 (产物): X = 98, Y = 1
        // =========================================================================

        // 1. 模板槽 (Slot 0)
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .addIngredients(recipe.getTemplate());

        // 2. 底板槽 (Slot 1)
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 1)
                .addIngredients(recipe.getBase());

        // 3. 附加材料槽 (Slot 2)
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 1)
                .addIngredients(recipe.getAddition());

        // 构造代表产物（带工序 NBT 数据，鼠标悬停可看效果）
        ItemStack sampleOutput = recipe.getResultItem(null).copy();
        ItemStack[] templates = recipe.getTemplate().getItems();
        ItemStack[] additions = recipe.getAddition().getItems();
        if (templates.length > 0 && additions.length > 0) {
            String tmplId = BuiltInRegistries.ITEM.getKey(templates[0].getItem()).toString();
            String matId = BuiltInRegistries.ITEM.getKey(additions[0].getItem()).toString();
            sampleOutput.set(ModDataComponents.TRIM_HISTORY.get(), List.of(
                    new ModDataComponents.TrimStep(tmplId, matId)
            ));
        }

        // 4. 产物槽 (Slot 3) - 完美对齐大输出框
        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 1)
                .addItemStack(sampleOutput);
    }

    @Override
    public void draw(RecipeHolder<RunewordSmithingRecipe> holder, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 背景已经完美自包含所有边框和箭头，无需额外手动绘制贴图
    }
}