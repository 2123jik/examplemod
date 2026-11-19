package com.example.examplemod.event.damage;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class DamagePopupServer {

    private static final int PHYSICAL_COLOR = 0xFFFFFF; // 物理/普通伤害白色

    private static final int FLAG_TARGET_IS_PLAYER = 1;
    private static final int FLAG_ATTACKER_IS_PLAYER = 1 << 1;
    private static final int FLAG_OWNER_IS_PLAYER = 1 << 2;

    private static final Map<UUID, PlayerFilterState> PLAYER_FILTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> OWNER_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Method ISS_SUMMON_GET_OWNER = findIssSummonGetOwner();

    private static Method findIssSummonGetOwner() {
        try {
            Class<?> clazz = Class.forName("io.redspace.ironsspellbooks.capabilities.magic.SummonManager");
            return clazz.getMethod("getOwner", Entity.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_FILTERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int serverPopupMask = DamagePopupConfig.getServerPopupMask();
        int serverChatMask = DamagePopupConfig.getServerChatMask();
        if (serverPopupMask == 0 && serverChatMask == 0) {
            return;
        }

        float damage = event.getNewDamage();
        if (damage <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        int color = resolveDamageColor(source);

        // 优先取真实攻击者（例如射手），无主体时兜底取直接投射物/实体（如发射器射出的箭），再无则为 -1（环境伤害）
        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        int attackerId = attacker != null ? attacker.getId() : -1;

        // 解析最终主人（狗、傀儡、召唤物、弹射物等溯源到主人）
        Entity owner = resolveOwner(serverLevel, attacker);
        int ownerId = owner != null ? owner.getId() : -1;

        boolean targetIsPlayer = victim instanceof Player;
        boolean attackerIsPlayer = attacker instanceof Player;
        boolean ownerIsPlayer = owner instanceof Player;

        byte entityFlags = 0;
        if (targetIsPlayer) entityFlags |= FLAG_TARGET_IS_PLAYER;
        if (attackerIsPlayer) entityFlags |= FLAG_ATTACKER_IS_PLAYER;
        if (ownerIsPlayer) entityFlags |= FLAG_OWNER_IS_PLAYER;

        // 提取标准 DamageType 注册表 ID（可直接写进 data/<ns>/tags/damage_type/*.json）
        String damageTypeId = source.typeHolder()
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("minecraft:generic");

        // 生成锚点：实体胸口偏上位置
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.78;
        double z = victim.getZ();

        // 计算水平受击方向（从伤害来源指向受击者）
        float hitDirX = 0.0F;
        float hitDirZ = 0.0F;
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos != null) {
            double dx = x - sourcePos.x;
            double dz = z - sourcePos.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-4) {
                hitDirX = (float) (dx / len);
                hitDirZ = (float) (dz / len);
            }
        }

        // 综合绝对伤害与目标最大生命值占比，归一化打击权重 [0.0, 1.0]
        float maxHp = Math.max(1.0F, victim.getMaxHealth());
        float absFactor = Mth.clamp((damage - 3.0F) / 27.0F, 0.0F, 1.0F);
        float relFactor = Mth.clamp((damage / maxHp) / 0.45F, 0.0F, 1.0F);
        float impactWeight = Mth.clamp(Math.max(absFactor * 0.85F, relFactor), 0.0F, 1.0F);

        DamagePopupPayload payload = new DamagePopupPayload(
                victim.getId(),
                attackerId,
                ownerId,
                entityFlags,
                x, y, z,
                victim.getBbWidth(),
                damage,
                color,
                hitDirX, hitDirZ,
                impactWeight,
                damageTypeId
        );

        int trackingRange = Math.max(64, victim.getType().clientTrackingRange() * 16);
        double maxDistSq = (double) trackingRange * trackingRange;

        for (ServerPlayer viewer : serverLevel.players()) {
            boolean isRelated = viewer == victim || viewer == attacker || viewer == owner;
            if (!isRelated && viewer.distanceToSqr(victim) > maxDistSq) {
                continue;
            }

            PlayerFilterState clientPrefs = PLAYER_FILTERS.getOrDefault(
                    viewer.getUUID(),
                    PlayerFilterState.DEFAULT
            );

            int effectivePopupMask = serverPopupMask & clientPrefs.popupMask();
            int effectiveChatMask = serverChatMask & clientPrefs.chatMask();

            int viewerId = viewer.getId();
            boolean wantsPopup = DamagePopupConfig.matchesMask(
                    effectivePopupMask, viewerId, victim.getId(), attackerId, ownerId,
                    targetIsPlayer, attackerIsPlayer, ownerIsPlayer
            );
            boolean wantsChat = DamagePopupConfig.matchesMask(
                    effectiveChatMask, viewerId, victim.getId(), attackerId, ownerId,
                    targetIsPlayer, attackerIsPlayer, ownerIsPlayer
            );

            if (wantsPopup || wantsChat) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    private static Entity resolveOwner(Level level, Entity entity) {
        Entity current = entity;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            Entity parent = findDirectOwner(level, current);
            if (parent == null || parent == current) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private static Entity findDirectOwner(Level level, Entity entity) {
        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                return owner;
            }
            UUID ownerUuid = ownable.getOwnerUUID();
            if (ownerUuid != null && level instanceof ServerLevel serverLevel) {
                Player player = serverLevel.getPlayerByUUID(ownerUuid);
                return player != null ? player : serverLevel.getEntity(ownerUuid);
            }
        }

        if (entity instanceof TraceableEntity traceable) {
            Entity owner = traceable.getOwner();
            if (owner != null) {
                return owner;
            }
        }

        if (ISS_SUMMON_GET_OWNER != null) {
            try {
                Object result = ISS_SUMMON_GET_OWNER.invoke(null, entity);
                if (result instanceof Entity owner) {
                    return owner;
                }
            } catch (Throwable ignored) {
            }
        }

        Optional<Method> methodOpt = OWNER_METHOD_CACHE.computeIfAbsent(entity.getClass(), cls -> {
            for (String name : new String[]{"getSummoner", "getOwner"}) {
                try {
                    Method m = cls.getMethod(name);
                    if (Entity.class.isAssignableFrom(m.getReturnType())) {
                        return Optional.of(m);
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            return Optional.empty();
        });

        if (methodOpt.isPresent()) {
            try {
                Object result = methodOpt.get().invoke(entity);
                if (result instanceof Entity owner) {
                    return owner;
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    /**
     * 优先匹配原生 SpellDamageSource，其次匹配铁魔法 SchoolType 注册的 DamageType；未匹配则返回白色
     */
    private static int resolveDamageColor(DamageSource source) {
        if (source instanceof SpellDamageSource spellSource) {
            return getSchoolColor(spellSource.spell().getSchoolType());
        }
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            if (source.typeHolder().is(school.getDamageType())) {
                return getSchoolColor(school);
            }
        }
        return PHYSICAL_COLOR;
    }

    private static int getSchoolColor(SchoolType school) {
        if (school == null) {
            return PHYSICAL_COLOR;
        }
        TextColor textColor = school.getDisplayName().getStyle().getColor();
        if (textColor != null) {
            return textColor.getValue();
        }
        Vector3f rgb = school.getTargetingColor();
        return ((int) (rgb.x() * 255.0F) << 16)
                | ((int) (rgb.y() * 255.0F) << 8)
                | (int) (rgb.z() * 255.0F);
    }

    public static void handleFilterSync(DamagePopupFilterSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PLAYER_FILTERS.put(
                        player.getUUID(),
                        new PlayerFilterState(payload.popupMask(), payload.chatMask())
                );
            }
        });
    }

    private record PlayerFilterState(int popupMask, int chatMask) {
        static final PlayerFilterState DEFAULT = new PlayerFilterState(
                DamagePopupConfig.DEFAULT_MASK,
                DamagePopupConfig.DEFAULT_MASK
        );
    }

    @EventBusSubscriber(modid = ExampleMod.MODID)
    public static class NetworkRegistry {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    DamagePopupPayload.TYPE,
                    DamagePopupPayload.STREAM_CODEC,
                    DamagePopupClient::handlePayload
            );
            registrar.playToServer(
                    DamagePopupFilterSyncPayload.TYPE,
                    DamagePopupFilterSyncPayload.STREAM_CODEC,
                    DamagePopupServer::handleFilterSync
            );
        }
    }

    public record DamagePopupFilterSyncPayload(
            int popupMask,
            int chatMask
    ) implements CustomPacketPayload {

        public static final Type<DamagePopupFilterSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "damage_popup_filter_sync"));

        public static final StreamCodec<ByteBuf, DamagePopupFilterSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DamagePopupFilterSyncPayload::popupMask,
                ByteBufCodecs.VAR_INT, DamagePopupFilterSyncPayload::chatMask,
                DamagePopupFilterSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * 超过 6 个字段，NeoForge 1.21.1 使用 StreamCodec.of 手动编解码
     */
    public record DamagePopupPayload(
            int targetId,
            int attackerId,
            int ownerId,
            byte entityFlags,
            double x,
            double y,
            double z,
            float targetWidth,
            float damage,
            int color,
            float hitDirX,
            float hitDirZ,
            float impactWeight,
            String damageTypeId
    ) implements CustomPacketPayload {

        public static final Type<DamagePopupPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "damage_popup"));

        public static final StreamCodec<ByteBuf, DamagePopupPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeInt(p.targetId);
                    buf.writeInt(p.attackerId);
                    buf.writeInt(p.ownerId);
                    buf.writeByte(p.entityFlags);
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                    buf.writeFloat(p.targetWidth);
                    buf.writeFloat(p.damage);
                    buf.writeInt(p.color);
                    buf.writeFloat(p.hitDirX);
                    buf.writeFloat(p.hitDirZ);
                    buf.writeFloat(p.impactWeight);
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.damageTypeId);
                },
                buf -> new DamagePopupPayload(
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readByte(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readInt(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                )
        );

        public boolean targetIsPlayer() {
            return (entityFlags & FLAG_TARGET_IS_PLAYER) != 0;
        }

        public boolean attackerIsPlayer() {
            return (entityFlags & FLAG_ATTACKER_IS_PLAYER) != 0;
        }

        public boolean ownerIsPlayer() {
            return (entityFlags & FLAG_OWNER_IS_PLAYER) != 0;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}