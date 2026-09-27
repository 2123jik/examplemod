package com.example.examplemod.compat.l2hostility;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class AutoSpellTrait extends MobTrait {

    private final Supplier<? extends AbstractSpell> spellSupplier;
    private final Map<Mob, Integer> lastCastPerMob = new WeakHashMap<>();
    private int cachedCooldown = -1;

    public AutoSpellTrait(Supplier<? extends AbstractSpell> spellSupplier) {
        super(() -> SpellTraitHelper.getDynamicColor(spellSupplier.get()));
        this.spellSupplier = spellSupplier;
    }

    private int getCooldownTicks() {
        if (cachedCooldown > 0) return cachedCooldown;
        AbstractSpell spell = spellSupplier.get();
        if (spell == null) return 60;
        cachedCooldown = SpellTraitHelper.getSafeCooldown(spell);
        return cachedCooldown;
    }

    @Override
    public void tick(LivingEntity entity, int traitLevel) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;
        // 只有被 PathfinderMobMixin 注入过的实体才具备施法能力
        if (!(mob instanceof IMagicEntity magicMob)) return;

        // 施法状态机完全交给 IMagicEntity，正在施法就什么都不做
        if (magicMob.isCasting()) return;

        AbstractSpell spell = spellSupplier.get();
        if (spell == null || !spell.isEnabled()) return;

        // 冷却
        Integer last = lastCastPerMob.get(mob);
        if (last != null && mob.tickCount < last + Math.max(40, getCooldownTicks())) return;

        // 目标
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        // Trait 等级 → 法术等级
        int maxTraitLv = this.getMaxLevel(mob.level().registryAccess());
        int spellLevel = SpellTraitHelper.scaleSpellLevel(traitLevel, maxTraitLv, spell);

        // 起手。剩下的 windup / 引导 / onCast / 同步 / 瞄准全部由 Mixin 处理
        magicMob.initiateCastSpell(spell, spellLevel);
        lastCastPerMob.put(mob, mob.tickCount);
    }

    @Override
    public String getDescriptionId() {
        AbstractSpell spell = spellSupplier.get();
        return spell != null ? spell.getComponentId() : super.getDescriptionId();
    }

    @Override
    public void addDetail(RegistryAccess access, List<Component> list) {
        AbstractSpell spell = spellSupplier.get();
        if (spell == null) { super.addDetail(access, list); return; }
        list.add(Component.translatable(spell.getComponentId() + ".guide")
                .withStyle(ChatFormatting.GRAY));
        try {
            for (MutableComponent line : spell.getUniqueInfo(1, null)) {
                list.add(line.withStyle(ChatFormatting.GRAY));
            }
        } catch (Throwable ignored) {}
    }
}