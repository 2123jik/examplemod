package com.example.examplemod.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuneTexturePadder {

    private static final String[] RUNE_NAMES = {
            "el", "eld", "tir", "nef", "eth", "ith", "tal", "ral", "ort", "thul",
            "amn", "sol", "shael", "dol", "hel", "io", "lum", "ko", "fal", "lem",
            "pul", "um", "mal", "ist", "gul", "vex", "ohm", "lo", "sur", "ber",
            "jah", "cham", "zod"
    };

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final int START_INDEX = 4; // ia_700000004 -> 末尾数字 700000004，取模或减起始值

    public static void main(String[] args) throws Exception {
        File inputDir = new File("raw_runes_28x28"); // 放 ia_700000004.png ~ ia_700000036.png 的目录
        File outputDir = new File("examplemod_Datapack/assets/examplemod/textures/item/runes");
        outputDir.mkdirs();

        File[] files = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("未找到任何 PNG 文件: " + inputDir.getAbsolutePath());
            return;
        }

        int success = 0;
        for (File f : files) {
            Matcher m = NUMBER_PATTERN.matcher(f.getName());
            if (!m.find()) {
                System.out.println("跳过无法识别编号的文件: " + f.getName());
                continue;
            }

            // 取最后两位或减去 700000000（如 700000004 -> 4）
            long rawNum = Long.parseLong(m.group(1));
            int seqNum = (int) (rawNum % 1000); // 700000004 -> 4, 700000036 -> 36
            int runeId = seqNum - START_INDEX + 1; // 4 -> 1, 36 -> 33

            if (runeId < 1 || runeId > RUNE_NAMES.length) {
                System.out.println("编号超出 1~33 范围，已跳过: " + f.getName() + " (解析出 runeId=" + runeId + ")");
                continue;
            }

            BufferedImage src = ImageIO.read(f);
            int targetSize = 32;
            BufferedImage dst = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = dst.createGraphics();
            int offsetX = (targetSize - src.getWidth()) / 2;
            int offsetY = (targetSize - src.getHeight()) / 2;
            g.drawImage(src, offsetX, offsetY, null);
            g.dispose();

            // 如果原图带纯黑背景需要顺便抠成透明，取消下面循环的注释即可：
            /*
            for (int y = 0; y < targetSize; y++) {
                for (int x = 0; x < targetSize; x++) {
                    int argb = dst.getRGB(x, y);
                    int r = (argb >> 16) & 0xFF, gCol = (argb >> 8) & 0xFF, b = argb & 0xFF;
                    if (r < 8 && gCol < 8 && b < 8) dst.setRGB(x, y, 0x00000000);
                }
            }
            */

            String outName = String.format("rune_%02d_%s.png", runeId, RUNE_NAMES[runeId - 1]);
            ImageIO.write(dst, "PNG", new File(outputDir, outName));
            System.out.printf("%s (%dx%d) -> %s (32x32)%n", f.getName(), src.getWidth(), src.getHeight(), outName);
            success++;
        }

        System.out.println(">>> 处理完成，共成功转换 " + success + " 张符文贴图！");
    }
}