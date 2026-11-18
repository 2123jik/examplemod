package com.example.examplemod.compat.jade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.StreamServerDataProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum CustomStatusEffectsServerProvider implements StreamServerDataProvider<EntityAccessor, List<MobEffectInstance>> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("examplemod", "status_effects");

    // 解决泛型冲突：反向调用 apply，编译 100% 通过
    private static final StreamCodec<RegistryFriendlyByteBuf, List<MobEffectInstance>> STREAM_CODEC =
            MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list());

    @Override
    public boolean shouldRequestData(EntityAccessor accessor) {
        return accessor.getEntity() instanceof LivingEntity;
    }

    @Override
    public @Nullable List<MobEffectInstance> streamData(EntityAccessor accessor) {
        if (accessor.getEntity() instanceof LivingEntity living) {
            Collection<MobEffectInstance> active = living.getActiveEffects();
            if (active.isEmpty()) {
                return null;
            }
            // 不加任何 isVisible 过滤！拿走真正的每一个 Effect
            return new ArrayList<>(active);
        }
        return null;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, List<MobEffectInstance>> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}