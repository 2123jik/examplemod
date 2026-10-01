package com.example.examplemod.trim;

import com.example.examplemod.trim.RunewordSmithingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;

//@JeiPlugin
public class ModJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new RunewordSmithingCategory(helper),
                new RunewordCodexCategory(helper) // 【新增图鉴分类】
        );    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        RecipeManager recipeManager = level.getRecipeManager();

        // 1. 注册 33 种单步锻造配方
        List<RecipeHolder<RunewordSmithingRecipe>> runewordRecipes = recipeManager.getRecipes().stream()
                .filter(holder -> holder.value() instanceof RunewordSmithingRecipe)
                .map(holder -> (RecipeHolder<RunewordSmithingRecipe>) (RecipeHolder<?>) holder)
                .toList();
        registration.addRecipes(RunewordSmithingCategory.TYPE, runewordRecipes);

        // 2. 注册 78 种完整纹饰之语图鉴条目！
        List<RunewordDisplayEntry> codexEntries = new ArrayList<>();
        for (RunewordDefinition def : RunewordManager.RUNEWORDS.values()) {
            // 默认用钻石剑作为预览基底（如果是防具则用钻石胸甲）
            ItemStack previewBase = def.validBaseTags().stream().anyMatch(t -> t.contains("chestplates"))
                    ? new ItemStack(Items.DIAMOND_CHESTPLATE)
                    : new ItemStack(Items.DIAMOND_SWORD);

            codexEntries.add(new RunewordDisplayEntry(def, previewBase));
        }
        registration.addRecipes(RunewordCodexCategory.TYPE, codexEntries);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 关键绑定：把原版锻造台设为催化剂！
        // 这样玩家只要对着锻造台按 U，或者在锻造台界面点配方书，就能直接看到我们的配方！
        registration.addRecipeCatalyst(new ItemStack(Items.SMITHING_TABLE), RunewordSmithingCategory.TYPE);
    }
}