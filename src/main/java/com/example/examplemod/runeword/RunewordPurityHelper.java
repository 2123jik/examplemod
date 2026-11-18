package com.example.examplemod.runeword;

import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RunewordPurityHelper {

    public static final ResourceLocation COMMON_RARITY = Apotheosis.loc("common");

    /**
     * 6 档纯度对应的属性倍率（以 normal = 1.0 为基准）
     */
    public static double getScale(String purityName) {
        return switch (purityName) {
            case "cracked"  -> 0.40;
            case "chipped"  -> 0.55;
            case "flawed"   -> 0.75;
            case "normal"   -> 1.00;
            case "flawless" -> 1.25;
            case "perfect"  -> 1.50;
            default         -> 1.00;
        };
    }

    public static double getScale(Purity purity) {
        return getScale(purity.getSerializedName());
    }

    /**
     * 缩放属性数值（正面属性按倍率缩放，负面惩罚保持原值避免越高品质惩罚越重）
     */
    public static double scaleAttributeAmount(double baseAmount, String purityName) {
        if (baseAmount <= 0) return baseAmount;
        double scaled = baseAmount * getScale(purityName);
        return Math.round(scaled * 1000.0) / 1000.0;
    }

    /**
     * 缩放“+全法术等级”（基础 > 0 时保底至少 +1）
     */
    public static int scaleSpellLevels(int baseLevels, String purityName) {
        if (baseLevels <= 0) return 0;
        return Math.max(1, (int) Math.round(baseLevels * getScale(purityName)));
    }

    public static ChatFormatting getPurityColor(String purityName) {
        return switch (purityName) {
            case "cracked"  -> ChatFormatting.GRAY;
            case "chipped"  -> ChatFormatting.WHITE;
            case "flawed"   -> ChatFormatting.GREEN;
            case "normal"   -> ChatFormatting.BLUE;
            case "flawless" -> ChatFormatting.LIGHT_PURPLE;
            case "perfect"  -> ChatFormatting.GOLD;
            default         -> ChatFormatting.GOLD;
        };
    }

    /**
     * 校验一件装备当前是否仍然合法激活着符文之语（防止玩家拆孔或重铸后依然残留属性）
     */
    public static boolean isRunewordIntact(ItemStack stack, ModDataComponents.ActiveRuneword active) {
        if (active == null || stack.isEmpty()) return false;

        RunewordDefinition def = RunewordManager.getAll().get(active.id());
        if (def == null) return false;

        // 1. 必须是 common 底材（请核对 AffixHelper.getRarity 返回类型）
        var rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound() || !rarityHolder.getId().equals(COMMON_RARITY)) {
            return false;
        }

        // 2. 孔数必须完全匹配且无空孔
        int sockets = SocketHelper.getSockets(stack);
        if (sockets != def.socketsRequired() || SocketHelper.hasEmptySockets(stack)) {
            return false;
        }

        // 3. 宝石顺序和纯度必须全部仍一致
        List<GemInstance> gems = SocketHelper.getGems(stack).gems();
        if (gems.size() != def.gemSequence().size()) return false;

        List<ResourceLocation> actualIds = new ArrayList<>(gems.size());
        for (GemInstance inst : gems) {
            if (!inst.isValid() || !inst.gem().isBound()) return false;
            Purity p = GemItem.getPurity(inst.gemStack());
            if (!p.getSerializedName().equals(active.purity())) {
                return false;
            }
            actualIds.add(inst.gem().getId());
        }

        return def.gemSequence().equals(actualIds);
    }

}