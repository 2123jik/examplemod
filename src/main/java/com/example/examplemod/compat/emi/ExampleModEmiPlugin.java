package com.example.examplemod.compat.emi;

import com.example.examplemod.network.RequestEmiCraftingPayload;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
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
    public ExampleModEmiPlugin() {
    }
    @Override
    public void register(EmiRegistry registry) {
        /*
         * 重点：
         * 这里必须传 MenuType.CRAFTING，
         * 不能传 CraftingMenu.class。
         */
        registry.addRecipeHandler(
                MenuType.CRAFTING,
                new DeepBackpackCraftingHandler()
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

            /*
             * 这里只提供客户端能看到的普通物品栏。
             *
             * 末影背包、Curios 饰品栏、嵌套背包的真实内容，
             * 由服务端在收到请求后扫描。
             */
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

            /*
             * EmiPlayerInventory 的这个构造器会自动加入当前菜单
             * 的 carried stack，所以不需要手动添加鼠标光标物品。
             */
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
            /*
             * 即使 EMI 客户端看不到嵌套背包里的材料，也显示快速合成按钮。
             * 服务端会进行最终判断。
             */
            return supportsRecipe(recipe);
        }

        @Override
        public boolean canCraft(
                EmiRecipe recipe,
                EmiCraftContext<CraftingMenu> context
        ) {
            /*
             * 客户端看不到饰品栏末影背包和嵌套背包内容，
             * 因此不能使用 context.getInventory().canCraft(recipe)
             * 作为最终判断。
             *
             * 这里只负责让 EMI 显示并允许点击“+”按钮。
             * 真正的材料检查由服务端 DeepBackpackFetcher 完成。
             */
            return supportsRecipe(recipe);
        }


        @Override
        public boolean craft(
                EmiRecipe recipe,
                EmiCraftContext<CraftingMenu> context
        ) {
            if (!supportsRecipe(recipe)
                    || context.getType()
                    != EmiCraftContext.Type.FILL_BUTTON) {
                return false;
            }

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) {
                return false;
            }

            CraftingMenu menu = context.getScreenHandler();

            if (minecraft.player.containerMenu != menu) {
                return false;
            }

            /*
             * 这个 EMI 版本没有 CRAFT_ALL。
             *
             * 普通点击通常 amount = 1。
             * Shift 点击时直接请求一个较大的上限，
             * 具体可填多少由服务端根据材料和槽位限制计算。
             */
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
