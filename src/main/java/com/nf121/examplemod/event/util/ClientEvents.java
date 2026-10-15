package com.nf121.examplemod.event.util;

import com.nf121.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("entitybook")
                        .executes(context -> {
                            Minecraft minecraft = Minecraft.getInstance();

                            /*
                             * 延迟到客户端任务中打开，避免聊天界面关闭时
                             * 把刚打开的 Screen 一起关闭。
                             */
                            minecraft.tell(() -> {
                                if (minecraft.level == null) {
                                    return;
                                }

                                try {
                                    minecraft.setScreen(
                                            new AttributeBookScreen(
                                                    AttributeBookData.open()
                                            )
                                    );
                                } catch (Exception exception) {
                                    if (minecraft.player != null) {
                                        minecraft.player.displayClientMessage(
                                                Component.literal(
                                                        "打开实体图鉴失败："
                                                                + exception.getMessage()
                                                ),
                                                false
                                        );
                                    }
                                }
                            });

                            return 1;
                        })
        );
    }
}
