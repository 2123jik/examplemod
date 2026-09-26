package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = GemItem.class, remap = false)
public abstract class MixinGemItem extends Item {

    public MixinGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        CustomData customData = heldStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (tag.getBoolean("is_unidentified_gem")) {
            if (level.isClientSide) {
                return InteractionResultHolder.sidedSuccess(heldStack, true);
            }

            // 【防误放锁 1】：前置背包空间检测
            int freeSlot = player.getInventory().getFreeSlot();
            if (freeSlot == -1 && heldStack.getCount() > 1) {
                player.sendSystemMessage(Component.literal("背包已满，无法鉴定！请腾出空间后再试。").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(heldStack);
            }

            // 1. 严格获取箱子生成时锁死的纯度
            Purity purity = GemItem.getPurity(heldStack);
            RandomSource random = player.getRandom();

            // 2. 筛选出支持此纯度的宝石
            List<Gem> validGems = GemRegistry.INSTANCE.getValues().stream()
                    .filter(g -> purity.isAtLeast(g.getMinPurity()))
                    .toList();

            if (validGems.isEmpty()) {
                return InteractionResultHolder.fail(heldStack);
            }

            Gem selectedGem = validGems.get(random.nextInt(validGems.size()));
            ItemStack identifiedGem = selectedGem.toStack(purity);

            // 3. 扣除原石
            heldStack.shrink(1);

            // 【防误放锁 2】：手持位隔离放入 + 双端网络同步
            boolean storedInInventory = false;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                if (hand == InteractionHand.MAIN_HAND && i == player.getInventory().selected) {
                    continue;
                }
                ItemStack slotStack = player.getInventory().items.get(i);
                if (slotStack.isEmpty()) {
                    player.getInventory().items.set(i, identifiedGem);
                    storedInInventory = true;
                    break;
                }
            }

            if (!storedInInventory) {
                if (heldStack.isEmpty()) {
                    player.setItemInHand(hand, identifiedGem);
                } else {
                    player.drop(identifiedGem, false);
                }
            }

            // 双端广播防止幽灵物品
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }

            // 【防误放锁 3】：12 tick 冷却
            player.getCooldowns().addCooldown(this, 12);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.sendSystemMessage(Component.literal("鉴定出宝石: ").withStyle(ChatFormatting.GOLD)
                    .append(identifiedGem.getHoverName()));

            return InteractionResultHolder.consume(heldStack);
        }

        return super.use(level, player, hand);
    }

    // =========================================================================
    // 1. 名字显示：未鉴定的[完美]宝石原石
    // =========================================================================
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.copyTag().getBoolean("is_unidentified_gem")) {
            Purity purity = GemItem.getPurity(stack);
            // 拼接效果：未鉴定的[完美]宝石原石
            cir.setReturnValue(Component.literal("未鉴定的")
                    .append(purity.toComponent())
                    .append(Component.literal("宝石")));
        }
    }

    // =========================================================================
    // 2. Tooltip 显示：纯度放前面，自然句式
    // =========================================================================
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.copyTag().getBoolean("is_unidentified_gem")) {
            lines.add(Component.literal("右键鉴定"));
            ci.cancel();
        }
    }
}