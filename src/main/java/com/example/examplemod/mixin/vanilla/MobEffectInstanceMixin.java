package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceMixin {

    @Shadow private int amplifier;
    @Shadow private int duration;
    @Shadow private boolean ambient;
    @Shadow private boolean visible;
    @Shadow private boolean showIcon;
    @Shadow private MobEffectInstance hiddenEffect;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onUpdate(MobEffectInstance other, CallbackInfoReturnable<Boolean> cir) {
        MobEffectInstance self = (MobEffectInstance) (Object) this;

        // 1. 严格过滤目标药水效果（例如 LUCK 幸运）
        if (self.getEffect().is(net.minecraft.world.effect.MobEffects.LUCK)) {

            int currentAmp = this.amplifier;
            int incomingAmp = other.getAmplifier();

            int currentDur = this.duration;
            int incomingDur = other.getDuration();

            boolean changed = false;

            // 2. 边界处理：如果其中有一个是无限时间 (-1)
            if (currentDur == -1 || incomingDur == -1) {
                int finalAmp = Math.max(currentAmp, incomingAmp);
                if (this.amplifier != finalAmp) {
                    this.amplifier = finalAmp;
                    changed = true;
                }
                if (this.duration != -1) {
                    this.duration = -1;
                    changed = true;
                }
            } else {
                // 3. 核心：基于“等级经济价值权重（2^amp）”的指数分配算法
                // I级(amp=0) = 1, II级(amp=1) = 2, III级(amp=2) = 4, IV级(amp=3) = 8 ...
                double currentWeight = Math.pow(2, currentAmp);
                double incomingWeight = Math.pow(2, incomingAmp);

                // 计算两个药水拥有的“真实价值能量”并累加
                double totalEnergy = (currentWeight * currentDur) + (incomingWeight * incomingDur);

                // 采用倾向于维持高等级的策略
                int finalAmp = Math.max(currentAmp, incomingAmp);
                double finalWeight = Math.pow(2, finalAmp);

                // 由总能量反推在该最高等级下应该具有的持续时间
                int finalDur = (int) Math.round(totalEnergy / finalWeight);

                // 限制安全边界 (Minecraft 等级上限为 255)
                finalAmp = Math.max(0, Math.min(255, finalAmp));
                finalDur = Math.max(1, finalDur);

                if (this.amplifier != finalAmp || this.duration != finalDur) {
                    this.amplifier = finalAmp;
                    this.duration = finalDur;
                    changed = true;
                }
            }

            // 4. 🔥 斩断隐藏属性链（对应 tick 源码的防御）
            // 必须强制将隐藏药水清空，否则原版 tick 完会从 hiddenEffect 里面诈尸还原
            if (this.hiddenEffect != null) {
                this.hiddenEffect = null;
                changed = true;
            }

            // 5. 同步原版各种状态（环境信标、粒子、图标）
            if (!other.isAmbient() && this.ambient || changed) {
                this.ambient = other.isAmbient();
                changed = true;
            }
            if (other.isVisible() != this.visible) {
                this.visible = other.isVisible();
                changed = true;
            }
            if (other.showIcon() != this.showIcon) {
                this.showIcon = other.showIcon();
                changed = true;
            }

            // 6. 返回改变状态，通知网络层给客户端同步
            cir.setReturnValue(changed);
        }
    }
}
