package com.example.examplemod.spell;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.CustomLightningLanceProjectile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.lightning.LightningLanceSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CustomLightningLanceSpell extends LightningLanceSpell {
    // 覆盖为自己模组的法术 ID
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "custom_lightning_lance");

    @Override
    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    // 1. 蓄力开始的同时跳起来
    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);

        // 蓄力起手瞬间给玩家一个向上的初速度，跳向空中
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, 0.65D, motion.z);
        entity.hurtMarked = true; // 强制同步至客户端物理引擎
    }

    // 2. 蓄力过程中保持浮空一段时间
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);

        Vec3 motion = entity.getDeltaMovement();
        // 抵消重力下坠，给极其轻微的浮力，让玩家在蓄力蓄势期间悬浮在半空
        entity.setDeltaMovement(motion.x * 0.9D, Math.max(motion.y * 0.5D, 0.02D), motion.z * 0.9D);
        entity.hurtMarked = true;
    }

    // 3. 蓄力结束，发出自定义的长矛实体
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // 创建我们自定义的弹体
        CustomLightningLanceProjectile lance = new CustomLightningLanceProjectile(level, entity);

        // 沿用原版的发射位置与朝向计算
        lance.setPos(entity.position().add(0.0D, entity.getEyeHeight(), 0.0D).add(entity.getForward()));
        lance.shoot(entity.getLookAngle());

        // 伤害继承原版法术强度（this.getSpellPower）
        lance.setDamage(this.getSpellPower(spellLevel, entity));

        level.addFreshEntity(lance);

        // 调用 super 播放原版发射音效（LIGHTNING_WOOSH_01）与施法完成流程
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}