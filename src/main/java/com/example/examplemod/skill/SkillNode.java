package com.example.examplemod.skill;

import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.world.item.component.ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;

public class SkillNode {
    private String id;
    private int maxRank;
    private final List<String> parents = new ArrayList<>();
    private final List<AttributeBonus> bonuses = new ArrayList<>();

    private float x;
    private float y;

    @Nullable
    private ResourceLocation icon;

    @Nullable
    private AbstractSpell spell;

    @Nullable
    private Component customTitle;

    public record AttributeBonus(
            ResourceLocation attributeId,
            double amountPerRank,
            AttributeModifier.Operation operation,
            ResourceLocation modifierId
    ) {
        public MutableComponent formatBonus(int rank) {
            double total = amountPerRank * Math.max(1, rank);
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attributeId);
            Component attrName = attr != null ? Component.translatable(attr.getDescriptionId()) : Component.literal(attributeId.getPath());

            if (operation == AttributeModifier.Operation.ADD_VALUE) {
                String valStr = ATTRIBUTE_MODIFIER_FORMAT.format(total);
                return Component.translatable("attribute.modifier.plus.0", valStr, attrName);
            } else {
                String valStr = ATTRIBUTE_MODIFIER_FORMAT.format(total * 100.0);
                return Component.translatable("attribute.modifier.plus.1", valStr, attrName);
            }
        }

        // 补全此方法
        public String attributeName() {
            return attributeId.toString();
        }
    }

    public SkillNode(String id, int maxRank, String... parents) {
        this.id = id;
        this.maxRank = maxRank;
        if (parents != null) {
            this.parents.addAll(Arrays.asList(parents));
        }
    }

    public SkillNode addBonus(String attributeRl, double amountPerRank, AttributeModifier.Operation operation) {
        ResourceLocation attrId = ResourceLocation.parse(attributeRl);
        int index = bonuses.size();
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                ExampleMod.MODID,
                "skill_" + this.id.replace(':', '_') + "_" + attrId.getPath() + "_" + index
        );
        bonuses.add(new AttributeBonus(attrId, amountPerRank, operation, modifierId));
        return this;
    }

    public SkillNode setIcon(@Nullable ResourceLocation icon) {
        this.icon = icon;
        return this;
    }

    public SkillNode setSpell(@Nullable AbstractSpell spell) {
        this.spell = spell;
        if (spell != null) {
            this.icon = spell.getSpellIconResource();
        }
        return this;
    }

    public SkillNode setTitle(@Nullable Component title) {
        this.customTitle = title;
        return this;
    }

    @Nullable
    public Component getLocalizedTitle() {
        if (this.spell != null) {
            return this.spell.getDisplayName(null);
        }
        if (this.customTitle != null) {
            return this.customTitle;
        }
        return null;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setId(String id) { this.id = id; }
    public void setMaxRank(int maxRank) { this.maxRank = Math.max(1, maxRank); }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }

    public String getId() { return id; }
    public int getMaxRank() { return maxRank; }
    public List<String> getParents() { return parents; }
    public List<AttributeBonus> getBonuses() { return bonuses; }
    public float getX() { return x; }
    public float getY() { return y; }
    @Nullable
    public ResourceLocation getIcon() { return icon; }
    @Nullable
    public AbstractSpell getSpell() { return spell; }
}