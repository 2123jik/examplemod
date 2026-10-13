package com.example.examplemod.compat.jade;

import com.example.examplemod.client.jade.CustomStatusEffectsClientProvider;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ExampleJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 服务端 + 客户端公共注册：为所有 LivingEntity 提供数据
        registration.registerEntityDataProvider(
                MobAttackServerProvider.INSTANCE,
                LivingEntity.class
        );
        registration.registerEntityDataProvider(CustomStatusEffectsServerProvider.INSTANCE, LivingEntity.class);

    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 客户端注册：渲染
        registration.registerEntityComponent(
                MobAttackClientProvider.INSTANCE,
                LivingEntity.class
        );
        registration.registerEntityComponent(CustomStatusEffectsClientProvider.INSTANCE, LivingEntity.class);
    }

}