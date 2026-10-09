package com.example.examplemod.compat.jei;

import com.example.examplemod.network.RequestFetchRecipeIngredientsPayload;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BackpackCraftingTransferHandler
        implements IRecipeTransferHandler<
                CraftingMenu,
                RecipeHolder<CraftingRecipe>
        > {

    private long lastTransferNanos;

    @Override
    public Class<CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            CraftingMenu menu,
            RecipeHolder<CraftingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        /*
         * JEI 会在检查按钮状态时调用 doTransfer=false。
         * 返回 null 表示允许转移，但此时绝对不能发包。
         *
         * 客户端没有完整的远程背包库存，因此这里不判断材料是否足够。
         */
        if (!doTransfer) {
            return null;
        }

        List<RequestFetchRecipeIngredientsPayload.SlotReq> requests =
                createRequests(menu, player, recipeHolder.value());

        if (requests.isEmpty()) {
            return null;
        }

        // 客户端点击节流；不代替服务端的数据校验。
        long now = System.nanoTime();
        if (lastTransferNanos != 0
                && now - lastTransferNanos < 150_000_000L) {
            return null;
        }
        lastTransferNanos = now;

        PacketDistributor.sendToServer(
                new RequestFetchRecipeIngredientsPayload(requests, maxTransfer)
        );


        return null;
    }

    private static List<RequestFetchRecipeIngredientsPayload.SlotReq>
    createRequests(
            CraftingMenu menu,
            Player player,
            CraftingRecipe recipe
    ) {
        List<RequestFetchRecipeIngredientsPayload.SlotReq> requests =
                new ArrayList<>();

        List<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();

            if (width > 3 || height > 3) {
                return requests;
            }

            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    int ingredientIndex = row * width + column;

                    if (ingredientIndex >= ingredients.size()) {
                        continue;
                    }

                    // CraftingMenu 的 0 是结果，1～9 是合成格。
                    int menuSlot = 1 + row * 3 + column;

                    addRequest(
                            requests,
                            menu,
                            player,
                            menuSlot,
                            ingredients.get(ingredientIndex)
                    );
                }
            }
        } else {
            // 无序配方按顺序放入九宫格。
            int menuSlot = 1;

            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                if (menuSlot > 9) {
                    return new ArrayList<>();
                }

                addRequest(
                        requests,
                        menu,
                        player,
                        menuSlot,
                        ingredient
                );

                menuSlot++;
            }
        }

        return requests;
    }

    private static void addRequest(
            List<RequestFetchRecipeIngredientsPayload.SlotReq> requests,
            CraftingMenu menu,
            Player player,
            int menuSlot,
            Ingredient ingredient
    ) {
        if (ingredient.isEmpty()) {
            return;
        }

        ItemStack selected = selectIngredient(
                menu,
                player,
                menuSlot,
                ingredient
        );

        if (!selected.isEmpty()) {
            requests.add(
                    new RequestFetchRecipeIngredientsPayload.SlotReq(
                            menuSlot,
                            selected.copyWithCount(1)
                    )
            );
        }
    }

    private static ItemStack selectIngredient(
            CraftingMenu menu,
            Player player,
            int menuSlot,
            Ingredient ingredient
    ) {
        // 优先保留目标合成格中已经符合配方的物品。
        ItemStack current = menu.getSlot(menuSlot).getItem();
        if (!current.isEmpty() && ingredient.test(current)) {
            return current;
        }

        // 然后优先选择主背包中已有的候选材料。
        for (int i = 0; i < 36; i++) {
            ItemStack candidate = player.getInventory().getItem(i);

            if (!candidate.isEmpty() && ingredient.test(candidate)) {
                return candidate;
            }
        }

        // 远程/嵌套背包库存由服务端读取。
        // 现有协议只能发送一个具体物品，这里回退到第一个候选项。
        for (ItemStack candidate : ingredient.getItems()) {
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }

        return ItemStack.EMPTY;
    }
}
