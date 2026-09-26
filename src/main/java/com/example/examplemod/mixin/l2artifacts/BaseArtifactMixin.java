package com.example.examplemod.mixin.l2artifacts;

import com.example.examplemod.util.ArtifactStatFormatter;
import dev.xkmc.l2artifacts.content.core.ArtifactStats;
import dev.xkmc.l2artifacts.content.core.BaseArtifact;
import dev.xkmc.l2artifacts.content.core.StatEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(value = BaseArtifact.class, remap = false)
public class BaseArtifactMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void appendPercentageToHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> list, TooltipFlag flag, CallbackInfo ci) {
        // 1. 按住 Shift 状态下原版展示的是经验等详细信息，不展示词条列表
        if (flag.hasShiftDown()) return;

        Optional<ArtifactStats> statsOpt = BaseArtifact.getStats(stack);
        if (statsOpt.isEmpty()) return;

        ArtifactStats stats = statsOpt.get();
        // 2. 还有待升级未确认时，原版只显示“有可用升级”，不展示词条
        if (stats.level() > stats.old_level()) return;

        int currentLevel = stats.level();

        // 3. 匹配并替换主词条
        if (stats.main_stat() != null) {
            Component mainTip = stats.main_stat().getTooltip(null);
            if (mainTip != null) {
                String targetStr = mainTip.getString();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getString().equals(targetStr)) {
                        list.set(i, ArtifactStatFormatter.format(list.get(i), stats.main_stat(), currentLevel, true));
                        break;
                    }
                }
            }
        }

        // 4. 匹配并替换副词条
        if (stats.sub_stats() != null) {
            for (StatEntry sub : stats.sub_stats()) {
                Component subTip = sub.getTooltip(null);
                if (subTip != null) {
                    String targetStr = subTip.getString();
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getString().equals(targetStr)) {
                            list.set(i, ArtifactStatFormatter.format(list.get(i), sub, currentLevel, false));
                            break;
                        }
                    }
                }
            }
        }
    }
}