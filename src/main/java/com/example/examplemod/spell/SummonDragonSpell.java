package com.example.examplemod.spell;

import com.example.examplemod.registry.ModEntities;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SummonDragonSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath("examplemod", "summon_dragon");
    private final DefaultConfig defaultConfig;

    public SummonDragonSpell() {
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.LEGENDARY)
                .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
                .setMaxLevel(5)
                .setCooldownSeconds(300.0)
                .build();

        this.baseManaCost = 200;
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 3;
        this.castTime = 60;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_GROWL);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 1;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance,
                                 RecastResult recastResult, ICastDataSerializable castData) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castData)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castData);
        }
    }


    public int getSupportDuration(int level) {
        return 20 * 60 * (5 + level * 2);
    }

    public float getDragonHealth(int spellLevel, LivingEntity caster) {
        return (100.0F + spellLevel * 50.0F) * this.getEntityPowerMultiplier(caster);
    }

    public float getFireballDamage(int spellLevel, LivingEntity caster) {
        return this.getSpellPower(spellLevel, caster) * 1.5F;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.literal("支援时长: " + (getSupportDuration(spellLevel) / 1200) + " 分钟"),
                Component.translatable("ui.irons_spellbooks.hp",
                        Utils.stringTruncation(getDragonHealth(spellLevel, caster), 0)),
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getFireballDamage(spellLevel, caster), 1)),
                Component.literal("火球间隔: 3 秒")
        );
    }


    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity,
                       CastSource castSource, MagicData playerMagicData) {

        PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

        if (!recasts.hasRecastForSpell(this)) {
            SummonedEntitiesCastData summonedData = new SummonedEntitiesCastData();
            int summonTime = getSupportDuration(spellLevel);
            float hp = getDragonHealth(spellLevel, entity);
            float dmg = getFireballDamage(spellLevel, entity);

            SummonedSupportDragon dragon = new SummonedSupportDragon(
                    ModEntities.SUMMONED_SUPPORT_DRAGON.get(), world);

            dragon.moveTo(
                    entity.getX(),
                    entity.getY() + 10.0,
                    entity.getZ(),
                    entity.getYRot(),
                    0.0F
            );

            if (dragon.getAttribute(Attributes.MAX_HEALTH) != null) {
                dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                dragon.setHealth(hp);
            }
            dragon.setSpellPower(dmg, spellLevel);

            SpellSummonEvent event = new SpellSummonEvent(
                    entity, dragon, this.spellId, spellLevel);
            NeoForge.EVENT_BUS.post(event);
            LivingEntity finalCreature = event.getCreature();

            world.addFreshEntity(finalCreature);
            SummonManager.initSummon(entity, finalCreature, summonTime, summonedData);

            RecastInstance recastInstance = new RecastInstance(
                    this.getSpellId(),
                    spellLevel,
                    this.getRecastCount(spellLevel, entity),
                    summonTime,
                    castSource,
                    summonedData
            );
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}