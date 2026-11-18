package com.example.examplemod.skill;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModPayloads;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.example.examplemod.registry.ModRegistry.SKILL_DATA;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class SkillEvents {

    // 玩家登录/重进时，重新挂载技能属性并同步数据到客户端 UI
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new ModPayloads.SyncSkillDataPayload(data));
        }
    }

    // 玩家死亡复活时，重新绑定属性修饰符并重新同步客户端
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new ModPayloads.SyncSkillDataPayload(data));
        }
    }

    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent.Post event) {
        // 1. 判定是否为玩家，且必须只在服务端运行
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // 2. 检查玩家是否解锁了该被动（换成你在技能树里配置的节点 ID，如 "warrior_mastery" 或 "1"）
        int rank = player.getData(SKILL_DATA).getRank("warrior_mastery");
        if (rank <= 0) {
            return;
        }

        // 3. 判定残血触发线（血量 < 30% 且玩家还活着）
        if (player.getHealth() > 0 && player.getHealth() < 0.3F * player.getMaxHealth()) {
            AbstractSpell spell = SpellRegistry.FORTIFY_SPELL.get();
            MagicData magicData = MagicData.getPlayerMagicData(player);

            if (!magicData.getPlayerCooldowns().isOnCooldown(spell)) {
                spell.onCast(player.level(), rank, player, CastSource.NONE, magicData);
                magicData.getPlayerCooldowns().addCooldown(spell, spell.getSpellCooldown());
            }
        }
    }
}