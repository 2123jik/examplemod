package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(value = Scroll.class, remap = false)
public abstract class MixinScroll extends Item {

    public MixinScroll(Properties properties) {
        super(properties);
    }

    // =========================================================================
    // 【底层安全盾】：杜绝空串崩溃，保证任何脏数据都不闪退
    // =========================================================================
    @Unique
    private static SpellRarity yourMod$safeGetRarity(CompoundTag tag) {
        if (tag.contains("spell_rarity")) {
            String str = tag.getString("spell_rarity").trim();
            if (!str.isEmpty()) {
                try {
                    return SpellRarity.valueOf(str.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return SpellRarity.COMMON; // 遇空值/非法值安全兜底为普通
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void handleScrollUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack heldStack = player.getItemInHand(hand);
        CustomData customData = heldStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (tag.getBoolean("is_unidentified")) {
            if (level.isClientSide) {
                cir.setReturnValue(InteractionResultHolder.sidedSuccess(heldStack, true));
                return;
            }

            // 【防误放锁 1】：前置背包空间检测
            int freeSlot = player.getInventory().getFreeSlot();
            if (freeSlot == -1 && heldStack.getCount() > 1) {
                player.sendSystemMessage(Component.literal("背包已满，无法鉴定！请腾出空间后再试。").withStyle(ChatFormatting.RED));
                cir.setReturnValue(InteractionResultHolder.fail(heldStack));
                return;
            }

            // 1. 安全读取官方品质
            SpellRarity targetRarity = yourMod$safeGetRarity(tag);
            String schoolFilter = tag.contains("school_filter") ? tag.getString("school_filter") : null;

            // 2. 根据官方稀有度生成真实卷轴
            ItemStack identifiedScroll = yourMod$createIdentifiedScroll(targetRarity, schoolFilter, player.getRandom());
            if (identifiedScroll.isEmpty()) {
                return;
            }

            // 3. 扣除 1 个未鉴定卷轴
            if (!player.isCreative()) {
                heldStack.shrink(1);
            }

            // 【防误放锁 2】：手持位隔离放入 + 双端网络同步
            boolean inserted = false;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                if (hand == InteractionHand.MAIN_HAND && i == player.getInventory().selected) {
                    continue; // 跳过当前交互手
                }
                ItemStack slotStack = player.getInventory().items.get(i);
                if (ItemStack.isSameItemSameComponents(slotStack, identifiedScroll) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                    slotStack.grow(1);
                    inserted = true;
                    break;
                } else if (slotStack.isEmpty()) {
                    player.getInventory().items.set(i, identifiedScroll);
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                if (heldStack.isEmpty()) {
                    player.setItemInHand(hand, identifiedScroll);
                } else {
                    player.drop(identifiedScroll, false);
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }

            // 【防误放锁 3】：12 tick 底层冷却
            player.getCooldowns().addCooldown(this, 12);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
            player.sendSystemMessage(Component.literal("鉴定出卷轴: ").withStyle(ChatFormatting.GOLD)
                    .append(identifiedScroll.getHoverName()));

            cir.setReturnValue(InteractionResultHolder.consume(heldStack));
        }
    }

    @Unique
    private ItemStack yourMod$createIdentifiedScroll(SpellRarity targetRarity, String schoolFilter, RandomSource random) {
        List<AbstractSpell> validSpells = SpellRegistry.REGISTRY.stream()
                .filter(s -> s.isEnabled() && s != SpellRegistry.none() && s.allowLooting())
                .filter(s -> targetRarity.getValue() >= s.getMinRarity() && targetRarity.getValue() <= s.getMaxRarity())
                .filter(s -> {
                    if (schoolFilter == null) return true;
                    return s.getSchoolType().getId().toString().equals(schoolFilter);
                })
                .toList();

        if (validSpells.isEmpty()) {
            return ItemStack.EMPTY;
        }

        AbstractSpell chosenSpell = validSpells.get(random.nextInt(validSpells.size()));

        List<Integer> matchingLevels = new ArrayList<>();
        for (int lvl = chosenSpell.getMinLevel(); lvl <= chosenSpell.getMaxLevel(); lvl++) {
            if (chosenSpell.getRarity(lvl) == targetRarity) {
                matchingLevels.add(lvl);
            }
        }

        int chosenLevel = matchingLevels.isEmpty()
                ? chosenSpell.getMinLevelForRarity(targetRarity)
                : matchingLevels.get(random.nextInt(matchingLevels.size()));

        ItemStack result = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(chosenSpell, chosenLevel, result);
        return result;
    }

    // =========================================================================
    // 3. 拦截未鉴定名字显示
    // =========================================================================
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.copyTag().getBoolean("is_unidentified")) {
            SpellRarity rarity = yourMod$safeGetRarity(data.copyTag());
            cir.setReturnValue(Component.literal("未鉴定的")
                    .append(rarity.getDisplayName())
                    .append(Component.literal("法术卷轴")));
        }
    }

    // =========================================================================
    // 4. 拦截 Tooltip：保持你喜欢的自然语序（[品质]品质，[学派]学派限定）
    // =========================================================================
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.copyTag().getBoolean("is_unidentified")) {
            CompoundTag tag = data.copyTag();

            // 你喜欢的语序：[火焰]学派限定
            if (tag.contains("school_filter")) {
                SchoolType school = SchoolRegistry.getSchool(ResourceLocation.parse(tag.getString("school_filter")));
                if (school != null) {
                    lines.add(Component.literal("")
                            .append(school.getDisplayName())
                            .append(Component.literal("学派限定")));
                }
            }

            lines.add(Component.literal("右键解读法术"));
            ci.cancel();
        }
    }
}