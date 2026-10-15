package com.nf121.examplemod.deathrecap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EntitySnapshot {
    public final String entityName;
    public final String entityTypeId;
    public final float health;
    public final float maxHealth;
    public final CompoundTag entityNbt;
    public final List<ItemStack> equipment;
    public final List<ItemStack> curios; // 接入饰品
    public final List<String> activeEffects;

    public static final StreamCodec<RegistryFriendlyByteBuf, EntitySnapshot> STREAM_CODEC = StreamCodec.of(
            EntitySnapshot::write,
            EntitySnapshot::read
    );

    public EntitySnapshot(LivingEntity entity) {
        this.entityName = entity.getDisplayName().getString();
        this.entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        this.health = entity.getHealth();
        this.maxHealth = entity.getMaxHealth();

        this.entityNbt = new CompoundTag();
        entity.saveWithoutId(this.entityNbt);

        this.equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.add(entity.getItemBySlot(slot).copy());
        }

        // 调用你写好的 CuriosCompat
        this.curios = CuriosCompat.getCurios(entity);

        this.activeEffects = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            String effectId = effect.getEffect().unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("unknown");
            this.activeEffects.add(effectId + " (Lv." + (effect.getAmplifier() + 1) + ")");
        }
    }

    public EntitySnapshot(String name, String typeId, float hp, float maxHp, CompoundTag nbt,
                          List<ItemStack> equip, List<ItemStack> curios, List<String> effects) {
        this.entityName = name;
        this.entityTypeId = typeId;
        this.health = hp;
        this.maxHealth = maxHp;
        this.entityNbt = nbt;
        this.equipment = equip;
        this.curios = curios;
        this.activeEffects = effects;
    }

    private static void write(RegistryFriendlyByteBuf buf, EntitySnapshot snapshot) {
        ByteBufCodecs.STRING_UTF8.encode(buf, snapshot.entityName);
        ByteBufCodecs.STRING_UTF8.encode(buf, snapshot.entityTypeId);
        ByteBufCodecs.FLOAT.encode(buf, snapshot.health);
        ByteBufCodecs.FLOAT.encode(buf, snapshot.maxHealth);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, snapshot.entityNbt);
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, snapshot.equipment);
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, snapshot.curios);
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, snapshot.activeEffects);
    }

    private static EntitySnapshot read(RegistryFriendlyByteBuf buf) {
        String name = ByteBufCodecs.STRING_UTF8.decode(buf);
        String typeId = ByteBufCodecs.STRING_UTF8.decode(buf);
        float hp = ByteBufCodecs.FLOAT.decode(buf);
        float maxHp = ByteBufCodecs.FLOAT.decode(buf);
        CompoundTag nbt = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        List<ItemStack> equip = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        List<ItemStack> curios = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        List<String> effects = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);

        return new EntitySnapshot(name, typeId, hp, maxHp, nbt, equip, curios, effects);
    }
}