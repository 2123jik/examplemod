package com.example.examplemod.compat.l2hostility.traits;

import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.item.Item;

public class SpellTraitSymbol extends TraitSymbol {

    public SpellTraitSymbol(Item.Properties props) {
        super(props);
    }

    /**
     * 让物品名直接用 trait 的名字（也就是 AutoSpellTrait 重写后的 spell.getComponentId()）。
     * 这个方法只在第一次需要 translation key 时调用（tooltip / 创造栏渲染），
     * 那时注册已完成，get() 必然非 null。
     */
    @Override
    protected String getOrCreateDescriptionId() {
        MobTrait trait = this.get();
        if (trait != null) {
            return trait.getDescriptionId();
        }
        return super.getOrCreateDescriptionId();
    }
}