package com.example.examplemod.client.layer;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.util.LevelRenderEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.data.LHTagGen;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.util.*;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class MobOverheadRenderer {

    private static final double MAX_DIST_SQ = 24.0D * 24.0D;
    private static final float SCALE = 0.022F;
    private static final double HEIGHT_OFFSET = 0.25D;
    private static final float ICON_SIZE = 9.0F;
    private static final float ICON_SPACING = 10.0F;

    private MobOverheadRenderer() {}

    @SubscribeEvent
    public static void renderOverhead(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob entity)
                || entity instanceof Animal
                || entity instanceof WaterAnimal) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !entity.isAlive() || entity.isInvisibleTo(mc.player)) return;

        float pTicks = event.getPartialTick();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 entityPos = entity instanceof EnderDragon d ? d.head.getPosition(pTicks) : entity.getPosition(pTicks);
        if (camPos.distanceToSqr(entityPos) > MAX_DIST_SQ) return;

        Vec3 anchor = getAnchor(entity, pTicks);
        Vec3 targetPos = entity.getPosition(pTicks).add(anchor.x, anchor.y + HEIGHT_OFFSET, anchor.z);
        if (entity.level().clip(new ClipContext(
                camPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
        )).getType() != HitResult.Type.MISS) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(anchor.x, anchor.y + HEIGHT_OFFSET, anchor.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(SCALE, -SCALE, SCALE);

            if (entity instanceof AbstractVillager villager) {
                drawVillagerCard(pose, event.getMultiBufferSource(), mc.font, villager);
            } else {
                drawMobHealthPanel(pose, event.getMultiBufferSource(), mc.font, entity);
            }
        } finally {
            pose.popPose();
        }
    }

    private static void drawVillagerCard(PoseStack ps, MultiBufferSource buf, Font font, AbstractVillager villager) {
        MerchantOffers offers = VillagerTradeClientCache.get(villager.getId());
        List<ItemStack> buyItems = offers != null ? extractUniqueItems(offers, true) : Collections.emptyList();
        List<ItemStack> sellItems = offers != null ? extractUniqueItems(offers, false) : Collections.emptyList();

        Component title = villager.getDisplayName();
        int titleW = font.width(title);
        int buyW = font.width("买: ") + (int) (buyItems.size() * ICON_SPACING);
        int sellW = font.width("卖: ") + (int) (sellItems.size() * ICON_SPACING);

        int cardWidth = Math.max(titleW, Math.max(buyW, sellW)) + 12;
        float left = -cardWidth * 0.5F;

        float titleY = -31.0F;
        float buyY = titleY + font.lineHeight + 2.0F;
        float sellY = buyY + ICON_SIZE + 2.0F;

        Matrix4f p = ps.last().pose();
        font.drawInBatch(title, -titleW * 0.5F, titleY, 0xFFE2E5E9, false, p, buf,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        renderTradeLine(ps, buf, font, "§e买: ", buyItems, left + 2, buyY, villager);
        renderTradeLine(ps, buf, font, "§a卖: ", sellItems, left + 2, sellY, villager);
    }

    private static void renderTradeLine(PoseStack ps, MultiBufferSource buf, Font font,
                                        String label, List<ItemStack> items,
                                        float startX, float startY, Mob entity) {
        Matrix4f pose = ps.last().pose();
        font.drawInBatch(label, startX, startY + 0.5F, 0xFFFFFFFF, false, pose, buf,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        if (items.isEmpty()) {
            font.drawInBatch("§7-", startX + font.width(label), startY + 0.5F, 0xFF888888, false, pose, buf,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            return;
        }

        float iconX = startX + font.width(label);
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (ItemStack stack : items) {
            ps.pushPose();
            ps.translate(iconX + ICON_SIZE * 0.5F, startY + ICON_SIZE * 0.5F, 0.05F);
            ps.scale(ICON_SIZE, -ICON_SIZE, ICON_SIZE);
            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.GUI,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    ps,
                    buf,
                    entity.level(),
                    entity.getId()
            );
            ps.popPose();
            iconX += ICON_SPACING;
        }
    }

    private static void drawMobHealthPanel(PoseStack ps, MultiBufferSource buf, Font font, Mob entity) {
        float barH = 4.0F;
        float border = 1.0F;
        float hpRatio = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        boolean isHostile = entity.getTarget() != null && entity.getTarget().isAlive();

        List<FormattedCharSequence> traitLines = collectTraitLines(entity, font);
        Component title = getMobTitle(entity);
        String hpText = (int) entity.getHealth() + " / " + (int) entity.getMaxHealth();

        int titleW = font.width(title);
        int hpW = font.width(hpText);
        int width = Math.max(100, titleW + hpW + 16);
        for (FormattedCharSequence line : traitLines) {
            width = Math.max(width, font.width(line) + 10);
        }
        width = Math.min(width, 180);

        float left = -width * 0.5F;
        float right = width * 0.5F;
        float infoY = -font.lineHeight - 3.0F;
        float top = traitLines.isEmpty()
                ? infoY - 3.0F
                : infoY - 3.0F - traitLines.size() * (font.lineHeight + 2);

        Matrix4f p = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(LevelRenderEngine.Pipelines.unlitColor(false, true, true));

        LevelRenderEngine.VertexWriter.quadColor(vc, p,
                left - border, -border, 0.0F, right + border, -border, 0.0F,
                right + border, barH + border, 0.0F, left - border, barH + border, 0.0F,
                0.09F, 0.11F, 0.13F, 0.75F);

        LevelRenderEngine.VertexWriter.quadColor(vc, p,
                left, 0, 0.01F, right, 0, 0.01F, right, barH, 0.01F, left, barH, 0.01F,
                0.20F, 0.22F, 0.25F, 0.85F);

        if (hpRatio > 0.0F) {
            float r = isHostile ? 0.8F : 0.4F + 0.14F * hpRatio;
            float g = isHostile ? 0.3F + 0.25F * hpRatio : 0.6F + 0.13F * hpRatio;
            float b = isHostile ? 0.35F : 0.5F + 0.1F * hpRatio;
            LevelRenderEngine.VertexWriter.quadColor(vc, p,
                    left, 0, 0.02F, left + width * hpRatio, 0, 0.02F,
                    left + width * hpRatio, barH, 0.02F, left, barH, 0.02F,
                    r, g, b, 1.0F);
        }

        if (titleW > 0) {
            font.drawInBatch(title, left, infoY, 0xFFE2E5E9, false, p, buf,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
        font.drawInBatch(hpText, right - hpW, infoY, 0xFFE2E5E9, false, p, buf,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        for (int i = 0; i < traitLines.size(); i++) {
            FormattedCharSequence line = traitLines.get(i);
            font.drawInBatch(line, -font.width(line) * 0.5F, top + 3.0F + i * (font.lineHeight + 2),
                    0xFFBEC5CE, false, p, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
    }

    private static List<ItemStack> extractUniqueItems(MerchantOffers offers, boolean isBuy) {
        Set<Object> seen = new HashSet<>();
        List<ItemStack> result = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            ItemStack resultStack = offer.getResult();
            boolean isVillagerBuying = resultStack.is(Items.EMERALD);

            if (isBuy) {
                if (isVillagerBuying) {
                    addUnique(offer.getCostA(), seen, result);
                    if (!offer.getCostB().isEmpty()) {
                        addUnique(offer.getCostB(), seen, result);
                    }
                }
            } else if (!isVillagerBuying) {
                addUnique(resultStack, seen, result);
            }
        }
        return result;
    }

    private static void addUnique(ItemStack stack, Set<Object> seen, List<ItemStack> out) {
        if (stack.isEmpty() || stack.is(Items.EMERALD)) return;

        Object key = (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                ? stack.getComponentsPatch()
                : stack.getItem();

        if (seen.add(key)) {
            out.add(stack);
        }
    }

    private static Component getMobTitle(Mob entity) {
        MobTraitCap cap = getCapability(entity);
        if (cap != null && !entity.getType().is(LHTagGen.HIDE_LEVEL)) {
            var list = cap.getTitle(true, false);
            var comp = Component.empty();
            list.forEach(c -> comp.append(c).append(" "));
            return comp;
        }
        return entity.getDisplayName();
    }

    private static List<FormattedCharSequence> collectTraitLines(Mob entity, Font font) {
        MobTraitCap cap = getCapability(entity);
        if (cap != null && !entity.getType().is(LHTagGen.HIDE_TRAITS)) {
            var titles = cap.getTitle(false, true);
            if (!titles.isEmpty()) {
                var comp = Component.empty();
                titles.forEach(comp::append);
                return font.split(comp, 140);
            }
        }
        return Collections.emptyList();
    }

    private static Vec3 getAnchor(Mob entity, float pTicks) {
        if (entity instanceof EnderDragon dragon) {
            return dragon.head.getPosition(pTicks).subtract(dragon.getPosition(pTicks))
                    .add(0, dragon.head.getBbHeight() + 0.5D, 0);
        }
        Vec3 anchor = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(pTicks));
        return anchor != null ? anchor : new Vec3(0, entity.getBoundingBox().getYsize(), 0);
    }

    @SuppressWarnings("rawtypes")
    private static MobTraitCap getCapability(Mob entity) {
        Optional<?> opt = ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(entity);
        return (opt.isPresent() && opt.get() instanceof MobTraitCap cap) ? cap : null;
    }
}