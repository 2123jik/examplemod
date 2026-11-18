package com.example.examplemod.runeword;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.event.CanSocketGemEvent;
import dev.shadowsoffire.apotheosis.event.ItemSocketingEvent;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class RunewordSocketingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunewordSocketingHandler.class);

    @SubscribeEvent
    public static void onCanSocketGem(CanSocketGemEvent event) {
        ItemStack target = event.getInputStack(); // 若为 getItemStack() 请按 IDEA 提示替换
        if (target.has(ModDataComponents.ACTIVE_RUNEWORD.get()) && !SocketHelper.hasEmptySockets(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemSocketed(ItemSocketingEvent event) {
        ItemStack result = event.getOutput().copy();
        if (result.isEmpty()) {
            return;
        }

        var rarityHolder = AffixHelper.getRarity(result);
        if (!rarityHolder.isBound() || !rarityHolder.getId().equals(RunewordPurityHelper.COMMON_RARITY)) {
            return;
        }

        int totalSockets = SocketHelper.getSockets(result);
        boolean hasEmpty = SocketHelper.hasEmptySockets(result);
        if (totalSockets <= 0 || hasEmpty) {
            return;
        }

        List<GemInstance> gemInstances = SocketHelper.getGems(result).gems();
        Purity requiredPurity = null;
        List<ResourceLocation> insertedGemIds = new ArrayList<>(totalSockets);
        List<String> puritiesDebug = new ArrayList<>(totalSockets);

        for (GemInstance inst : gemInstances) {
            if (!inst.isValid() || !inst.gem().isBound()) {
                return;
            }
            Purity currentPurity = GemItem.getPurity(inst.gemStack());
            puritiesDebug.add(currentPurity.getSerializedName());
            if (requiredPurity == null) {
                requiredPurity = currentPurity;
            } else if (requiredPurity != currentPurity) {
                return;
            }
            insertedGemIds.add(inst.gem().getId());
        }



        for (RunewordDefinition def : RunewordManager.getAll().values()) {
            if (def.socketsRequired() != totalSockets) continue;
            if (!def.matchesBaseItem(result)) continue;
            if (!def.gemSequence().equals(insertedGemIds)) continue;

            result.set(
                    ModDataComponents.ACTIVE_RUNEWORD.get(),
                    new ModDataComponents.ActiveRuneword(def.id(), requiredPurity.getSerializedName())
            );
            event.setOutput(result); // 关键：显式写回 event.setOutput！
            return;
        }

    }
}