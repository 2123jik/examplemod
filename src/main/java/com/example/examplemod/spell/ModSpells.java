package com.example.examplemod.spell;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = 
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ExampleMod.MOD_ID);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUMMON_CAVE_SPIDER =
            SPELLS.register("summon_cave_spider", SummonCaveSpiderSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUMMON_DRAGON =
            SPELLS.register("summon_dragon", SummonDragonSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> CUSTOM_LIGHTNING_LANCE_SPELL =
            SPELLS.register("custom_lightning_lance", CustomLightningLanceSpell::new);

}