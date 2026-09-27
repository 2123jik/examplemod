package com.example.examplemod.mixin.villna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {

    public CompassItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void onUseOnBed(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(BlockTags.BEDS) && state.is(Blocks.RESPAWN_ANCHOR)) {
            
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!level.isClientSide) {
                Player player = context.getPlayer();
                ItemStack handStack = context.getItemInHand();

                LodestoneTracker tracker = new LodestoneTracker(
                        Optional.of(GlobalPos.of(level.dimension(), pos)),
                        false
                );

                if (handStack.getCount() == 1) {
                    handStack.set(DataComponents.LODESTONE_TRACKER, tracker);
                } else {
                    ItemStack newStack = handStack.transmuteCopy(Items.COMPASS, 1);
                    handStack.shrink(1);
                    newStack.set(DataComponents.LODESTONE_TRACKER, tracker);

                    if (!player.getInventory().add(newStack)) {
                        player.drop(newStack, false);
                    }
                }
            }

            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }
}