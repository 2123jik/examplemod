package com.example.examplemod.compat.apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Set;

public class SpellLevelAffix extends Affix {

    public static final Codec<SpellLevelAffix> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            affixDef(),
            ResourceLocation.CODEC.fieldOf("spell").forGetter(a -> a.spellId),
            LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values)
    ).apply(inst, SpellLevelAffix::new));

    private final ResourceLocation spellId;
    private final Set<LootCategory> categories;
    private final Map<LootRarity, StepFunction> values;

    public SpellLevelAffix(AffixDefinition definition, ResourceLocation spellId,
                           Set<LootCategory> categories, Map<LootRarity, StepFunction> values) {
        super(definition);
        this.spellId = spellId;
        this.categories = categories;
        this.values = values;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return !cat.isNone() && this.categories.contains(cat) && this.values.containsKey(rarity);
    }

    public int getBonusLevels(AffixInstance inst, AbstractSpell spell) {
        if (!spell.getSpellResource().equals(this.spellId)) {
            return 0;
        }
        StepFunction func = this.values.get(inst.rarity().get());
        return func == null ? 0 : func.getInt(inst.level());
    }

    private Component getSpellDisplayName() {
        AbstractSpell spell = SpellRegistry.getSpell(this.spellId);
        // spell.getDisplayName(null) 内部就是 Component.translatable("spell.<ns>.<name>")
        return spell != null ? spell.getDisplayName(null) : Component.literal(this.spellId.toString());
    }

    @Override
    public Component getName(boolean prefix) {
        return Component.translatable(
                prefix ? "affix.examplemod.spell_level.prefix" : "affix.examplemod.spell_level.suffix",
                this.getSpellDisplayName()
        );
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        StepFunction func = this.values.get(inst.rarity().get());
        if (func == null) return Component.empty();
        int bonus = func.getInt(inst.level());
        return Component.translatable("affix.examplemod.spell_level.desc", bonus, this.getSpellDisplayName());
    }
}