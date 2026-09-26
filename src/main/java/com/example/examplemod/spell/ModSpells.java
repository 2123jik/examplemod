package com.example.examplemod.spell;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSpells {
    // 创建针对 SpellRegistry 的 DeferredRegister
    public static final DeferredRegister<AbstractSpell> SPELLS = 
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ExampleMod.MODID);

    // 注册法术
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUMMON_CAVE_SPIDER =
            SPELLS.register("summon_cave_spider", SummonCaveSpiderSpell::new);

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}