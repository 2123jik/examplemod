package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.UnidentifiedItemHelper;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = GemItem.class, remap = false)
public abstract class MixinGemItem extends Item {

    public MixinGemItem(Properties properties) { super(properties); }

    @Unique
    private static CompoundTag examplemod$readTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.getUnsafe();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        CompoundTag tag = examplemod$readTag(heldStack);

        if (!tag.getBoolean("is_unidentified_gem")) {
            return super.use(level, player, hand);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(heldStack, true);
        }

        if (!UnidentifiedItemHelper.hasRoomForIdentify(player, heldStack)) {
            player.sendSystemMessage(UnidentifiedItemHelper.fullMessage());
            return InteractionResultHolder.fail(heldStack);
        }

        Purity purity = GemItem.getPurity(heldStack);
        RandomSource random = player.getRandom();

        List<Gem> validGems = GemRegistry.INSTANCE.getValues().stream()
                .filter(g -> purity.isAtLeast(g.getMinPurity()))
                .toList();
        if (validGems.isEmpty()) {
            return InteractionResultHolder.fail(heldStack);
        }

        Gem selectedGem = validGems.get(random.nextInt(validGems.size()));
        ItemStack identifiedGem = selectedGem.toStack(purity);

        UnidentifiedItemHelper.deliver(
                player, hand, heldStack, identifiedGem,
                12, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f, "鉴定出宝石: ");

        return InteractionResultHolder.consume(heldStack);
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        CompoundTag tag = examplemod$readTag(stack);
        if (!tag.getBoolean("is_unidentified_gem")) return;

        Purity purity = GemItem.getPurity(stack);
        cir.setReturnValue(Component.literal("未鉴定的")
                .append(purity.toComponent())
                .append(Component.literal("宝石")));
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext ctx,
                                             List<Component> lines, TooltipFlag flag,
                                             CallbackInfo ci) {
        CompoundTag tag = examplemod$readTag(stack);
        if (!tag.getBoolean("is_unidentified_gem")) return;

        lines.add(Component.literal("右键鉴定"));
        ci.cancel();
    }
}