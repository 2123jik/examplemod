package com.nf121.examplemod.event;

import com.nf121.examplemod.registry.ModDataComponents;
import com.nf121.examplemod.util.UnidentifiedItemHelper;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public class UnidentifiedItemUseHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack heldStack = event.getItemStack();
        if (!heldStack.has(ModDataComponents.IS_UNIDENTIFIED.get())) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();

        // 客户端提前拦截，返回 SUCCESS 播放抬手动作
        if (level.isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // 背包检查
        if (!UnidentifiedItemHelper.hasRoomForIdentify(player, heldStack)) {
            player.sendSystemMessage(UnidentifiedItemHelper.fullMessage());
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        // 1. 处理宝石鉴定
        if (heldStack.getItem() instanceof GemItem) {
            Purity purity = GemItem.getPurity(heldStack);
            RandomSource random = player.getRandom();

            List<Gem> validGems = GemRegistry.INSTANCE.getValues().stream()
                    .filter(g -> purity.isAtLeast(g.getMinPurity()))
                    .toList();

            if (!validGems.isEmpty()) {
                Gem selectedGem = validGems.get(random.nextInt(validGems.size()));
                ItemStack identifiedGem = selectedGem.toStack(purity);

                UnidentifiedItemHelper.deliver(
                        player, hand, heldStack, identifiedGem,
                        12, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f, "message.examplemod.identify_gem");

                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
                return;
            }
        }

        // 2. 处理法术卷轴鉴定
        if (heldStack.getItem() instanceof Scroll) {
            var scrollData = heldStack.get(ModDataComponents.SCROLL_INFO.get());
            SpellRarity targetRarity = scrollData != null ? scrollData.rarity() : SpellRarity.COMMON;
            ResourceLocation filter = scrollData != null ? scrollData.schoolFilter().orElse(null) : null;

            ItemStack identifiedScroll = createIdentifiedScroll(targetRarity, filter, player.getRandom());
            if (!identifiedScroll.isEmpty()) {
                UnidentifiedItemHelper.deliver(
                        player, hand, heldStack, identifiedScroll,
                        12, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f, "message.examplemod.identify_scroll");

                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
            }
        }
    }

    private static final Map<SpellRarity, List<AbstractSpell>> SPELLS_BY_RARITY = new ConcurrentHashMap<>();

    private static List<AbstractSpell> spellsFor(SpellRarity rarity) {
        return SPELLS_BY_RARITY.computeIfAbsent(rarity, r ->
                SpellRegistry.REGISTRY.stream()
                        .filter(s -> s.isEnabled()
                                && s != SpellRegistry.none()
                                && s.allowLooting()
                                && r.getValue() >= s.getMinRarity()
                                && r.getValue() <= s.getMaxRarity())
                        .toList());
    }

    private static ItemStack createIdentifiedScroll(SpellRarity targetRarity, ResourceLocation schoolFilter, RandomSource random) {
        List<AbstractSpell> pool = spellsFor(targetRarity).stream()
                .filter(s -> schoolFilter == null || s.getSchoolType().getId().equals(schoolFilter))
                .toList();

        if (pool.isEmpty()) return ItemStack.EMPTY;

        AbstractSpell chosen = pool.get(random.nextInt(pool.size()));
        List<Integer> matchingLevels = new ArrayList<>();
        for (int lvl = chosen.getMinLevel(); lvl <= chosen.getMaxLevel(); lvl++) {
            if (chosen.getRarity(lvl) == targetRarity) matchingLevels.add(lvl);
        }
        int level = matchingLevels.isEmpty()
                ? chosen.getMinLevelForRarity(targetRarity)
                : matchingLevels.get(random.nextInt(matchingLevels.size()));

        ItemStack result = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(chosen, level, result);
        return result;
    }
}