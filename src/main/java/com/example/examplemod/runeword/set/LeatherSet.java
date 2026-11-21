package com.example.examplemod.runeword.set;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * 皮革套装：拓荒者皮甲
 * 1件：无效果
 * 2件：各加1条词缀
 * 3件：各加1条词缀
 * 4件：唯一1条全套核心词缀
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class LeatherSet extends AbstractArmorSet {

    public static final LeatherSet INSTANCE = new LeatherSet();

    private LeatherSet() {
        super("leather", "拓荒者皮甲");

        helmet(Items.LEATHER_HELMET,
                Affix.attr(Attributes.BLOCK_INTERACTION_RANGE, 1.0, Operation.ADD_VALUE, "+1.0 方块触及距离"),
                Affix.effect(MobEffects.NIGHT_VISION, 0, "常驻效果：夜视")
        );

        chestplate(Items.LEATHER_CHESTPLATE,
                Affix.attr("irons_spellbooks:spell_resist", 0.20, Operation.ADD_VALUE, "+20% 法术抗性"),
                Affix.effect("farmersdelight:nourishment", 0, "常驻效果：滋养")
        );

        leggings(Items.LEATHER_LEGGINGS,
                Affix.attr(Attributes.BLOCK_BREAK_SPEED, 0.20, Operation.ADD_MULTIPLIED_BASE, "+20% 挖掘速度"),
                Affix.attr(NeoForgeMod.SWIM_SPEED, 0.25, Operation.ADD_MULTIPLIED_BASE, "+25% 游泳速度")
        );

        boots(Items.LEATHER_BOOTS,
                Affix.attr(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE, "+15% 移动速度"),
                Affix.attr(Attributes.STEP_HEIGHT, 0.4, Operation.ADD_VALUE, "可直接走上 1 格高方块")
        );

        // 唯一 4 件套词缀
        setBonus(Affix.magnet(4.5, true, "自动吸附 4.5 格内掉落物（潜行时暂停吸附）"));
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> register(INSTANCE));
    }
}