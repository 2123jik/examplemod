package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.GemView;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MultiAttrBonus.class, remap = false)
public abstract class MultiAttrBonusTooltipMixin {

    @Shadow
    @Final
    protected List<MultiAttrBonus.ModifierInst> modifiers;

    @Shadow
    @Final
    protected String desc;

    @Inject(
            method = "getSocketBonusTooltip",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void examplemod$correctTooltipValues(
            GemView view,
            AttributeTooltipContext context,
            CallbackInfoReturnable<Component> cir
    ) {
        int count = this.modifiers.size();
        Object[] values = new Object[count * 2];

        for (int i = 0; i < count; i++) {
            var definition = this.modifiers.get(i);

            // 仅用于生成这行文本，不提交给属性系统。
            var modifier = definition.build(
                    ResourceLocation.fromNamespaceAndPath(
                            "examplemod",
                            "gem_tooltip/" + i
                    ),
                    view.purity()
            );

            var attribute = definition.attr().value();

            values[i] = attribute.toComponent(
                    modifier,
                    context.flag()
            );

            values[count + i] = attribute.toValueComponent(
                    definition.op(),
                    modifier.amount(),
                    context.flag()
            );
        }

        cir.setReturnValue(
                Component.translatable(this.desc, values)
                        .withStyle(ChatFormatting.YELLOW)
        );
    }
}
