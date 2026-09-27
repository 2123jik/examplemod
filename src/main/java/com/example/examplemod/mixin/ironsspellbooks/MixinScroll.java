package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.util.UnidentifiedItemHelper;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = Scroll.class, remap = false)
public abstract class MixinScroll extends Item {

    public MixinScroll(Properties properties) { super(properties); }

    // =========================================================================
    // 【缓存】按稀有度缓存的候选法术列表，避免每次右键都 stream 全注册表
    // =========================================================================
    @Unique
    private static final Map<SpellRarity, List<AbstractSpell>> SPELLS_BY_RARITY =
            new ConcurrentHashMap<>();

    @Unique
    private static List<AbstractSpell> yourMod$spellsFor(SpellRarity rarity) {
        return SPELLS_BY_RARITY.computeIfAbsent(rarity, r ->
                SpellRegistry.REGISTRY.stream()
                        .filter(s -> s.isEnabled()
                                && s != SpellRegistry.none()
                                && s.allowLooting()
                                && r.getValue() >= s.getMinRarity()
                                && r.getValue() <= s.getMaxRarity())
                        .toList());
    }

    // =========================================================================
    // 【安全盾】脏数据不闪退
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
        return SpellRarity.COMMON;
    }

    /** 只读视图，不做 NBT 深拷贝。 */
    @Unique
    private static CompoundTag yourMod$readTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.getUnsafe();
    }

    // =========================================================================
    // use：鉴定入口
    // =========================================================================
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void handleScrollUse(Level level, Player player, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack heldStack = player.getItemInHand(hand);
        CompoundTag tag = yourMod$readTag(heldStack);

        if (!tag.getBoolean("is_unidentified")) return;

        if (level.isClientSide) {
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(heldStack, true));
            return;
        }

        // 背包空间前置检测
        if (!UnidentifiedItemHelper.hasRoomForIdentify(player, heldStack)) {
            player.sendSystemMessage(UnidentifiedItemHelper.fullMessage());
            cir.setReturnValue(InteractionResultHolder.fail(heldStack));
            return;
        }

        SpellRarity targetRarity = yourMod$safeGetRarity(tag);
        String schoolFilter = tag.contains("school_filter") ? tag.getString("school_filter") : null;

        ItemStack identifiedScroll = yourMod$createIdentifiedScroll(
                targetRarity, schoolFilter, player.getRandom());
        if (identifiedScroll.isEmpty()) return;

        UnidentifiedItemHelper.deliver(
                player, hand, heldStack, identifiedScroll,
                12, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f, "鉴定出卷轴: ");

        cir.setReturnValue(InteractionResultHolder.consume(heldStack));
    }

    @Unique
    private ItemStack yourMod$createIdentifiedScroll(SpellRarity targetRarity,
                                                     String schoolFilter,
                                                     RandomSource random) {
        List<AbstractSpell> pool = yourMod$spellsFor(targetRarity).stream()
                .filter(s -> schoolFilter == null
                        || s.getSchoolType().getId().toString().equals(schoolFilter))
                .toList();

        if (pool.isEmpty()) return ItemStack.EMPTY;

        AbstractSpell chosen = pool.get(random.nextInt(pool.size()));

        // 收集所有匹配该稀有度的等级
        List<Integer> matchingLevels = new ArrayList<>();
        for (int lvl = chosen.getMinLevel(); lvl <= chosen.getMaxLevel(); lvl++) {
            if (chosen.getRarity(lvl) == targetRarity) matchingLevels.add(lvl);
        }
        int level = matchingLevels.isEmpty()
                ? chosen.getMinLevelForRarity(targetRarity)
                : matchingLevels.get(random.nextInt(matchingLevels.size()));

        ItemStack result = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(chosen, level, result);
        return result;
    }

    // =========================================================================
    // getName：每帧都调，用 getUnsafe()
    // =========================================================================
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        CompoundTag tag = yourMod$readTag(stack);
        if (!tag.getBoolean("is_unidentified")) return;

        SpellRarity rarity = yourMod$safeGetRarity(tag);   // 复用同一份 tag，不再拷贝
        cir.setReturnValue(Component.literal("未鉴定的")
                .append(rarity.getDisplayName())
                .append(Component.literal("法术卷轴")));
    }

    // =========================================================================
    // appendHoverText：同样每帧都调
    // =========================================================================
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext ctx,
                                             List<Component> lines, TooltipFlag flag,
                                             CallbackInfo ci) {
        CompoundTag tag = yourMod$readTag(stack);
        if (!tag.getBoolean("is_unidentified")) return;

        if (tag.contains("school_filter")) {
            SchoolType school = SchoolRegistry.getSchool(
                    ResourceLocation.parse(tag.getString("school_filter")));
            if (school != null) {
                lines.add(Component.empty()
                        .append(school.getDisplayName())
                        .append(Component.literal("学派限定")));
            }
        }

        lines.add(Component.literal("右键解读法术"));
        ci.cancel();
    }
}