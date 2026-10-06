//package com.example.examplemod.compat.emi;
//
//import com.example.examplemod.network.RequestFetchRecipeIngredientsPayload;
//import dev.emi.emi.api.recipe.EmiRecipe;
//import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
//import dev.emi.emi.api.recipe.handler.EmiCraftContext;
//import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
//import dev.emi.emi.api.stack.EmiIngredient;
//import dev.emi.emi.api.widget.Widget;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.world.inventory.CraftingMenu;
//import net.minecraft.world.inventory.Slot;
//import net.minecraft.world.item.ItemStack;
//import net.neoforged.neoforge.network.PacketDistributor;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class CraftingRecipeHandler implements StandardRecipeHandler<CraftingMenu> {
//
//    private long lastCraftTime = 0;
//
//    @Override
//    public List<Slot> getCraftingSlots(CraftingMenu menu) {
//        return menu.slots.subList(1, 10);
//    }
//
//    @Override
//    public List<Slot> getInputSources(CraftingMenu menu) {
//        return menu.slots.subList(10, 46);
//    }
//
//    @Override
//    public Slot getOutputSlot(CraftingMenu menu) {
//        return menu.getSlot(0);
//    }
//
//    @Override
//    public boolean supportsRecipe(EmiRecipe recipe) {
//        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING;
//    }
//
//    @Override
//    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
//        return true;
//    }
//
//    @Override
//    public void render(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context, List<Widget> widgets, GuiGraphics draw) {
//        // 留空，不渲染红底
//    }
//
//    @Override
//    public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
//        long now = System.currentTimeMillis();
//        // 限制至少间隔 150ms 才能再次发包，防止狂点造成发包竞争与刷物
//        if (now - lastCraftTime < 150) {
//            return true;
//        }
//        lastCraftTime = now;
//
//        List<RequestFetchRecipeIngredientsPayload.SlotReq> needed = new ArrayList<>();
//        List<EmiIngredient> inputs = recipe.getInputs();
//
//        for (int i = 0; i < Math.min(9, inputs.size()); i++) {
//            EmiIngredient input = inputs.get(i);
//            if (input != null && !input.isEmpty() && !input.getEmiStacks().isEmpty()) {
//                ItemStack is = input.getEmiStacks().get(0).getItemStack();
//                if (!is.isEmpty()) {
//                    needed.add(new RequestFetchRecipeIngredientsPayload.SlotReq(
//                            i + 1,
//                            is.copyWithCount((int) Math.max(1, input.getAmount()))
//                    ));
//                }
//            }
//        }
//
//        if (!needed.isEmpty()) {
//            PacketDistributor.sendToServer(new RequestFetchRecipeIngredientsPayload(needed));
//        }
//        return true;
//    }
//}