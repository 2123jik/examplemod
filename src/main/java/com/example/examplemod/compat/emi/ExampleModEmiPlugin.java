//package com.example.examplemod.compat.emi;
//
//import dev.emi.emi.api.EmiEntrypoint;
//import dev.emi.emi.api.EmiPlugin;
//import dev.emi.emi.api.EmiRegistry;
//import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
//import dev.emi.emi.registry.EmiRecipeFiller;
//import net.minecraft.world.inventory.MenuType;
//
//import java.util.List;
//
////@EmiEntrypoint
//public class ExampleModEmiPlugin implements EmiPlugin {
//
//    @Override
//    public void register(EmiRegistry registry) {
//        CraftingRecipeHandler customHandler = new CraftingRecipeHandler();
//
//        // 1. 正常注册
//        registry.addRecipeHandler(MenuType.CRAFTING, customHandler);
//
//        // 2. 【核心提权】：将我们的 Handler 强制移到列表最前面，压制原版 Handler
//        try {
//            List<EmiRecipeHandler<?>> list = EmiRecipeFiller.handlers.get(MenuType.CRAFTING);
//            if (list != null && list.remove(customHandler)) {
//                list.add(0, customHandler); // 插入到第 0 位
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}