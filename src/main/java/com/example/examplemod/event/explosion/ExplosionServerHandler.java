package com.example.examplemod.event.explosion;

import com.example.examplemod.menu.enderinterface.EnderInterface;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExplosionServerHandler {

    // 传输数据包：增加一个 int 类型的 RGB 颜色（-1 表示无特定颜色，走默认经典烈焰）
    public record ExplosionShockwavePayload(float x, float y, float z, float maxRadius, int rgbColor) implements CustomPacketPayload {
        public static final Type<ExplosionShockwavePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnderInterface.MODID, "explosion_shockwave"));
        public static final StreamCodec<ByteBuf, ExplosionShockwavePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, ExplosionShockwavePayload::x,
                ByteBufCodecs.FLOAT, ExplosionShockwavePayload::y,
                ByteBufCodecs.FLOAT, ExplosionShockwavePayload::z,
                ByteBufCodecs.FLOAT, ExplosionShockwavePayload::maxRadius,
                ByteBufCodecs.INT, ExplosionShockwavePayload::rgbColor,
                ExplosionShockwavePayload::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = EnderInterface.MODID)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    ExplosionShockwavePayload.TYPE,
                    ExplosionShockwavePayload.STREAM_CODEC,
                    ExplosionClientRenderer::handlePacket
            );
        }
    }

    @EventBusSubscriber(modid = EnderInterface.MODID)
    public static class GameBusEvents {
        @SubscribeEvent
        public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                var explosion = event.getExplosion();
                Vec3 center = explosion.center();
                float rawRadius = explosion.radius();
                Entity directSource = explosion.getDirectSourceEntity();

                float finalRadius;
                if (rawRadius > 0.05F) {
                    finalRadius = rawRadius * 2.0F; // 原版正规爆炸
                } else {
                    finalRadius = extractPseudoExplosionRadius(directSource); // 穿透伪爆炸
                }

                int schoolColor = -1;
                if (directSource instanceof ISchoolHolder holder) {
                    // 弹幕身上已经自带了纯正的学派色彩！
                    schoolColor = holder.getSchoolColor();
                }
                PacketDistributor.sendToPlayersNear(
                        serverLevel, null,
                        center.x, center.y, center.z,
                        128.0,
                        new ExplosionShockwavePayload((float) center.x, (float) center.y, (float) center.z, finalRadius, schoolColor)
                );
            }
        }
    }

    // --- 高性能反射提取法术学派颜色 ---
    private static int extractSchoolColor(Entity entity) {
        if (entity == null) return -1;
        Class<?> clazz = entity.getClass();

        try {
            // 1. 尝试从实体获取 SchoolType (铁魔法的 MagicProjectile 经常自带 getSchoolType() 或 getSpell())
            Object schoolType = invokeMethod(entity, "getSchoolType");
            if (schoolType == null) {
                Object spell = invokeMethod(entity, "getSpell");
                if (spell != null) {
                    schoolType = invokeMethod(spell, "getSchoolType");
                }
            }

            // 2. 如果找到了 SchoolType 对象，直接读其 Style 中的颜色
            if (schoolType != null) {
                // 方式 A: 直接调用 getTargetingColor() 或者从 displayStyle 获取
                Object style = invokeMethod(schoolType, "getDisplayStyle");
                if (style == null) {
                    Field f = schoolType.getClass().getDeclaredField("displayStyle");
                    f.setAccessible(true);
                    style = f.get(schoolType);
                }
                if (style instanceof net.minecraft.network.chat.Style s && s.getColor() != null) {
                    return s.getColor().getValue(); // 拿到权威 RGB 色值！例如 0x3accd3
                }
            }
        } catch (Exception ignored) {}

        return -1; // 找不到则返回 -1 走默认
    }

    private static Object invokeMethod(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    // --- 提取半径 ---
    private static final Map<Class<?>, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private static float extractPseudoExplosionRadius(Entity entity) {
        if (entity == null) return 2.5F;
        Class<?> clazz = entity.getClass();
        try {
            Method m = METHOD_CACHE.computeIfAbsent(clazz, c -> {
                for (String name : new String[]{"getExplosionRadius", "getRadius", "explosionRadius"}) {
                    try {
                        Method method = c.getMethod(name);
                        method.setAccessible(true);
                        return method;
                    } catch (NoSuchMethodException ignored) {}
                }
                return null;
            });
            if (m != null) {
                Object res = m.invoke(entity);
                if (res instanceof Number num && num.floatValue() > 0.1F) return num.floatValue();
            }
        } catch (Exception ignored) {}

        try {
            Field f = FIELD_CACHE.computeIfAbsent(clazz, c -> {
                Class<?> cur = c;
                while (cur != null && cur != Object.class) {
                    try {
                        Field field = cur.getDeclaredField("explosionRadius");
                        field.setAccessible(true);
                        return field;
                    } catch (NoSuchFieldException e) { cur = cur.getSuperclass(); }
                }
                return null;
            });
            if (f != null) {
                Object val = f.get(entity);
                if (val instanceof Number num && num.floatValue() > 0.1F) return num.floatValue();
            }
        } catch (Exception ignored) {}

        return 2.5F;
    }
}