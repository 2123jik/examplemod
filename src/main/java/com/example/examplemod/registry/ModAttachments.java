package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.skill.PlayerSkillData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ExampleMod.MODID);

    public static final Supplier<AttachmentType<PlayerSkillData>> SKILL_DATA = ATTACHMENTS.register(
            "skill_data",
            () -> AttachmentType.builder(PlayerSkillData::new)
                    .serialize(PlayerSkillData.CODEC)
                    .copyOnDeath() // 死亡保留
                    .build()
    );
}