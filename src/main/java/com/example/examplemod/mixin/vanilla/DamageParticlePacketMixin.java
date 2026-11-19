package com.example.examplemod.mixin.vanilla;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DamageParticlePacketMixin {
    @Inject(
        method = "handleParticleEvent",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/network/protocol/game/"
                + "ClientboundLevelParticlesPacket;getCount()I",
            ordinal = 0
        ),
        cancellable = true,
        require = 1
    )
    private void hitfx$replaceDamageBurst(
        ClientboundLevelParticlesPacket packet,
        CallbackInfo ci
    ) {
        if (packet.getParticle().getType() != ParticleTypes.DAMAGE_INDICATOR) {
            return;
        }

        // 取消原版根据 packet.count 展开的粒子循环。
        ci.cancel();
    }
}
