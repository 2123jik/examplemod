package com.example.examplemod.mixin.ironsspellbooks.fire;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BurningDashSpell.class, remap = false)
public class BurningDashSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectIgnitionCharge(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var dashAttr = entity.getAttribute(ModAttributes.BURNING_DASH_IGNITION.getDelegate());
        if (dashAttr != null && dashAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            // 突进过程中每经过 2 刻检测撞击到的敌人数量，叠加劫火印记
            for (int i = 2; i <= 14; i += 3) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(i, () -> {
                    if (!entity.isAlive()) return;
                    var hits = world.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(1.2), e -> e != entity && !e.isAlliedTo(entity));
                    if (!hits.isEmpty()) {
                        int currentCharges = entity.getPersistentData().getInt("IgnitionCharges");
                        // 至多叠加 5 层
                        entity.getPersistentData().putInt("IgnitionCharges", Math.min(5, currentCharges + hits.size()));
                        entity.getPersistentData().putInt("IgnitionChargeTimer", 160); // 持续 8 秒
                    }
                }));
            }
        }
    }
}