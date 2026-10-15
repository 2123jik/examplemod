package com.nf121.examplemod.client.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class PauseMenuOverlay {

    private static final DateTimeFormatter REAL_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        BlockPos pos = mc.player.blockPosition();

        // 避免遮挡居中主按钮 (中央留白保护)
        int marginX = 8;
        int marginY = 8;
        int safeRightEdge = (screen.width / 2) - 102 - 14;
        int maxAllowedWidth = safeRightEdge - marginX;

        if (maxAllowedWidth < 100) return;

        // 收集人类直观标签
        List<Component> tags = collectHumanFriendlyTags(mc, pos);

        // 像素级自动排版折行
        List<Component> renderedLines = flowPackTags(tags, font, maxAllowedWidth, " §7| ");

        int panelWidth = 0;
        for (Component line : renderedLines) {
            panelWidth = Math.max(panelWidth, font.width(line));
        }

        int padding = 6;
        int lineHeight = font.lineHeight + 3;
        int panelHeight = renderedLines.size() * lineHeight;

        // 优雅磨砂质感背景
        graphics.fill(marginX - padding, marginY - padding,
                marginX + panelWidth + padding, marginY + panelHeight + padding - 2,
                0xCC18181B); // 深色半透明卡片
        graphics.fill(marginX - padding, marginY - padding,
                marginX + panelWidth + padding, marginY - padding + 1,
                0x30FFFFFF); // 顶部高光微边框

        // 绘制文字
        for (int i = 0; i < renderedLines.size(); i++) {
            graphics.drawString(font, renderedLines.get(i), marginX, marginY + (i * lineHeight), 0xFFFFFF, true);
        }
    }

    private static List<Component> collectHumanFriendlyTags(Minecraft mc, BlockPos pos) {
        List<Component> tags = new ArrayList<>();

        // ==========================================
        // 1. 运行表现 (用白话描述电脑与世界卡不卡)
        // ==========================================
        int fps = mc.getFps();
        tags.add(Component.literal(formatFpsHuman(fps)));

        // 世界/服务器卡顿评定 (结合 MSPT)
        String worldPace = evaluateWorldPace(mc);
        if (worldPace != null) {
            tags.add(Component.literal(worldPace));
        }

        // 网络延时评价 (单人游戏不显示)
        String pingInfo = evaluateNetworkStatus(mc);
        if (pingInfo != null) {
            tags.add(Component.literal(pingInfo));
        }

        // 内存健康度
        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMem = rt.maxMemory() / 1024 / 1024;
        int memPercent = (int) (usedMem * 100 / maxMem);
        tags.add(Component.literal(formatMemoryHuman(memPercent)));

        // ==========================================
        // 2. 玩家位置与所处环境
        // ==========================================
        tags.add(Component.literal(String.format("§f坐标 §6%d, %d, %d", pos.getX(), pos.getY(), pos.getZ())));
        tags.add(Component.literal("§7朝向 " + getHumanDirection(mc.player.getDirection())));

        // 生态与遗迹结构
        Holder<Biome> biome = mc.level.getBiome(pos);
        String biomeName = biome.unwrapKey()
                .map(k -> Component.translatable(Util.makeDescriptionId("biome", k.location())).getString())
                .orElse("未知群系");
        tags.add(Component.literal("§a" + biomeName));

        String struct = getStructureName(mc, pos);
        if (struct != null) {
            tags.add(Component.literal("§e遗迹: " + struct));
        }

        // ==========================================
        // 3. 安全感/光照评级 (防刷怪判定)
        // ==========================================
        int blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos);
        if (blockLight == 0) {
            tags.add(Component.literal("§c暗处(极易刷怪)"));
        } else if (blockLight < 7) {
            tags.add(Component.literal("§6微弱照明(注意刷怪)"));
        } else {
            tags.add(Component.literal("§a安全(不刷怪)"));
        }

        // ==========================================
        // 4. 天文昼夜与天气
        // ==========================================
        tags.add(Component.literal(getHumanTimeAndWeather(mc.level)));
        tags.add(Component.literal(getHumanMoon(mc.level.getMoonPhase())));

        // ==========================================
        // 5. 准星在看什么 (直观标出对象与生命)
        // ==========================================
        String lookAt = getLookingAtDescription(mc);
        if (lookAt != null) {
            tags.add(Component.literal(lookAt));
        }

        // 现实时间收尾
        tags.add(Component.literal("§8现实 " + LocalTime.now().format(REAL_TIME_FMT)));

        return tags;
    }

    // ----------------------------------------------------
    // 人性化转换工具方法
    // ----------------------------------------------------

    /** 画面帧率人性化 */
    private static String formatFpsHuman(int fps) {
        if (fps >= 100) return "§a极度丝滑 (" + fps + "帧)";
        if (fps >= 60)  return "§a画面流畅 (" + fps + "帧)";
        if (fps >= 40)  return "§e轻微掉帧 (" + fps + "帧)";
        if (fps >= 20)  return "§6明显卡顿 (" + fps + "帧)";
        return "§c极度卡顿/PPT (" + fps + "帧)";
    }

    /** 世界刻运行评价（单人能获取到精确 MSPT，多人则看客户端表现） */
    private static String evaluateWorldPace(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            float mspt = mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
            if (mspt <= 30.0f) return "§a运算轻松";
            if (mspt <= 48.0f) return "§e运算正常";
            if (mspt <= 60.0f) return "§6世界稍有卡顿";
            return "§c世界严重卡顿(时间变慢)";
        }
        return null;
    }

    /** 网络延时人性化 */
    private static String evaluateNetworkStatus(Minecraft mc) {
        if (mc.isLocalServer() || mc.getConnection() == null) return null;
        if (mc.player == null) return null;

        var playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (playerInfo == null) return null;

        int ping = playerInfo.getLatency();
        if (ping <= 50)  return "§a网络极好 (" + ping + "ms)";
        if (ping <= 120) return "§e网络正常 (" + ping + "ms)";
        if (ping <= 220) return "§6网络较差 (" + ping + "ms)";
        return "§c网络严重延迟 (" + ping + "ms)";
    }

    /** 内存占用人性化 */
    private static String formatMemoryHuman(int percent) {
        if (percent < 70) return "§7内存充足 (" + percent + "%)";
        if (percent < 85) return "§e内存占用偏高 (" + percent + "%)";
        return "§c内存告急可能卡顿 (" + percent + "%)";
    }

    /** 生动的时间与天气说明 */
    private static String getHumanTimeAndWeather(Level level) {
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long timeOfDay = (dayTime % 24000L);

        String period;
        if (timeOfDay >= 0 && timeOfDay < 1000) period = "黎明";
        else if (timeOfDay < 6000) period = "上午";
        else if (timeOfDay < 9000) period = "正午";
        else if (timeOfDay < 12000) period = "傍晚";
        else if (timeOfDay < 13000) period = "黄昏(可就寝)";
        else if (timeOfDay < 18000) period = "深夜";
        else if (timeOfDay < 22000) period = "深夜";
        else period = "清晨";

        String weather;
        if (level.isThundering()) weather = "§c暴风雷雨";
        else if (level.isRaining()) weather = "§9雨雪中";
        else weather = "§a晴空万里";

        return String.format("§b第%d天 %s · %s", day + 1, period, weather);
    }

    /** 月相说明（普通人根本不知道月相有什么用，点明怪物强弱） */
    private static String getHumanMoon(int phase) {
        return switch (phase) {
            case 0 -> "§d满月 (怪多且装备强)";
            case 4 -> "§7新月 (无月光/怪较弱)";
            default -> "§7常规月相";
        };
    }

    /** 人性化朝向 */
    private static String getHumanDirection(Direction d) {
        return switch (d) {
            case NORTH -> "正北 (向上)";
            case SOUTH -> "正南 (向下)";
            case WEST  -> "正西 (向左)";
            case EAST  -> "正东 (向右)";
            default    -> d.getName();
        };
    }

    /** 准星目标的人性化转写 */
    private static String getLookingAtDescription(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return null;

        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return "§e正看着: " + mc.level.getBlockState(bhr.getBlockPos()).getBlock().getName().getString();
        }

        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof LivingEntity living) {
                float health = living.getHealth();
                float maxHealth = living.getMaxHealth();
                int hearts = (int) Math.ceil(health / 2f);
                return String.format("§c目标: %s (§f%d/%d生命 §c%s§c)",
                        e.getDisplayName().getString(), (int) health, (int) maxHealth, "❤".repeat(Math.min(hearts, 10)));
            }
            return "§c目标: " + e.getDisplayName().getString();
        }
        return null;
    }

    /** 获取结构/遗迹名称 */
    private static String getStructureName(Minecraft mc, BlockPos pos) {
        if (mc.getSingleplayerServer() != null && mc.level != null) {
            ServerLevel sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
            if (sl != null) {
                StructureStart start = sl.structureManager().getStructureWithPieceAt(pos, h -> true);
                if (start.isValid()) {
                    ResourceLocation key = sl.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .getKey(start.getStructure());
                    if (key != null) {
                        return Component.translatable(Util.makeDescriptionId("structure", key)).getString();
                    }
                }
            }
        }
        return null;
    }

    /** 智能像素级流式折行排版 */
    private static List<Component> flowPackTags(List<Component> tags, Font font, int maxPixelWidth, String separator) {
        List<Component> lines = new ArrayList<>();
        if (tags.isEmpty()) return lines;

        MutableComponent currentLine = Component.empty();
        int currentLineWidth = 0;
        int sepWidth = font.width(separator);

        for (Component tag : tags) {
            int tagWidth = font.width(tag);

            if (tagWidth > maxPixelWidth) {
                String truncated = font.plainSubstrByWidth(tag.getString(), maxPixelWidth - font.width("...")) + "...";
                tag = Component.literal(truncated);
                tagWidth = font.width(tag);
            }

            if (currentLineWidth == 0) {
                currentLine.append(tag);
                currentLineWidth = tagWidth;
            } else if (currentLineWidth + sepWidth + tagWidth <= maxPixelWidth) {
                currentLine.append(Component.literal(separator)).append(tag);
                currentLineWidth += sepWidth + tagWidth;
            } else {
                lines.add(currentLine);
                currentLine = Component.empty().append(tag);
                currentLineWidth = tagWidth;
            }
        }

        if (currentLineWidth > 0) {
            lines.add(currentLine);
        }

        return lines;
    }
}