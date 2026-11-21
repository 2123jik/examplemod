package com.example.examplemod.client.input;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.skill.gui.SkillTreeFragment;
import com.mojang.blaze3d.platform.InputConstants;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class SkillTreeKeyHandler {

    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
            "key.examplemod.open_skill_tree",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.examplemod"
    );

    private SkillTreeKeyHandler() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILL_TREE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_SKILL_TREE.consumeClick()) {
            MuiModApi.openScreen(new SkillTreeFragment());
        }
    }
}