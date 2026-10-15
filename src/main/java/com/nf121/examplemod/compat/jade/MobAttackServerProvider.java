package com.nf121.examplemod.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

public enum MobAttackServerProvider implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) return;
        if (!living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {return;}
        double base = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (base <= 0) return;

        // 1. 恶意模组倍率（没装/没等级 = 1.0）
        double l2hMult = L2HHooks.getMultiplier(living);

        // 2. 原版世界难度对玩家的倍率（困难 = 1.5，简单 = 0.5+1，普通 = 1.0）
        double diffMult = switch (living.level().getDifficulty()) {
            case PEACEFUL -> 0.0;
            case EASY     -> (base * l2hMult <= 2.0) ? 1.0 : (0.5 + 1.0 / (base * l2hMult)); // 原版 min(dmg/2 + 1, dmg)
            case NORMAL   -> 1.0;
            case HARD     -> 1.5;
        };

        // 直接算出一巴掌拍玩家身上的伤害
        double finalDamage = base * l2hMult * diffMult;
        data.putDouble("actual_attack", finalDamage);
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("examplemod", "mob_attack");
    }
}