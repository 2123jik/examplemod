package com.example.examplemod.engine;

import com.mojang.serialization.Codec;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "examplemod")
public class TestWandItem extends Item {

    public static final int BASE_SLOT_CAPACITY = 4;

    private static final ResourceLocation WAND_SLOTS_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "wand_slots");
    private static final ResourceLocation MODIFIER_ID_KEY = ResourceLocation.fromNamespaceAndPath("examplemod", "modifier_id");
    private static final ResourceLocation MODIFIER_LEVEL_KEY = ResourceLocation.fromNamespaceAndPath("examplemod", "modifier_level");
    private static final ResourceLocation WAND_ITEM_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "test_wand");
    private static final ResourceLocation GEM_ITEM_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "modifier_gem");

    public static final Supplier<DataComponentType<ItemContainerContents>> WAND_SLOTS =
            DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, WAND_SLOTS_ID);
    public static final Supplier<DataComponentType<ResourceLocation>> MODIFIER_ID =
            DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, MODIFIER_ID_KEY);
    public static final Supplier<DataComponentType<Integer>> MODIFIER_LEVEL =
            DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, MODIFIER_LEVEL_KEY);
    public static final Supplier<Item> TEST_WAND =
            DeferredHolder.create(Registries.ITEM, WAND_ITEM_ID);
    public static final Supplier<Item> MODIFIER_GEM =
            DeferredHolder.create(Registries.ITEM, GEM_ITEM_ID);

    public TestWandItem(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.DATA_COMPONENT_TYPE, h -> {
            h.register(WAND_SLOTS_ID, DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());
            h.register(MODIFIER_ID_KEY, DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());
            h.register(MODIFIER_LEVEL_KEY, DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
        });

        event.register(Registries.ITEM, h -> {
            h.register(WAND_ITEM_ID, new TestWandItem(new Item.Properties().stacksTo(1)));
            h.register(GEM_ITEM_ID, new ModifierGemItem(new Item.Properties().stacksTo(1)));
        });
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(new ItemStack(TEST_WAND.get()));
            for (ResourceLocation id : ModifierCatalog.allIds()) {
                for (int lvl : new int[]{1, 3}) {
                    ItemStack gem = new ItemStack(MODIFIER_GEM.get());
                    gem.set(MODIFIER_ID.get(), id);
                    gem.set(MODIFIER_LEVEL.get(), lvl);
                    event.accept(gem);
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                sp.openMenu(
                        new SimpleMenuProvider(
                                (id, inv, p) -> new WandEditMenu(id, inv, hand),
                                stack.getHoverName()
                        ),
                        buf -> buf.writeVarInt(hand.ordinal())
                );
            } else {
                WandExecutionEngine.castConnected(sp);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.MAINHAND && event.getSlot() != EquipmentSlot.OFFHAND) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        if (event.getFrom().has(WAND_SLOTS.get()) || event.getTo().has(WAND_SLOTS.get())) {
            WandPassiveTickHandler.updateHeldWand(sp, WandExecutionEngine.compileConnectedPassives(sp));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            WandPassiveTickHandler.updateHeldWand(sp, WandExecutionEngine.compileConnectedPassives(sp));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            WandPassiveTickHandler.updateHeldWand(sp, WandExecutionEngine.compileConnectedPassives(sp));
        }
    }

    public static boolean isValidModule(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof Scroll && ISpellContainer.isSpellContainer(stack)) return true;
        if (stack.getItem() instanceof ModifierGemItem) {
            ResourceLocation id = stack.get(MODIFIER_ID.get());
            return id != null && ModifierCatalog.get(id) != null;
        }
        return false;
    }

    public static int[] computeSlotUsageAndMax(List<ItemStack> stacks, int hardLimit) {
        int used = 0;
        int maxCap = Math.min(BASE_SLOT_CAPACITY, hardLimit);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof Scroll) {
                used += 1;
            } else if (stack.getItem() instanceof ModifierGemItem) {
                var spec = getGemSpec(stack);
                if (spec != null) {
                    used += spec.slotCost();
                    maxCap += spec.getBonusSlots(getGemLevel(stack));
                }
            }
        }
        return new int[]{used, Math.min(hardLimit, maxCap)};
    }

    @Nullable
    public static ModifierCatalog.ModSpec getGemSpec(ItemStack stack) {
        ResourceLocation id = stack.get(MODIFIER_ID.get());
        return id != null ? ModifierCatalog.get(id) : null;
    }

    public static int getGemLevel(ItemStack stack) {
        return Math.max(1, stack.getOrDefault(MODIFIER_LEVEL.get(), 1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("法术编排法杖");
    }

    public static class ModifierGemItem extends Item {
        public ModifierGemItem(Properties props) {
            super(props);
        }

        @Override
        public Component getName(ItemStack stack) {
            var spec = getGemSpec(stack);
            if (spec == null) return Component.literal("空白法术词条");
            return Component.literal(spec.displayName() + " Lv." + getGemLevel(stack));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            var spec = getGemSpec(stack);
            if (spec == null) return;
            int lvl = getGemLevel(stack);
            String typeLabel = spec.isPassiveTab() ? "法术被动" : "法术增强";
            tip.add(Component.literal("[" + typeLabel + "]  占格: " + spec.slotCost()
                    + (spec.getBonusSlots(lvl) > 0 ? " (容量+" + spec.getBonusSlots(lvl) + ")" : ""))
                    .withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal(ModifierCatalog.formatDesc(spec, lvl)).withStyle(ChatFormatting.WHITE));

            float flatRes = spec.getFlatReserve(lvl);
            float pctRes = spec.getPercentReserve(lvl);
            if (flatRes > 0 || pctRes > 0) {
                tip.add(Component.literal(String.format("预留法力值: %.0f + %.0f%% 最大法力值",
                        flatRes, pctRes * 100f)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            float mMult = spec.getManaMult(lvl);
            float mAdd = spec.getFlatAddMana(lvl);
            if (mMult != 1.0F || mAdd > 0) {
                tip.add(Component.literal(String.format("施法消耗: ×%.2f + %.0f 法力值",
                        mMult, mAdd)).withStyle(ChatFormatting.BLUE));
            }
        }
    }
}