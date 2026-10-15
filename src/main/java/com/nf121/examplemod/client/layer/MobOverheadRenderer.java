package com.nf121.examplemod.client.layer;

import com.nf121.examplemod.client.util.LevelRenderEngine;
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
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.util.*;

public final class MobOverheadRenderer {

    private static final double MAX_DIST_SQ = 24.0D * 24.0D;
    private static final float SCALE = 0.022F;
    private static final double HEIGHT_OFFSET = 0.25D;
    private static final float ICON_SIZE = 9.0F; // 物品大小与字体对齐
    private static final float ICON_SPACING = 10.0F;

    private MobOverheadRenderer() {}

    public static void renderOverhead(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob entity)||event.getEntity()instanceof Animal) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !entity.isAlive() || entity.isInvisibleTo(mc.player)) return;

        float pTicks = event.getPartialTick();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 entityPos = entity instanceof EnderDragon d ? d.head.getPosition(pTicks) : entity.getPosition(pTicks);

        if (camPos.distanceToSqr(entityPos) > MAX_DIST_SQ) return;

        // 视线快速检测
        Vec3 anchor = getAnchor(entity, pTicks);
        Vec3 targetPos = entity.getPosition(pTicks).add(anchor.x, anchor.y + HEIGHT_OFFSET, anchor.z);
        if (entity.level().clip(new ClipContext(camPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.MISS) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(anchor.x, anchor.y + HEIGHT_OFFSET, anchor.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(SCALE, -SCALE, SCALE);

            if (entity instanceof AbstractVillager villager) {
                // 1. 村民走专用的纯交易卡片（无血条、无血量）
                drawVillagerCard(pose, event.getMultiBufferSource(), mc.font, villager);
            } else {
                // 2. 普通/敌对怪物走血条与词缀面板
                drawMobHealthPanel(pose, event.getMultiBufferSource(), mc.font, entity);
            }
        } finally {
            pose.popPose();
        }
    }

    // =========================================================================
    // A. 村民专属纯交易看板 (无血条，纯图文卡片)
    // =========================================================================
    private static void drawVillagerCard(PoseStack ps, MultiBufferSource buf, Font font, AbstractVillager villager) {
        MerchantOffers offers = VillagerTradeClientCache.get(villager.getId());
        List<ItemStack> buyItems = offers != null ? extractUniqueItems(offers, true) : Collections.emptyList();
        List<ItemStack> sellItems = offers != null ? extractUniqueItems(offers, false) : Collections.emptyList();

        Component title = villager.getDisplayName();
        int titleW = font.width(title);
        int buyW = font.width("买: ") + (int) (buyItems.size() * ICON_SPACING);
        int sellW = font.width("卖: ") + (int) (sellItems.size() * ICON_SPACING);

        // 自适应计算卡片宽度
        int cardWidth = Math.max(titleW, Math.max(buyW, sellW)) + 12;
        float left = -cardWidth * 0.5F;
        float right = cardWidth * 0.5F;

        // 行坐标计算
        float titleY = -31.0F;
        float buyY = titleY + font.lineHeight + 2.0F;   // -20
        float sellY = buyY + ICON_SIZE + 2.0F;          // -9
        float cardBottom = sellY + ICON_SIZE + 2.0F;    // +2
        float cardTop = titleY - 3.0F;

        Matrix4f p = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(LevelRenderEngine.Pipelines.unlitColor(false, true, true));

        // 仅绘制整洁圆润的半透底板与边框（无血条槽）
        LevelRenderEngine.VertexWriter.quadColor(vc, p, left - 4, cardTop, -0.02F, right + 4, cardTop, -0.02F, right + 4, cardBottom, -0.02F, left - 4, cardBottom, -0.02F, 0.05F, 0.07F, 0.09F, 0.55F);
        LevelRenderEngine.VertexWriter.quadColor(vc, p, left - 3, cardTop + 1, -0.01F, right + 3, cardTop + 1, -0.01F, right + 3, cardBottom - 1, -0.01F, left - 3, cardBottom - 1, -0.01F, 0.12F, 0.16F, 0.20F, 0.7F);

        // 居中绘制村民名字/职业
        font.drawInBatch(title, -titleW * 0.5F, titleY, 0xFFE2E5E9, false, p, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        // 绘制买/卖行与物品图标
        renderTradeLine(ps, buf, font, "§e买: ", buyItems, left + 2, buyY, villager);
        renderTradeLine(ps, buf, font, "§a卖: ", sellItems, left + 2, sellY, villager);
    }

    private static void renderTradeLine(PoseStack ps, MultiBufferSource buf, Font font,
                                        String label, List<ItemStack> items,
                                        float startX, float startY, Mob entity) {
        font.drawInBatch(label, startX, startY + 0.5F, 0xFFFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        if (items.isEmpty()) {
            font.drawInBatch("§7-", startX + font.width(label), startY + 0.5F, 0xFF888888, false, ps.last().pose(), buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            return;
        }

        float iconX = startX + font.width(label);
        for (ItemStack stack : items) {
            ps.pushPose();
            ps.translate(iconX + ICON_SIZE * 0.5F, startY + ICON_SIZE * 0.5F, 0.05F);
            ps.scale(ICON_SIZE, -ICON_SIZE, ICON_SIZE);

            Minecraft.getInstance().getItemRenderer().renderStatic(
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

    // =========================================================================
    // B. 普通/敌对怪物血条面板 (保持原有血条与 L2 Hostility 词缀逻辑)
    // =========================================================================
    private static void drawMobHealthPanel(PoseStack ps, MultiBufferSource buf, Font font, Mob entity) {
        float barH = 4.0F, border = 1.0F;
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

        float left = -width * 0.5F, right = width * 0.5F;
        float infoY = -font.lineHeight - 3.0F;
        float top = traitLines.isEmpty() ? infoY - 3.0F : infoY - 3.0F - traitLines.size() * (font.lineHeight + 2);

        Matrix4f p = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(LevelRenderEngine.Pipelines.unlitColor(false, true, true));

        // 底板
        LevelRenderEngine.VertexWriter.quadColor(vc, p, left - 4, top, -0.02F, right + 4, top, -0.02F, right + 4, barH + 3, -0.02F, left - 4, barH + 3, -0.02F, 0.05F, 0.07F, 0.09F, 0.45F);
        // 血条外框
        LevelRenderEngine.VertexWriter.quadColor(vc, p, left - border, -border, 0.0F, right + border, -border, 0.0F, right + border, barH + border, 0.0F, left - border, barH + border, 0.0F, 0.09F, 0.11F, 0.13F, 0.75F);
        // 血条槽底色
        LevelRenderEngine.VertexWriter.quadColor(vc, p, left, 0, 0.01F, right, 0, 0.01F, right, barH, 0.01F, left, barH, 0.01F, 0.20F, 0.22F, 0.25F, 0.85F);

        // 血条填充
        if (hpRatio > 0.0F) {
            float r = isHostile ? 0.8F : 0.4F + 0.14F * hpRatio;
            float g = isHostile ? 0.3F + 0.25F * hpRatio : 0.6F + 0.13F * hpRatio;
            float b = isHostile ? 0.35F : 0.5F + 0.1F * hpRatio;
            LevelRenderEngine.VertexWriter.quadColor(vc, p, left, 0, 0.02F, left + width * hpRatio, 0, 0.02F, left + width * hpRatio, barH, 0.02F, left, barH, 0.02F, r, g, b, 1.0F);
        }

        // 文字
        if (titleW > 0) font.drawInBatch(title, left, infoY, 0xFFE2E5E9, false, p, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        font.drawInBatch(hpText, right - hpW, infoY, 0xFFE2E5E9, false, p, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        for (int i = 0; i < traitLines.size(); i++) {
            FormattedCharSequence line = traitLines.get(i);
            font.drawInBatch(line, -font.width(line) * 0.5F, top + 3.0F + i * (font.lineHeight + 2), 0xFFBEC5CE, false, p, buf, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
    }

// =========================================================================
// 修复后的交易物品提取逻辑
// =========================================================================

    /**
     * 提取村民交易列表中的独特物品
     * @param offers 村民交易列表
     * @param isBuy true: 提取村民收购的物品 (玩家卖材料赚绿宝石)
     *              false: 提取村民出售的物品 (玩家花绿宝石买商品)
     */
    private static List<ItemStack> extractUniqueItems(MerchantOffers offers, boolean isBuy) {
        Set<Object> seen = new HashSet<>();
        List<ItemStack> result = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            ItemStack resultStack = offer.getResult();
            boolean isVillagerBuying = resultStack.is(Items.EMERALD); // 产出绿宝石 = 村民收购

            if (isBuy) {
                // 只有当产物是绿宝石时，它的输入原料才是村民“买”的东西
                if (isVillagerBuying) {
                    addUnique(offer.getCostA(), seen, result);
                    if (!offer.getCostB().isEmpty()) {
                        addUnique(offer.getCostB(), seen, result);
                    }
                }
            } else {
                // 只要产物不是绿宝石，说明这是村民“卖”给玩家的商品
                if (!isVillagerBuying) {
                    addUnique(resultStack, seen, result);
                }
            }
        }
        return result;
    }

    private static void addUnique(ItemStack stack, Set<Object> seen, List<ItemStack> out) {
        if (stack.isEmpty() || stack.is(Items.EMERALD)) return;

        // 针对附魔书、药水做精准去重（NeoForge 1.20.5+ 使用 Components）
        Object key;
        if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)) {
            key = stack.getComponentsPatch(); // 区分不同附魔和药水属性
        } else {
            key = stack.getItem();
        }

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
            return dragon.head.getPosition(pTicks).subtract(dragon.getPosition(pTicks)).add(0, dragon.head.getBbHeight() + 0.5D, 0);
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