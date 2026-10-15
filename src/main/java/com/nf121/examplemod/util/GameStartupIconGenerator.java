package com.nf121.examplemod.util;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

public class GameStartupIconGenerator {

    private static boolean hasGenerated = false;

    /**
     * 进入游戏时触发
     */
    public static void generateTexturesOnStartup() {
        if (hasGenerated) return;
        hasGenerated = true;

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        // 两张来源纹理路径
        ResourceLocation sunderingLoc = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "textures/mob_effect/sundering.png");
        ResourceLocation fragilityLoc = ResourceLocation.fromNamespaceAndPath("mowziesmobs", "textures/mob_effect/fragility.png");

        try (InputStream sunderingStream = resourceManager.open(sunderingLoc);
             InputStream fragilityStream = resourceManager.open(fragilityLoc)) {

            BufferedImage baseSundering = ImageIO.read(sunderingStream);
            BufferedImage baseFragility = ImageIO.read(fragilityStream);

            // 1. 定位输出目录：同时输出到开发源码 resources 目录 和 run 运行目录（双保险）
            File devSourceDir = Paths.get("../../src/main/resources/assets/" + ExampleMod.MOD_ID + "/textures/mob_effect").toFile();
            File runOutputDir = new File("generated_textures/assets/" + ExampleMod.MOD_ID + "/textures/mob_effect");

            if (!devSourceDir.exists()) devSourceDir.mkdirs();
            if (!runOutputDir.exists()) runOutputDir.mkdirs();

            System.out.println("[ExampleMod] 开始进游戏自动生成 26 张学派削抗贴图...");

            for (var entry : ModMobEffects.REGISTERED_SCHOOLS) {
                // 直接使用官方精准颜色！
                int rgb = entry.color();

                // 1. 处理 sundering -> TOTAL (裂缝中填入颜色)
                BufferedImage totalImg = processSundering(baseSundering, rgb);
                saveImage(totalImg, entry.name() + "_shred_total.png", devSourceDir, runOutputDir);

                // 2. 处理 fragility -> FLAT (碎屑颗粒填入颜色)
                BufferedImage flatImg = processFragility(baseFragility, rgb);
                saveImage(flatImg, entry.name() + "_shred_flat.png", devSourceDir, runOutputDir);
            }

            System.out.println("[ExampleMod] 全部贴图已写入: " + devSourceDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("[ExampleMod] 贴图生成失败，请确认已安装 Apothic Attributes 和 Mowzie's Mobs！");
            e.printStackTrace();
        }
    }

    private static void saveImage(BufferedImage img, String fileName, File devDir, File runDir) {
        try {
            File f1 = new File(devDir, fileName);
            File f2 = new File(runDir, fileName);
            ImageIO.write(img, "PNG", f1);
            ImageIO.write(img, "PNG", f2);
        } catch (Exception ignored) {}
    }

    /**
     * 【裂痕填色算法】：
     * - 盾牌主体（高明度灰色）：保留冷色金属盔甲质感，仅附着极微弱的学派反光。
     * - 切缝裂痕（暗色切口与缝隙）：注入纯正高亮学派魔力色！
     */
    private static BufferedImage processSundering(BufferedImage src, int schoolRgb) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int sR = (schoolRgb >> 16) & 0xFF;
        int sG = (schoolRgb >> 8) & 0xFF;
        int sB = schoolRgb & 0xFF;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = src.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                if (a == 0) continue;

                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // 判断是否是“裂缝”（暗色切口、中心斜切线通常明度较低）
                // 阈值：明度小于 130 为裂缝处，大于等于 130 为盾牌本体金属
                if (gray < 130) {
                    // 裂口：填充饱满明亮的学派能量色
                    int outR = Math.min(255, (sR * 255) / Math.max(1, gray + 80));
                    int outG = Math.min(255, (sG * 255) / Math.max(1, gray + 80));
                    int outB = Math.min(255, (sB * 255) / Math.max(1, gray + 80));
                    result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outB);
                } else {
                    // 盾牌本体：保留 85% 钢灰金属色 + 15% 学派微光漫反射
                    int outR = (int) (gray * 0.85 + sR * 0.15);
                    int outG = (int) (gray * 0.85 + sG * 0.15);
                    int outB = (int) (gray * 0.85 + sB * 0.15);
                    result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outB);
                }
            }
        }
        return result;
    }

    /**
     * 【碎屑填色算法】：
     * - 盾牌主体（左下方）：保留钢铁质感。
     * - 崩解脱落的碎块粒子（右上半区域及孤立小块）：染上完全饱和的学派色！
     */
    private static BufferedImage processFragility(BufferedImage src, int schoolRgb) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int sR = (schoolRgb >> 16) & 0xFF;
        int sG = (schoolRgb >> 8) & 0xFF;
        int sB = schoolRgb & 0xFF;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = src.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                if (a == 0) continue;

                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // fragility 的碎片全部聚集在右上角（x 偏大 且 y 偏小）
                // 空间判定：右上角散落碎片区域 (x >= 9 且 y <= 9 且 非主体大块)
                boolean isFlyingDebris = (x >= 9 && y <= 8) || (gray < 110);

                if (isFlyingDebris) {
                    // 碎块/剥落面：直接染成纯正的学派魔力色
                    int outR = (gray * sR) / 255;
                    int outG = (gray * sG) / 255;
                    int outB = (gray * sB) / 255;
                    result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outR);
                } else {
                    // 左下盾牌主体：金属灰质感
                    int outR = (int) (gray * 0.85 + sR * 0.15);
                    int outG = (int) (gray * 0.85 + sG * 0.15);
                    int outB = (int) (gray * 0.85 + sB * 0.15);
                    result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outB);
                }
            }
        }
        return result;
    }
}