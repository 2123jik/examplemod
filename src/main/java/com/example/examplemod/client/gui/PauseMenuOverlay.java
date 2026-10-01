package com.example.examplemod.client.gui;

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

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] MOON_NAMES = {"满月", "亏凸", "下弦", "残月", "新月", "娥眉", "上弦", "盈凸"};

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        BlockPos pos = mc.player.blockPosition();

        // 中间主按钮宽度为 204px (两边各 102px)，预留 14px 绝对安全间隔，绝不压按钮
        int marginX = 8;
        int marginY = 8;
        int safeRightEdge = (screen.width / 2) - 102 - 14;
        int maxAllowedWidth = safeRightEdge - marginX;

        if (maxAllowedWidth < 80) return;

        // 收集标签
        List<Component> tags = collectTags(mc, pos);

        // 像素级流式换行
        List<Component> renderedLines = flowPackTags(tags, font, maxAllowedWidth, " §7• ");

        // 测量实际内容宽高
        int panelWidth = 0;
        for (Component line : renderedLines) {
            panelWidth = Math.max(panelWidth, font.width(line));
        }

        int padding = 4;
        int lineHeight = font.lineHeight + 2;
        int panelHeight = renderedLines.size() * lineHeight;

        // 背景与外框
        graphics.fill(marginX - padding, marginY - padding,
                marginX + panelWidth + padding, marginY + panelHeight + padding - 2,
                0xAA111111);
        graphics.fill(marginX - padding, marginY - padding,
                marginX + panelWidth + padding, marginY - padding + 1,
                0x40FFFFFF);

        // 渲染文本
        for (int i = 0; i < renderedLines.size(); i++) {
            graphics.drawString(font, renderedLines.get(i), marginX, marginY + (i * lineHeight), 0xFFFFFF, true);
        }
    }

    private static List<Component> collectTags(Minecraft mc, BlockPos pos) {
        List<Component> tags = new ArrayList<>();

        // 1. 位置与下界折算
        tags.add(Component.literal(String.format("§6XYZ: §f%d %d %d", pos.getX(), pos.getY(), pos.getZ())));
        if (mc.level.dimension() == Level.OVERWORLD) {
            tags.add(Component.literal(String.format("§8下界: §7%d %d", pos.getX() / 8, pos.getZ() / 8)));
        } else if (mc.level.dimension() == Level.NETHER) {
            tags.add(Component.literal(String.format("§8主界: §7%d %d", pos.getX() * 8, pos.getZ() * 8)));
        }

        // 2. 朝向与区块
        Direction dir = mc.player.getDirection();
        float yaw = (mc.player.getYRot() % 360 + 360) % 360;
        tags.add(Component.literal(String.format("§f%s §8%.0f°", getDirSimple(dir), yaw)));
        tags.add(Component.literal(String.format("§8区块[%d,%d]", pos.getX() >> 4, pos.getZ() >> 4)));

        // 3. 生态群系
        Holder<Biome> biome = mc.level.getBiome(pos);
        String biomeName = biome.unwrapKey()
                .map(k -> Component.translatable(Util.makeDescriptionId("biome", k.location())).getString())
                .orElse("未知群系");
        tags.add(Component.literal("§a" + biomeName));

        // 4. 结构判定 (修正：使用真实的谓词重载)
        String struct = getStructureName(mc, pos);
        if (struct != null) {
            tags.add(Component.literal("§e" + struct));
        }

        // 5. 光照
        tags.add(Component.literal(String.format("§f光:§b天%d §e块%d",
                mc.level.getBrightness(LightLayer.SKY, pos),
                mc.level.getBrightness(LightLayer.BLOCK, pos))));

        // 6. 天象与时间
        long dayTime = mc.level.getDayTime();
        long day = dayTime / 24000L;
        long timeOfDay = (dayTime + 6000L) % 24000L;
        int hours = (int) (timeOfDay / 1000L);
        int mins = (int) ((timeOfDay % 1000L) * 60L / 1000L);
        String weather = mc.level.isThundering() ? "§c雷暴" : (mc.level.isRaining() ? "§9雨" : "§a晴");

        tags.add(Component.literal(String.format("§bDay %d %02d:%02d", day, hours, mins)));
        tags.add(Component.literal("§d" + MOON_NAMES[mc.level.getMoonPhase()]));
        tags.add(Component.literal(weather));

        // 7. 准星目视
        String target = getTargetSummary(mc);
        if (target != null) {
            tags.add(Component.literal(target));
        }

        // 8. 性能与实体统计 (修正：使用 LevelRenderer 的真实方法 getEntityStatistics)
        tags.add(Component.literal(String.format("§a%d FPS", mc.getFps())));

        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMem = rt.maxMemory() / 1024 / 1024;
        tags.add(Component.literal(String.format("§7内存 %d%%", (usedMem * 100 / maxMem))));

        // 从 getEntityStatistics() 中截取渲染实体信息，或者直接取总实体
        tags.add(Component.literal(String.format("§7%s", parseEntitySummary(mc))));
        tags.add(Component.literal("§8" + LocalTime.now().format(TIME_FMT)));

        return tags;
    }

    /**
     * 解析 LevelRenderer 真实的 getEntityStatistics() 返回值
     * 官方格式为: "E: 12/48, B: 0, SD: 10" -> 截取提取前缀 "E: 12/48"
     */
    private static String parseEntitySummary(Minecraft mc) {
        if (mc.levelRenderer != null) {
            String stats = mc.levelRenderer.getEntityStatistics();
            int commaIdx = stats.indexOf(',');
            if (commaIdx != -1) {
                return stats.substring(0, commaIdx); // 返回 "E: 12/48"
            }
            return stats;
        }
        return "实体: " + mc.level.getEntityCount();
    }

    /**
     * 修正：调用真实的 StructureManager#getStructureWithPieceAt(BlockPos, Predicate)
     */
    private static String getStructureName(Minecraft mc, BlockPos pos) {
        if (mc.getSingleplayerServer() != null && mc.level != null) {
            ServerLevel sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
            if (sl != null) {
                // 传入 h -> true 匹配当前位置的任意结构 Piece
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

    private static String getTargetSummary(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return null;

        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return "§e目视: " + mc.level.getBlockState(bhr.getBlockPos()).getBlock().getName().getString();
        }
        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof LivingEntity living) {
                return String.format("§c目视: %s (%.0f♥)", e.getDisplayName().getString(), living.getHealth() / 2f);
            }
            return "§c目视: " + e.getDisplayName().getString();
        }
        return null;
    }

    private static String getDirSimple(Direction d) {
        return switch (d) {
            case NORTH -> "北(-Z)";
            case SOUTH -> "南(+Z)";
            case WEST  -> "西(-X)";
            case EAST  -> "东(+X)";
            default    -> d.getName();
        };
    }
}