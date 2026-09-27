package com.example.examplemod.content.spell;

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

public class SummonCaveSpiderSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("examplemod", "summon_cave_spider");
    private final DefaultConfig defaultConfig;

    public SummonCaveSpiderSpell() {
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
                .setMaxLevel(10)
                .setCooldownSeconds(120.0)
                .build();

        this.baseManaCost = 40;
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
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
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    public int getSpiderCount(int level) {
        if (level <= 0) return 0;
        if (level < 3) return 1;
        if (level < 5) return 2;
        if (level < 8) return 3;
        if (level < 11) return 4;
        if (level < 15) return 5;
        if (level < 19) return 6;
        return 7;     }

    public int getBonusLevels(int level) {
        return Math.max(0, level - 19);
    }

    public float getSpiderHealth(int spellLevel, LivingEntity caster) {
        int bonus = getBonusLevels(spellLevel);
        float base = 12.0F + (spellLevel * 2.0F) + (bonus * 6.0F);
        return base * this.getEntityPowerMultiplier(caster);
    }

    public float getSpiderDamage(int spellLevel, LivingEntity caster) {
        int bonus = getBonusLevels(spellLevel);
        return this.getSpellPower(spellLevel, caster) + (bonus * 1.5F);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.literal("Count: " + getSpiderCount(spellLevel)),
                Component.translatable("ui.irons_spellbooks.hp", Utils.stringTruncation(getSpiderHealth(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getSpiderDamage(spellLevel, caster), 1))
        );
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

        if (!recasts.hasRecastForSpell(this)) {
            SummonedEntitiesCastData summonedCastData = new SummonedEntitiesCastData();
            int summonTime = 12000;             int count = getSpiderCount(spellLevel);
            float hp = getSpiderHealth(spellLevel, entity);
            float dmg = getSpiderDamage(spellLevel, entity);

            for (int i = 0; i < count; i++) {
                SummonedCaveSpider spider = new SummonedCaveSpider(ModEntities.SUMMONED_CAVE_SPIDER.get(), world);

                double angle = (2 * Math.PI / Math.max(1, count)) * i;
                double offsetX = 1.5 * Math.cos(angle);
                double offsetZ = 1.5 * Math.sin(angle);
                spider.moveTo(entity.getX() + offsetX, entity.getY(), entity.getZ() + offsetZ, entity.getYRot(), 0.0F);

                if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
                    spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    spider.setHealth(hp);
                }
                if (spider.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                    spider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
                }

                SpellSummonEvent event = new SpellSummonEvent(entity, spider, this.spellId, spellLevel);
                NeoForge.EVENT_BUS.post(event);
                LivingEntity finalCreature = event.getCreature();

                world.addFreshEntity(finalCreature);
                SummonManager.initSummon(entity, finalCreature, summonTime, summonedCastData);
            }

            RecastInstance recastInstance = new RecastInstance(
                    this.getSpellId(),
                    spellLevel,
                    this.getRecastCount(spellLevel, entity),
                    summonTime,
                    castSource,
                    summonedCastData
            );
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}