package com.example.examplemod.util;

import com.hidoni.transmog.TransmogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class WeaponReplacer {

    /**
     * 1.21+ 原版标准武器附魔 Tag (minecraft:enchantable/weapon)
     */
    public static final TagKey<Item> WEAPON_TAG = ItemTags.WEAPON_ENCHANTABLE;

    /**
     * 判断物品是否属于可附魔武器（支持剑、斧等兜底）
     */
    public static boolean isEnchantableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(WEAPON_TAG) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
    }

    /**
     * 为原武器赋予随机武器外观（仅修改渲染模型，保留原有属性与数据）
     *
     * @param original 怪物主手持有的原武器
     * @param random   随机源
     * @return 是否成功附加了外观
     */
    public static boolean tryApplyWeaponAppearance(ItemStack original, RandomSource random) {
        if (!isEnchantableWeapon(original)) {
            return false;
        }

        // 如果物品已经被幻化过，跳过
        if (TransmogUtils.isItemStackTransmogged(original)) {
            return false;
        }

        // 从可附魔武器 Tag 中随机抽取一个武器作为外观
        Optional<Item> randomWeaponItem = getRandomWeaponFromTag(random);
        if (randomWeaponItem.isEmpty() || randomWeaponItem.get() == Items.AIR) {
            return false;
        }

        Item appearanceItem = randomWeaponItem.get();
        // 如果抽中跟原来一样的外观则无需操作
        if (appearanceItem == original.getItem()) {
            return false;
        }

        // 核心：利用 Transmog 的 API 仅将随机武器的模型和外观覆盖上去
        ItemStack appearanceStack = new ItemStack(appearanceItem);
        TransmogUtils.transmogAppearanceOntoItemStack(appearanceStack, original);
        return true;
    }

    /**
     * 从注册表的 WEAPON_ENCHANTABLE 标签中随机选取一个有效物品
     */
    private static Optional<Item> getRandomWeaponFromTag(RandomSource random) {
        return BuiltInRegistries.ITEM.getTag(WEAPON_TAG)
                .flatMap(tag -> tag.getRandomElement(random))
                .map(Holder::value);
    }
}