package com.example.examplemod.util;

import com.mojang.logging.LogUtils;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class Common {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void addDrop(BlockDropsEvent event, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();

        event.getDrops().add(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack
        ));
    }

    public static ItemStack createGatewayPearl(WorldTier tier) {
        if (tier == null||tier==WorldTier.HAVEN) {
            return ItemStack.EMPTY;
        }

        String gatewayPath = "tiered/" + tier.getSerializedName();

        Optional<ResourceLocation> gatewayId = GatewayRegistry.INSTANCE
                .getKeys()
                .stream()
                .filter(id -> id.getPath().equals(gatewayPath))
                .findFirst();

        if (gatewayId.isEmpty()) {
            LOGGER.warn("No gateway found with path '{}'", gatewayPath);
            return ItemStack.EMPTY;
        }

        ResourceLocation id = gatewayId.get();
        Gateway gateway = GatewayRegistry.INSTANCE.getValue(id);

        if (gateway == null) {
            LOGGER.warn("Gateway registry returned null for '{}'", id);
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(GatewayObjects.GATE_PEARL);

        GatePearlItem.setGate(
                stack,
                GatewayRegistry.INSTANCE.holder(gateway)
        );

        return stack;
    }

    public static ItemStack createRandomLowestGem(RandomSource random) {
        List<Gem> gems = GemRegistry.INSTANCE.getValues()
                .stream()
                .toList();

        if (gems.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Gem gem = gems.get(random.nextInt(gems.size()));

        return GemItem.createStack(
                gem,
                gem.getMinPurity()
        );
    }
    /**
     * 枚举越接近中间位置，概率越高。
     * 最低和最高枚举值始终保留非零概率。
     *
     * 权重示例：
     * 3 个枚举：1、2、1
     * 5 个枚举：1、4、5、4、1
     */
    public static <E extends Enum<E>> boolean roll(
            RandomSource random,
            E value
    ) {
        E[] values = value.getDeclaringClass().getEnumConstants();

        int index = value.ordinal();
        int count = values.length;

        int currentWeight = getWeight(index, count);

        /*
         * sum(1 + i * (count - 1 - i))
         * = count + count * (count - 1) * (count - 2) / 6
         */
        int totalWeight =
                count + count * (count - 1) * (count - 2) / 6;

        return random.nextInt(totalWeight) < currentWeight;
    }

    private static int getWeight(int index, int count) {
        return 1 + index * (count - 1 - index);
    }
}