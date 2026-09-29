package com.example.examplemod.client;


import com.example.examplemod.skill.gui.SkillTreeFragment;
import com.mojang.blaze3d.platform.InputConstants;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientKeyHandler {
    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
            "key.examplemod.open_skill_tree",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.examplemod"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILL_TREE);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_SKILL_TREE.consumeClick()) {
            MuiModApi.openScreen(new SkillTreeFragment());
        }
    }
}