package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.MultiMenuManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ServerGamePacketListenerImpl.class)
public class PacketListenerMixin {
    @Shadow public ServerPlayer player;
    @Unique private AbstractContainerMenu enderinterface$savedMenu;

    @Unique
    private void enderinterface$swapIn(int containerId) {
        if (!this.player.server.isSameThread()) return;
        AbstractContainerMenu target = MultiMenuManager.getMenu(this.player, containerId);
        if (target != null && target != this.player.containerMenu) {
            this.enderinterface$savedMenu = this.player.containerMenu;
            this.player.containerMenu = target;
        }
    }

    @Unique
    private void enderinterface$swapOutAndSync() {
        if (MultiMenuManager.hasAny(this.player)) {
            AbstractContainerMenu active = this.player.containerMenu;
            ItemStack carried = active.getCarried();
            Map<Integer, MultiMenuManager.RemoteSession> sessions = MultiMenuManager.getSessions(this.player);
            if (sessions != null) {
                for (MultiMenuManager.RemoteSession s : sessions.values()) {
                    AbstractContainerMenu m = s.menu();
                    if (m != active) {
                        m.setCarried(carried.copy());
                        m.broadcastChanges();
                    }
                }
            }
            if (this.player.inventoryMenu != active) {
                this.player.inventoryMenu.setCarried(carried.copy());
                this.player.inventoryMenu.broadcastChanges();
            }
        }
        if (this.enderinterface$savedMenu != null) {
            this.player.containerMenu = this.enderinterface$savedMenu;
            this.enderinterface$savedMenu = null;
        }
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"))
    private void preClick(ServerboundContainerClickPacket p, CallbackInfo ci) { enderinterface$swapIn(p.getContainerId()); }

    @Inject(method = "handleContainerClick", at = @At("RETURN"))
    private void postClick(ServerboundContainerClickPacket p, CallbackInfo ci) { enderinterface$swapOutAndSync(); }

    @Inject(method = "handleContainerButtonClick", at = @At("HEAD"))
    private void preBtn(ServerboundContainerButtonClickPacket p, CallbackInfo ci) { enderinterface$swapIn(p.containerId()); }

    @Inject(method = "handleContainerButtonClick", at = @At("RETURN"))
    private void postBtn(ServerboundContainerButtonClickPacket p, CallbackInfo ci) { enderinterface$swapOutAndSync(); }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"))
    private void preRecipe(ServerboundPlaceRecipePacket p, CallbackInfo ci) { enderinterface$swapIn(p.getContainerId()); }

    @Inject(method = "handlePlaceRecipe", at = @At("RETURN"))
    private void postRecipe(ServerboundPlaceRecipePacket p, CallbackInfo ci) { enderinterface$swapOutAndSync(); }

    @Inject(method = "handleContainerSlotStateChanged", at = @At("HEAD"))
    private void preCrafter(ServerboundContainerSlotStateChangedPacket p, CallbackInfo ci) { enderinterface$swapIn(p.containerId()); }

    @Inject(method = "handleContainerSlotStateChanged", at = @At("RETURN"))
    private void postCrafter(ServerboundContainerSlotStateChangedPacket p, CallbackInfo ci) { enderinterface$swapOutAndSync(); }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void routeClose(ServerboundContainerClosePacket p, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) return;
        if (MultiMenuManager.hasAny(this.player)) {
            int id = p.getContainerId();
            if (id == 0) {
                if (this.player.containerMenu != this.player.inventoryMenu) {
                    ci.cancel();
                }
                return;
            }
            if (MultiMenuManager.getMenu(this.player, id) != null) {
                MultiMenuManager.closeOne(this.player, id);
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = {"handleContainerClick", "handleContainerButtonClick", "handlePlaceRecipe", "handleSetBeaconPacket"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z")
    )
    private boolean bypassPacketStillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        if (MultiMenuManager.isRemoteMenuValid(menu, player)) return true;
        return original.call(menu, player);
    }

    @WrapOperation(
            method = "handleRenameItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z")
    )
    private boolean bypassAnvilStillValid(AnvilMenu menu, Player player, Operation<Boolean> original) {
        if (MultiMenuManager.isRemoteMenuValid(menu, player)) return true;
        return original.call(menu, player);
    }

    // 1. 防呆保护：禁止把接口放进它自己打开的容器里（涵盖左键放入、Shift快捷移动、数字键/副手交换、拖拽铺放）
    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void guardSelfNesting(ServerboundContainerClickPacket p, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) return;
        MultiMenuManager.RemoteSession session = MultiMenuManager.getSession(this.player, p.getContainerId());

        if (session == null) return;

        AbstractContainerMenu menu = session.menu();
        int idx = p.getSlotNum();
        Slot slot = (idx >= 0 && idx < menu.slots.size()) ? menu.getSlot(idx) : null;
        boolean blocked = false;

        if (p.getClickType() == ClickType.QUICK_CRAFT) {
            // 拖拽铺放时如果鼠标抓着自指接口，直接拦截
            if (MultiMenuManager.isSelfPointingInterface(menu.getCarried(), session)) {
                blocked = true;
            }
        } else if (slot != null && slot.container != this.player.getInventory()) {
            // 操作的是容器本身（非玩家背包）的槽位
            if (MultiMenuManager.isSelfPointingInterface(menu.getCarried(), session)) {
                blocked = true;
            } else if (p.getClickType() == ClickType.SWAP) {
                int btn = p.getButtonNum();
                ItemStack hotbar = (btn == 40)
                        ? this.player.getOffhandItem()
                        : (btn >= 0 && btn < 9 ? this.player.getInventory().getItem(btn) : ItemStack.EMPTY);
                if (MultiMenuManager.isSelfPointingInterface(hotbar, session)) {
                    blocked = true;
                }
            }
        } else if (slot != null && p.getClickType() == ClickType.QUICK_MOVE) {
            // Shift+左键从玩家背包往容器里快速移动
            if (MultiMenuManager.isSelfPointingInterface(slot.getItem(), session)) {
                blocked = true;
            }
        }

        if (blocked) {
            menu.sendAllDataToRemote();
            this.player.displayClientMessage(Component.translatableWithFallback(
                    "message.examplemod.interface.self_nest_blocked",
                    "§c不能把末影接口放进它自己指向的容器里！"
            ), true);
            ci.cancel();
        }
    }
    @WrapOperation(
            method = "handleSelectTrade",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z")
    )
    private boolean bypassMerchantStillValid(net.minecraft.world.inventory.MerchantMenu menu, Player player, Operation<Boolean> original) {
        if (MultiMenuManager.isRemoteMenuValid(menu, player)) return true;
        return original.call(menu, player);
    }
}