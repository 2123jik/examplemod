package com.nf121.examplemod.mixin.solcarrot;

import com.cazsius.solcarrot.tracking.FoodList;
import com.cazsius.solcarrot.tracking.MaxHealthHandler;
import com.cazsius.solcarrot.tracking.ProgressInfo;
import com.nf121.examplemod.registry.ModPayloads;
import com.nf121.examplemod.skill.PlayerSkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.nf121.examplemod.registry.ModRegistry.SKILL_DATA;

@Mixin(value = MaxHealthHandler.class, remap = false)
public abstract class MaxHealthHandlerMixin {

    @Shadow
    @Final
    private static ResourceLocation MILESTONE_HEALTH_MODIFIER_ID;

    /**
     * 拦截原版的 updateFoodHPModifier 方法
     * 1. 阻止原版加生命
     * 2. 清除可能遗留的生命 Modifier
     * 3. 结算并发放技能点数
     */
    @Inject(method = "updateFoodHPModifier", at = @At("HEAD"), cancellable = true)
    private static void onUpdateFoodHPModifier(Player player, CallbackInfoReturnable<Boolean> cir) {
        // 1. 如果玩家身上存在原版的生命 Modifier（比如曾经装过没改版的 SoL），将其彻底移除
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null && maxHealthAttr.getModifier(MILESTONE_HEALTH_MODIFIER_ID) != null) {
            maxHealthAttr.removeModifier(MILESTONE_HEALTH_MODIFIER_ID);
        }

        // 2. 客户端不负责数据写入，直接返回 false
        if (player.level().isClientSide) {
            cir.setReturnValue(false);
            return;
        }

        // 3. 直接通过 SoL 的 API 读取当前达到的里程碑数量
        ProgressInfo progressInfo = FoodList.get(player).getProgressInfo();
        int milestonesAchieved = progressInfo.milestonesAchieved();

        // 4. 将技能点结算给玩家
        PlayerSkillData skillData = player.getData(SKILL_DATA.get());

        // 设定每个里程碑奖励几个技能点（例如 1 点）
        int pointsPerMilestone = 5;
        boolean hasChanged = skillData.updateFoodMilestones(milestonesAchieved, pointsPerMilestone);

        // 如果点数增加了（吃新食物达成了里程碑），立刻发包通知客户端刷新 UI
        if (hasChanged && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ModPayloads.SyncSkillDataPayload(skillData));
        }

        // 返回给 SoL 的 FoodTracker，用于触发原版的升级音效和粒子
        cir.setReturnValue(hasChanged);

    }

    /**
     * 阻止原版在玩家死亡重生（Clone）时重新赋予血量 Modifier
     */
    @Inject(method = "onPlayerClone", at = @At("HEAD"), cancellable = true)
    private static void onPlayerClone(PlayerEvent.Clone event, CallbackInfo ci) {
        // 彻底阻止原版克隆血量 Modifier 的逻辑
        ci.cancel();
    }
}