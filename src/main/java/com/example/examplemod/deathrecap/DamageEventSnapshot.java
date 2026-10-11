package com.example.examplemod.deathrecap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class DamageEventSnapshot {
    public final long timeMillis;
    public final float damage;
    public final String damageTypeId;

    // 1. 核心杀手锏：案发调用栈（谁的代码在打你）
    public final List<String> callStack;

    // 2. 参与实体快照（内部已包含完整的 entityNbt、装备、属性与 3D 渲染所需数据）
    public final EntitySnapshot victim;
    public final EntitySnapshot attacker; // 若无直接生物攻击者（如摔死、虚空、火烧）则为 null

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageEventSnapshot> STREAM_CODEC = StreamCodec.of(
            DamageEventSnapshot::write,
            DamageEventSnapshot::read
    );

    // 服务端受击时调用的构造函数
    public DamageEventSnapshot(DamageSource source, float damage, LivingEntity victimEntity) {
        this.timeMillis = System.currentTimeMillis();
        this.damage = damage;
        this.damageTypeId = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown");

        // 捕获受击瞬间整个 JVM 的方法调用栈
        this.callStack = new ArrayList<>();
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        for (StackTraceElement elem : traces) {
            String line = elem.toString();
            // 过滤无用的底层反射与通用事件派发逻辑，优先保留核心代码与 Mod 类
            if (!line.startsWith("java.lang") && !line.startsWith("net.neoforged.bus")) {
                this.callStack.add(line);
                if (this.callStack.size() >= 30) break; // 保存最近 30 层调用栈
            }
        }

        // 保存受害者（玩家自身）的完整快照（包含全量 NBT、状态、装备）
        this.victim = new EntitySnapshot(victimEntity);

        // 保存攻击者的完整快照（若无攻击者则为 null）
        this.attacker = (source.getEntity() instanceof LivingEntity livingAttacker)
                ? new EntitySnapshot(livingAttacker) : null;
    }

    // 网络反序列化构造函数
    public DamageEventSnapshot(long timeMillis, float damage, String damageTypeId, List<String> callStack,
                               EntitySnapshot victim, EntitySnapshot attacker) {
        this.timeMillis = timeMillis;
        this.damage = damage;
        this.damageTypeId = damageTypeId;
        this.callStack = callStack;
        this.victim = victim;
        this.attacker = attacker;
    }

    private static void write(RegistryFriendlyByteBuf buf, DamageEventSnapshot s) {
        ByteBufCodecs.VAR_LONG.encode(buf, s.timeMillis);
        ByteBufCodecs.FLOAT.encode(buf, s.damage);
        ByteBufCodecs.STRING_UTF8.encode(buf, s.damageTypeId);
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, s.callStack);

        EntitySnapshot.STREAM_CODEC.encode(buf, s.victim);
        ByteBufCodecs.BOOL.encode(buf, s.attacker != null);
        if (s.attacker != null) {
            EntitySnapshot.STREAM_CODEC.encode(buf, s.attacker);
        }
    }

    private static DamageEventSnapshot read(RegistryFriendlyByteBuf buf) {
        long time = ByteBufCodecs.VAR_LONG.decode(buf);
        float dmg = ByteBufCodecs.FLOAT.decode(buf);
        String type = ByteBufCodecs.STRING_UTF8.decode(buf);
        List<String> stack = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);

        EntitySnapshot victim = EntitySnapshot.STREAM_CODEC.decode(buf);
        boolean hasAttacker = ByteBufCodecs.BOOL.decode(buf);
        EntitySnapshot attacker = hasAttacker ? EntitySnapshot.STREAM_CODEC.decode(buf) : null;

        return new DamageEventSnapshot(time, dmg, type, stack, victim, attacker);
    }
}