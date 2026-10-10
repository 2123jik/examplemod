package com.example.examplemod.compat.emi;

import com.example.examplemod.network.RequestEmiCraftingPayload;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public final class ExampleModEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        DeepBackpackCraftingHandler handler =
                new DeepBackpackCraftingHandler();

        registry.addRecipeHandler(
                MenuType.CRAFTING,
                handler
        );

        /*
         * EMI 的 getFirstValidHandler() 会从前往后查找。
         *
         * 因此必须把自己的 Handler 放到 CRAFTING 列表第一位，
         * 否则原版 Handler 会先匹配。
         */
        List<EmiRecipeHandler<?>> handlers =
                EmiRecipeFiller.handlers.get(MenuType.CRAFTING);

        if (handlers != null) {
            handlers.remove(handler);
            handlers.add(0, handler);
        }

        System.out.println(
                "[ExampleMod EMI] crafting handlers = "
                        + handlers
        );
    }

    private static final class DeepBackpackCraftingHandler
            implements EmiRecipeHandler<CraftingMenu> {

        @Override
        public EmiPlayerInventory getInventory(
                AbstractContainerScreen<CraftingMenu> screen
        ) {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) {
                return new EmiPlayerInventory(List.of());
            }

            Inventory inventory = minecraft.player.getInventory();
            List<EmiStack> stacks = new ArrayList<>();

            for (ItemStack stack : inventory.items) {
                if (!stack.isEmpty()) {
                    stacks.add(EmiStack.of(stack.copy()));
                }
            }

            for (ItemStack stack : inventory.offhand) {
                if (!stack.isEmpty()) {
                    stacks.add(EmiStack.of(stack.copy()));
                }
            }

            return new EmiPlayerInventory(stacks);
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe != null
                    && recipe.getId() != null
                    && recipe.getInputs().size() <= 9;
        }

        @Override
        public boolean alwaysDisplaySupport(EmiRecipe recipe) {
            return supportsRecipe(recipe);
        }

        @Override
        public boolean canCraft(
                EmiRecipe recipe,
                EmiCraftContext<CraftingMenu> context
        ) {
            /*
             * 不要使用 context.getInventory().canCraft(recipe)。
             * 客户端看不到末影背包内容。
             *
             * 真正的材料检查交给服务端。
             */
            return supportsRecipe(recipe);
        }

        @Override
        public boolean craft(
                EmiRecipe recipe,
                EmiCraftContext<CraftingMenu> context
        ) {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) {
                return false;
            }

            CraftingMenu menu = context.getScreenHandler();

            if (minecraft.player.containerMenu != menu) {
                return false;
            }

            int requestedSets = Screen.hasShiftDown()
                    ? 64
                    : Math.max(1, context.getAmount());

            PacketDistributor.sendToServer(
                    new RequestEmiCraftingPayload(
                            recipe.getId(),
                            requestedSets
                    )
            );

            return true;
        }
    }
}
