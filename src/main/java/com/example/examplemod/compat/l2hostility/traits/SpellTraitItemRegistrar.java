package com.example.examplemod.compat.l2hostility.traits;

import com.example.examplemod.ExampleMod;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.init.registrate.LHBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID) // 默认 game bus
public class SpellTraitItemRegistrar {

    /** itemId -> 已注册的 Item 实例，供创造栏事件读取。用 LinkedHashMap 保持白名单顺序。 */
    private static final Map<ResourceLocation, Item> REGISTERED = new LinkedHashMap<>();

    // ============================================================
    // 1) 为每个白名单法术动态注册一个 TraitSymbol
    //    注意：ResourceLocation 必须与 trait 完全一致，否则 TraitSymbol.get() 返回 null
    // ============================================================
    @SubscribeEvent
    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;

        for (String idStr : SpellTraitRegistrar.WHITELIST) {
            ResourceLocation itemId = SpellTraitRegistrar.traitIdOf(idStr);
            if (itemId == null) continue;

            event.register(Registries.ITEM, itemId, () -> {
                TraitSymbol symbol = new SpellTraitSymbol(new Item.Properties());
                REGISTERED.put(itemId, symbol);
                return symbol;
            });
        }
    }

    // ============================================================
    // 2) 塞进莱特兰恶意的创造栏
    //    LHBlocks.TAB 就是 L2Hostility.REGISTRATE.buildL2CreativeTab("hostility", ...)
    // ============================================================
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(LHBlocks.TAB.key())) return;

        for (Item item : REGISTERED.values()) {
            event.accept(new ItemStack(item));
        }
    }
}