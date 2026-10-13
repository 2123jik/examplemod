package com.example.examplemod.registry;

import com.example.examplemod.registry.modulargolems.ModRegistry;
import com.example.examplemod.spell.ModSpells;
import net.neoforged.bus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {}

    /**
     * 一键向 Mod 总线注册所有 DeferredRegister
     */
    public static void register(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModParticles.PARTICLE_TYPES.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModAttributes.ATTRIBUTES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModSpells.SPELLS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);

        // 附属/联动注册
        ModRegistry.MODIFIERS.register(modBus);
        ModRegistry.ITEMS.register(modBus);
    }
}