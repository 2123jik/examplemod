package com.example.examplemod.entity;

import com.example.examplemod.ExampleMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static com.example.examplemod.registry.ModRegistry.CABINET_ENTITY;

/**
 * 将装备柜库存公开给其他模组。
 *
 * 所有方向使用同一库存。
 * side == null 的无方向查询也支持。
 *
 * 这个类属于公共逻辑，不能限制为 Dist.CLIENT。
 */
@EventBusSubscriber(
        modid = ExampleMod.MODID
)
public final class EquipmentCabinetCapabilities {
    private EquipmentCabinetCapabilities() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CABINET_ENTITY.get(),
                (cabinet, side) -> cabinet.getItemHandler()
        );
    }
}
