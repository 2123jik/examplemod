package com.example.examplemod.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class TimedAttributeModifier {
    private final Holder<Attribute> attribute;
    private final ResourceLocation id;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final int totalDuration;
    private int remainingTicks;

    public TimedAttributeModifier(
            Holder<Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation,
            int durationTicks
    ) {
        this.attribute = attribute;
        this.id = id;
        this.amount = amount;
        this.operation = operation;
        this.totalDuration = durationTicks;
        this.remainingTicks = durationTicks;
    }

    public Holder<Attribute> getAttribute() { return attribute; }
    public ResourceLocation getId() { return id; }
    public double getAmount() { return amount; }
    public AttributeModifier.Operation getOperation() { return operation; }
    public int getRemainingTicks() { return remainingTicks; }

    /** 转换为原版 1.21 的 AttributeModifier */
    public AttributeModifier toVanillaModifier() {
        return new AttributeModifier(id, amount, operation);
    }

    /** 刷新持续时间 */
    public void refreshDuration(int durationTicks) {
        this.remainingTicks = Math.max(this.remainingTicks, durationTicks);
    }

    /** 倒计时，返回是否已过期 */
    public boolean tick() {
        return --this.remainingTicks <= 0;
    }
}