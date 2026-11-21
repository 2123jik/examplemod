package com.example.examplemod.spell;

import com.example.examplemod.ExampleMod;
import com.google.gson.Gson;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class SummonWitchSpell extends AbstractSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "summon_witch");

    public static final SummonWitchSpell INSTANCE = new SummonWitchSpell();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE) // 唤魔派系，也可改为 NATURE / ELDRITCH 等
            .setMaxLevel(5)
            .setCooldownSeconds(90)
            .build();

    public SummonWitchSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 20; // 1秒吟唱；若想瞬发可改 CastType.INSTANT 并把这里设为 0
        this.baseManaCost = 60;
    }

    /** 注解自动注册法术，无需改动主类 */
    @SubscribeEvent
    public static void onRegisterSpells(RegisterEvent event) {
        event.register(SpellRegistry.SPELL_REGISTRY_KEY, helper ->
                helper.register(SPELL_ID, INSTANCE));
    }
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        // 1. 直接在代码里写翻译表（以后加新法术/物品往 Map 里继续塞就行）
        byte[] zhBytes = toLangJson(Map.of(
                "spell." + ExampleMod.MODID + ".summon_witch", "召唤女巫",
                "spell." + ExampleMod.MODID + ".summon_witch.guide", "召唤一名为你投掷药水助战的友好女巫。"
        ));
        byte[] enBytes = toLangJson(Map.of(
                "spell." + ExampleMod.MODID + ".summon_witch", "Summon Witch",
                "spell." + ExampleMod.MODID + ".summon_witch.guide", "Summons a friendly witch to throw potions at your enemies."
        ));
        byte[] mcmetaBytes = "{\"pack\":{\"description\":\"Virtual Lang\",\"pack_format\":34}}"
                .getBytes(StandardCharsets.UTF_8);

        PackLocationInfo info = new PackLocationInfo(
                "mod/" + ExampleMod.MODID + "_virtual_lang",
                Component.literal(ExampleMod.MODID + " Virtual Lang"),
                PackSource.BUILT_IN,
                Optional.empty() // 客户端纯内存资源包无需 KnownPack（KnownPack 主要用于服务端数据包免同步校验）
        );

        PackResources virtualPack = new PackResources() {
            @Override
            public IoSupplier<InputStream> getRootResource(String... elements) {
                return elements.length == 1 && PACK_META.equals(elements[0])
                        ? () -> new ByteArrayInputStream(mcmetaBytes) : null;
            }

            @Override
            public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
                if (type == PackType.CLIENT_RESOURCES && loc.getNamespace().equals(ExampleMod.MODID)) {
                    if ("lang/zh_cn.json".equals(loc.getPath())) return () -> new ByteArrayInputStream(zhBytes);
                    if ("lang/en_us.json".equals(loc.getPath())) return () -> new ByteArrayInputStream(enBytes);
                }
                return null;
            }

            @Override
            public void listResources(PackType type, String ns, String path, ResourceOutput out) {
                if (type == PackType.CLIENT_RESOURCES && ns.equals(ExampleMod.MODID) && (path.isEmpty() || "lang".equals(path))) {
                    out.accept(ResourceLocation.fromNamespaceAndPath(ns, "lang/zh_cn.json"), () -> new ByteArrayInputStream(zhBytes));
                    out.accept(ResourceLocation.fromNamespaceAndPath(ns, "lang/en_us.json"), () -> new ByteArrayInputStream(enBytes));
                }
            }

            @Override
            public Set<String> getNamespaces(PackType type) {
                return type == PackType.CLIENT_RESOURCES ? Set.of(ExampleMod.MODID) : Set.of();
            }

            @Override
            public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
                try (InputStream in = new ByteArrayInputStream(mcmetaBytes)) {
                    return AbstractPackResources.getMetadataFromStream(serializer, in);
                }
            }

            @Override
            public PackLocationInfo location() { return info; }

            @Override
            public boolean isHidden() { return true; } // 静默生效，不在客户端“资源包”界面显示

            @Override
            public void close() {}
        };

        // 对照 AddPackFindersEvent 源码第 80~87 行的精简写法：
        Pack pack = Pack.readMetaAndCreate(
                info,
                BuiltInPackSource.fromName(loc -> virtualPack),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.BOTTOM, true) // 设为 BOTTOM，方便玩家外挂资源包覆盖汉化
        );

        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }

    private static byte[] toLangJson(Map<String, String> map) {
        return new Gson().toJson(map).getBytes(StandardCharsets.UTF_8);
    }
    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }



    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int seconds = getSummonTime(spellLevel, caster) / 20;
        return List.of(
                Component.literal("召唤原版女巫: 1 名"),
                Component.literal("持续时间: " + seconds + " 秒")
        );
    }

    /** 启用二段施法（再次施放可提前解除召唤） */
    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance,
                                 RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    private int getSummonTime(int spellLevel, @Nullable LivingEntity caster) {
        // 基础 60 秒 + 每级/法强额外延长
        return 20 * (60 + (int) (getSpellPower(spellLevel, caster) * 15));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide()) {
            PlayerRecasts recasts = playerMagicData.getPlayerRecasts();
            if (!recasts.hasRecastForSpell(this)) {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();
                int summonTime = getSummonTime(spellLevel, entity);

                Witch witch = EntityType.WITCH.create(level);
                if (witch != null) {
                    Vec3 forward = entity.getForward().scale(1.5);
                    witch.moveTo(entity.getX() + forward.x, entity.getY(), entity.getZ() + forward.z, entity.getYRot(), 0);
                    witch.setCanJoinRaid(false);      // 不参与村庄袭击
                    witch.setPersistenceRequired();   // 销毁交由 SummonManager 统一控制

                    // 先绑定 SummonManager 所有者与计时，再加入世界，确保 EntityJoinLevelEvent 能立刻识别
                    SummonManager.initSummon(entity, witch, summonTime, castData);
                    level.addFreshEntity(witch);

                    RecastInstance recast = new RecastInstance(
                            this.getSpellId(),
                            spellLevel,
                            getRecastCount(spellLevel, entity),
                            summonTime,
                            castSource,
                            castData
                    );
                    recasts.addRecast(recast, playerMagicData);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // ==================== 召唤物 AI 接管与友伤保护（无需新建其他类） ====================

    @EventBusSubscriber(modid = ExampleMod.MODID)
    public static class WitchSummonEvents {

        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;
            if (!(event.getEntity() instanceof Witch witch)) return;
            if (SummonManager.getOwner(witch) == null) return;

            boolean alreadySetup = witch.targetSelector.getAvailableGoals().stream()
                    .anyMatch(g -> g.getGoal() instanceof SummonedWitchTargetGoal);
            if (!alreadySetup) {
                witch.setCanJoinRaid(false);
                // 清除原版索敌 AI
                witch.targetSelector.removeAllGoals(goal -> true);
                witch.targetSelector.addGoal(1, new SummonedWitchTargetGoal(witch));

                // 关键：移除原版会给 Raider 丢回血药水的 RangedAttackGoal，换成我们自己的攻击 Goal
                witch.goalSelector.removeAllGoals(goal -> goal instanceof RangedAttackGoal);
                witch.goalSelector.addGoal(2, new SummonedWitchAttackGoal(witch));
                witch.goalSelector.addGoal(3, new SummonedWitchFollowOwnerGoal(witch));
            }
        }
        /** 自定义远程攻击：彻底绕开原版 Witch 对 Raider（含女巫/掠夺者）丢回血药水的坑 */
        private static class SummonedWitchAttackGoal extends Goal {
            private final Witch witch;
            private int attackTime = 0;

            public SummonedWitchAttackGoal(Witch witch) {
                this.witch = witch;
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                LivingEntity target = witch.getTarget();
                return target != null && target.isAlive();
            }

            @Override
            public void stop() {
                this.attackTime = 0;
            }

            @Override
            public void tick() {
                LivingEntity target = witch.getTarget();
                if (target == null || !target.isAlive()) return;

                double distSqr = witch.distanceToSqr(target);
                boolean hasLos = witch.getSensing().hasLineOfSight(target);

                if (distSqr <= 100.0 && hasLos) { // 10 格内且有视野：停下丢药水
                    witch.getNavigation().stop();
                } else {
                    witch.getNavigation().moveTo(target, 1.0);
                }

                witch.getLookControl().setLookAt(target, 30.0F, 30.0F);

                if (--this.attackTime <= 0) {
                    if (!hasLos || distSqr > 144.0 || witch.isDrinkingPotion()) return;
                    throwPotion(target);
                    this.attackTime = 35; // 约 1.75 秒丢一瓶，比原版更积极
                }
            }

            private void throwPotion(LivingEntity target) {
                Vec3 movement = target.getDeltaMovement();
                double dx = target.getX() + movement.x - witch.getX();
                double dy = target.getEyeY() - 1.1 - witch.getY();
                double dz = target.getZ() + movement.z - witch.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                Holder<Potion> chosen = Potions.STRONG_HARMING;
                if (target.isInvertedHealAndHarm()) {
                    chosen = Potions.STRONG_HEALING; // 亡灵怪改用治疗 II 炸
                } else if (!(target instanceof Witch)) {
                    if (horizontalDist >= 8.0 && !target.hasEffect(MobEffects.SLOW_FALLING)) {
                        chosen = Potions.SLOWNESS;
                    } else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
                        chosen = Potions.POISON;
                    }
                }

                ThrownPotion potion = new ThrownPotion(witch.level(), witch);
                potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, chosen));
                potion.shoot(dx, dy + horizontalDist * 0.2, dz, 0.75F, 3.0F);

                if (!witch.isSilent()) {
                    witch.level().playSound(null, witch.getX(), witch.getY(), witch.getZ(),
                            SoundEvents.WITCH_THROW, witch.getSoundSource(),
                            1.0F, 0.8F + witch.getRandom().nextFloat() * 0.4F);
                }
                witch.level().addFreshEntity(potion);
            }
        }
        /** 防止召唤女巫丢出的伤害药水误伤召唤者或同主的其它召唤物 */
        @SubscribeEvent
        public static void onIncomingDamage(LivingIncomingDamageEvent event) {
            if (event.getSource().getEntity() instanceof Witch witch) {
                Entity owner = SummonManager.getOwner(witch);
                if (owner != null && (event.getEntity() == owner || SummonManager.getOwner(event.getEntity()) == owner)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /** 自定义索敌：护主 -> 集火主人攻击的目标 -> 反击 -> 周围敌对生物 */
    private static class SummonedWitchTargetGoal extends TargetGoal {
        private LivingEntity pendingTarget;

        public SummonedWitchTargetGoal(Witch witch) {
            super(witch, false);
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!(SummonManager.getOwner(mob) instanceof LivingEntity owner) || !owner.isAlive()) {
                return false;
            }
            if (isValidTarget(owner.getLastHurtByMob(), owner)) {
                pendingTarget = owner.getLastHurtByMob();
                return true;
            }
            if (isValidTarget(owner.getLastHurtMob(), owner)) {
                pendingTarget = owner.getLastHurtMob();
                return true;
            }
            if (isValidTarget(mob.getLastHurtByMob(), owner)) {
                pendingTarget = mob.getLastHurtByMob();
                return true;
            }
            List<LivingEntity> enemies = mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    mob.getBoundingBox().inflate(16.0),
                    e -> e instanceof Enemy && isValidTarget(e, owner)
            );
            enemies.sort(Comparator.comparingDouble(mob::distanceToSqr));
            if (!enemies.isEmpty()) {
                pendingTarget = enemies.get(0);
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(pendingTarget);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (!(SummonManager.getOwner(mob) instanceof LivingEntity owner)) {
                return false;
            }
            LivingEntity target = mob.getTarget();
            return isValidTarget(target, owner) && mob.distanceToSqr(target) < 24 * 24;
        }

        private boolean isValidTarget(@Nullable LivingEntity target, LivingEntity owner) {
            if (target == null || !target.isAlive() || target == mob || target == owner) {
                return false;
            }
            if (SummonManager.getOwner(target) == owner) {
                return false;
            }
            return !target.isAlliedTo(owner);
        }
    }

    /** 自定义跟随：脱战时跟随召唤者，过远自动传送回身边 */
    private static class SummonedWitchFollowOwnerGoal extends Goal {
        private final Witch witch;

        public SummonedWitchFollowOwnerGoal(Witch witch) {
            this.witch = witch;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(SummonManager.getOwner(witch) instanceof LivingEntity owner) || !owner.isAlive()) {
                return false;
            }
            if (witch.getTarget() != null && witch.distanceToSqr(owner) < 24 * 24) {
                return false;
            }
            return witch.distanceToSqr(owner) > 10 * 10;
        }

        @Override
        public boolean canContinueToUse() {
            if (!(SummonManager.getOwner(witch) instanceof LivingEntity owner) || !owner.isAlive()) {
                return false;
            }
            if (witch.getTarget() != null && witch.distanceToSqr(owner) < 24 * 24) {
                return false;
            }
            return witch.distanceToSqr(owner) > 6 * 6;
        }

        @Override
        public void tick() {
            if (!(SummonManager.getOwner(witch) instanceof LivingEntity owner)) {
                return;
            }
            if (witch.distanceToSqr(owner) > 28 * 28) {
                witch.moveTo(owner.getX(), owner.getY(), owner.getZ(), witch.getYRot(), witch.getXRot());
            } else {
                witch.getNavigation().moveTo(owner, 1.15);
            }
        }
    }
}