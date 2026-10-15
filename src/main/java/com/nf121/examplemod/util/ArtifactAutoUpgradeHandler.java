package com.nf121.examplemod.util;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.l2artifacts.content.core.ArtifactSet;
import dev.xkmc.l2artifacts.content.core.ArtifactSlot;
import dev.xkmc.l2artifacts.content.core.BaseArtifact;
import dev.xkmc.l2artifacts.content.search.common.AbstractScrollerMenu;
import dev.xkmc.l2artifacts.content.search.token.ArtifactTabData;
import dev.xkmc.l2artifacts.init.L2Artifacts;
import dev.xkmc.l2artifacts.init.registrate.entries.SetEntry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ArtifactAutoUpgradeHandler {

    public static void processAutoUpgrade(ServerPlayer player) {
        if (!(player.containerMenu instanceof AbstractScrollerMenu<?> menu)) return;

        ArtifactTabData token = menu.token;
        if (token == null || !token.stillValid(player)) return;

        RegistryAccess registryAccess = player.level().registryAccess();
        RandomSource random = player.getRandom();

        List<ItemStack> rawList = token.data.getAll();
        Map<BaseArtifact, List<ItemStack>> artifactGroups = new LinkedHashMap<>();
        List<ItemStack> otherItems = new ArrayList<>();

        // 1. 归类遗物
        for (ItemStack stack : rawList) {
            if (stack.getItem() instanceof BaseArtifact artifact) {
                artifactGroups.computeIfAbsent(artifact, k -> new ArrayList<>()).add(stack);
            } else {
                otherItems.add(stack);
            }
        }

        List<ItemStack> resultList = new ArrayList<>(otherItems);

        // 2. 遍历同类遗物并合成升阶
        for (Map.Entry<BaseArtifact, List<ItemStack>> entry : artifactGroups.entrySet()) {
            BaseArtifact artifact = entry.getKey();
            List<ItemStack> stacks = entry.getValue();
            int count = stacks.size();

            ArtifactSet set = artifact.set.get();
            ArtifactSlot targetSlot = artifact.slot.get();
            int currentRank = artifact.rank;

            SetEntry<?> setEntry = L2Artifacts.REGISTRATE.SET_MAP.get(set.getRegistryName());
            if (setEntry == null) {
                resultList.addAll(stacks);
                continue;
            }

            // === 规则 1：优先尝试 8 合 1 -> Rank + 3 ===
            Optional<ItemStack> rankPlus3Template = findArtifactItem(setEntry, targetSlot, currentRank + 3);
            if (rankPlus3Template.isPresent()) {
                int craft8 = count / 8;
                count %= 8;
                for (int i = 0; i < craft8; i++) {
                    ItemStack newStack = rankPlus3Template.get().copy();
                    resolveArtifact(newStack, registryAccess, random);
                    resultList.add(newStack);
                }
            }

            // === 规则 2：剩余的尝试 4 合 1 -> Rank + 2 ===
            // (若不支持 +3 但支持 +2，原本的 8 个或剩下的也会在这里被合成 2 个 Rank+2)
            Optional<ItemStack> rankPlus2Template = findArtifactItem(setEntry, targetSlot, currentRank + 2);
            if (rankPlus2Template.isPresent()) {
                int craft4 = count / 4;
                count %= 4;
                for (int i = 0; i < craft4; i++) {
                    ItemStack newStack = rankPlus2Template.get().copy();
                    resolveArtifact(newStack, registryAccess, random);
                    resultList.add(newStack);
                }
            }
            Optional<ItemStack> rankPlus1Template = findArtifactItem(setEntry, targetSlot, currentRank + 1);
            if (rankPlus1Template.isPresent()) {
                int craft2 = count / 2;
                count %= 2;
                for (int i = 0; i < craft2; i++) {
                    ItemStack newStack = rankPlus1Template.get().copy();
                    resolveArtifact(newStack, registryAccess, random);
                    resultList.add(newStack);
                }
            }

            // === 规则 3：剩余未满足数量的遗物原样保留，若未鉴定则自动补齐鉴定 ===
            for (int i = 0; i < count; i++) {
                ItemStack remain = stacks.get(i);
                if (BaseArtifact.getStats(remain).isEmpty()) {
                    resolveArtifact(remain, registryAccess, random);
                }
                resultList.add(remain);
            }
        }

        // 3. 数据写回保存
        rawList.clear();
        rawList.addAll(resultList);
        token.updateAndSave();

        // 4. 安全触发界面刷新
        menu.clickMenuButton(player, 0);
    }

    /**
     * 安全根据部位和目标品阶查找物品，彻底规避数组越界问题
     */
    private static Optional<ItemStack> findArtifactItem(SetEntry<?> setEntry, ArtifactSlot targetSlot, int targetRank) {
        if (setEntry.items == null) return Optional.empty();

        // 遍历套装包含的所有部位行
        for (ItemEntry<BaseArtifact>[] slotRow : setEntry.items) {
            if (slotRow == null || slotRow.length == 0) continue;

            // 检查该行对应的部位是否匹配
            BaseArtifact firstItem = slotRow[0].get();
            if (firstItem.slot.get().equals(targetSlot)) {
                int minRank = firstItem.rank;
                int maxRank = slotRow[slotRow.length - 1].get().rank;

                // 校验目标品阶是否在该套装该部位的品阶区间内
                if (targetRank >= minRank && targetRank <= maxRank) {
                    int rankOffset = targetRank - minRank;
                    if (rankOffset >= 0 && rankOffset < slotRow.length) {
                        return Optional.of(slotRow[rankOffset].asStack());
                    }
                }
                return Optional.empty(); // 属于该部位，但目标品阶超出了该部位支持的最大上限
            }
        }

        return Optional.empty();
    }

    private static void resolveArtifact(ItemStack stack, RegistryAccess access, RandomSource random) {
        if (stack.getItem() instanceof BaseArtifact artifact) {
            artifact.resolve(access, stack, false, random);
        }
    }
}