package com.example.examplemod.mixin.ironsspellbooks.eldritch;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.void_tentacle.VoidTentacle;
import io.redspace.ironsspellbooks.spells.eldritch.SculkTentaclesSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SculkTentaclesSpell.class, remap = false)
public class SculkTentaclesSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectHiveInfestation(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        if (level.isClientSide) return;

        var hiveAttr = entity.getAttribute(ModAttributes.SCULK_HIVE_INFESTATION.getDelegate());
        if (hiveAttr != null && hiveAttr.getValue() >= 1.0) {
            // 在触手爆发后延迟生效：感染范围内敌人并二次破土
            ((ServerLevel) level).getServer().tell(new net.minecraft.server.TickTask(15, () -> {
                AABB searchArea = entity.getBoundingBox().inflate(16.0);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, searchArea)) {
                    if (victim != entity && !victim.isAlliedTo(entity)) {
                        Vec3 spawn = Utils.moveToRelativeGroundLevel(level, victim.position(), 4);
                        if (!level.getBlockState(BlockPos.containing(spawn).below()).isAir()) {
                            // 从敌人脚底钻出寄生宿主触手
                            VoidTentacle parasiticTentacle = new VoidTentacle(level, entity, ((SculkTentaclesSpell) (Object) this).getSpellPower(spellLevel, entity) * 1.2f);
                            parasiticTentacle.moveTo(spawn);
                            parasiticTentacle.setYRot(Utils.random.nextInt(360));
                            level.addFreshEntity(parasiticTentacle);
                        }
                    }
                }
            }));
        }
    }
}