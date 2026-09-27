package com.example.examplemod.compat.l2hostility.traits;

import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.world.item.Item;

public class SpellTraitSymbol extends TraitSymbol {

    public SpellTraitSymbol(Item.Properties props) {
        super(props);
    }

    @Override
    protected String getOrCreateDescriptionId() {
        MobTrait trait = this.get();
        if (trait != null) {
            return trait.getDescriptionId();
        }
        return super.getOrCreateDescriptionId();
    }
}