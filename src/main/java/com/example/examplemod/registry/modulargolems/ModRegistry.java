package com.example.examplemod.registry.modulargolems;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.compat.modulargolems.modifier.BackstabModifier;
import dev.xkmc.modulargolems.content.item.upgrade.SimpleUpgradeItem;
import dev.xkmc.modulargolems.content.modifier.base.GolemModifier;
import dev.xkmc.modulargolems.init.registrate.GolemTypes;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {

    // 1. 注册 Modifier 到 Modular Golems 自定义注册表
    public static final DeferredRegister<GolemModifier> MODIFIERS =
            DeferredRegister.create(GolemTypes.MODIFIERS.key(), ExampleMod.MOD_ID);

    public static final DeferredHolder<GolemModifier, BackstabModifier> BACKSTAB_MODIFIER =
            MODIFIERS.register("backstab", () -> new BackstabModifier(3));

    // 2. 注册升级卡物品（使用 NeoForge 的 createItems）
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MOD_ID);

    public static final DeferredItem<SimpleUpgradeItem> BACKSTAB_UPGRADE =
            ITEMS.register("backstab_upgrade", () -> new SimpleUpgradeItem(
                    new Item.Properties(),
                    BACKSTAB_MODIFIER::get,
                    1,      // 默认提供的等级为 1
                    false   // 是否自带附魔光晕
            ));
}