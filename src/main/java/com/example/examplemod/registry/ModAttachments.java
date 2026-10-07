package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.skill.PlayerSkillData;
import com.example.examplemod.util.TimedAttributeModifier;
import com.example.examplemod.util.TimedModifierTracker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ExampleMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSkillData>> SKILL_DATA = ATTACHMENTS.register(
            "skill_data",
            () -> AttachmentType.builder(PlayerSkillData::new)
                    .serialize(PlayerSkillData.CODEC)
                    .copyOnDeath() // 死亡保留
                    .build()
    );

    // 注册非持久化的 Attachment
    public static final Supplier<AttachmentType<TimedModifierTracker>> TIMED_MODIFIERS =
            ATTACHMENTS.register("timed_modifiers", () -> AttachmentType.builder(TimedModifierTracker::new).build());

    /**
     * 对外公开的主入口：像添加药水一样直接调用
     */
    public static void apply(LivingEntity entity, TimedAttributeModifier modifier) {
        if (entity == null || entity.level().isClientSide()) return;
        entity.getData(TIMED_MODIFIERS.get()).addModifier(entity, modifier);
    }
}