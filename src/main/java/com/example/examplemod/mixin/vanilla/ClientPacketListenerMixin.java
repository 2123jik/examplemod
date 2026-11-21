package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.MultiWindowClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"), cancellable = true)
    private void routeSetSlot(ClientboundContainerSetSlotPacket p, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        int id = p.getContainerId();
        if (id == -1) {
            MultiWindowClient.broadcastCarried(p.getItem());
            return;
        }
        if (id > 0 && (mc.player == null || id != mc.player.containerMenu.containerId)) {
            AbstractContainerMenu m = MultiWindowClient.findMenu(id);
            if (m != null) {
                m.setItem(p.getSlot(), p.getStateId(), p.getItem());
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"), cancellable = true)
    private void routeContent(ClientboundContainerSetContentPacket p, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        int id = p.getContainerId();
        if (id > 0 && (mc.player == null || id != mc.player.containerMenu.containerId)) {
            AbstractContainerMenu m = MultiWindowClient.findMenu(id);
            if (m != null) {
                m.initializeContents(p.getStateId(), p.getItems(), p.getCarriedItem());
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleContainerSetData", at = @At("HEAD"), cancellable = true)
    private void routeData(ClientboundContainerSetDataPacket p, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        int id = p.getContainerId();
        if (id > 0 && (mc.player == null || id != mc.player.containerMenu.containerId)) {
            AbstractContainerMenu m = MultiWindowClient.findMenu(id);
            if (m != null) {
                m.setData(p.getId(), p.getValue());
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void routeServerClose(ClientboundContainerClosePacket p, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;

        if (MultiWindowClient.findMenu(p.getContainerId()) != null) {
            MultiWindowClient.handleServerClose(p.getContainerId());
            ci.cancel();
        }
    }
    @org.spongepowered.asm.mixin.Unique
    private AbstractContainerMenu enderinterface$savedMerchantMenu;

    @Inject(method = "handleMerchantOffers", at = @At("HEAD"))
    private void preMerchantOffers(net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket p, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.player == null) return;
        int id = p.getContainerId();
        if (id > 0 && id != mc.player.containerMenu.containerId) {
            AbstractContainerMenu m = MultiWindowClient.findMenu(id);
            if (m != null) {
                this.enderinterface$savedMerchantMenu = mc.player.containerMenu;
                mc.player.containerMenu = m;
            }
        }
    }

    @Inject(method = "handleMerchantOffers", at = @At("RETURN"))
    private void postMerchantOffers(net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket p, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (this.enderinterface$savedMerchantMenu != null && mc.player != null) {
            mc.player.containerMenu = this.enderinterface$savedMerchantMenu;
            this.enderinterface$savedMerchantMenu = null;
        }
    }
}