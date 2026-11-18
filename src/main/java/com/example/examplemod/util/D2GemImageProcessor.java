package com.example.examplemod.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class D2GemImageProcessor {

    private static final String INPUT_DIR =
            "C:\\Users\\lyj\\Downloads\\图片助手(ImageAssistant) 批量图片下载器\\wiki.projectdiablo2.com\\2026-11-17_15-23-27\\Runes_-_Project_Diablo_2";

    private static final String OUTPUT_DIR =
            "src/main/resources/assets/examplemod/textures/item/d2_gems";

    private static final long START_INDEX = 1900000033L;
    private static final int BLACK_THRESHOLD = 28;
    private static final boolean PAD_TO_32 = true; // 28x28 居中扩充为 32x32 透明底（2的幂，适配 MC 贴图）

    private static final List<String> GEM_SLUGS = List.of(
            "d2_sapphire", "d2_emerald", "d2_topaz", "d2_ruby",
            "d2_amethyst", "d2_diamond", "d2_skull"
    );

    private static final List<String> TIERS = List.of(
            "chipped", "flawed", "normal", "flawless", "perfect"
    );

    public static void main(String[] args) throws Exception {
        File inDir = new File(INPUT_DIR);
        File outDir = new File(OUTPUT_DIR);
        outDir.mkdirs();

        long idx = START_INDEX;
        int count = 0;

        for (String gemSlug : GEM_SLUGS) {
            for (String tier : TIERS) {
                File srcFile = new File(inDir, "ia_" + idx + ".png");
                idx++;

                if (!srcFile.exists()) {
                    System.err.println("缺失文件，跳过: " + srcFile.getName());
                    continue;
                }

                BufferedImage src = ImageIO.read(srcFile);
                if (src == null) continue;

                BufferedImage cleaned = removeOuterBlackBackground(src, BLACK_THRESHOLD);
                if (PAD_TO_32) {
                    cleaned = padToSquare32(cleaned);
                }

                // 1. 保存分品质版本 (例如 d2_sapphire_perfect.png)
                String tierFileName = gemSlug + "_" + tier + ".png";
                ImageIO.write(cleaned, "png", new File(outDir, tierFileName));

                // 2. 用 perfect 作为默认主贴图 (例如 d2_sapphire.png)
                if ("perfect".equals(tier)) {
                    ImageIO.write(cleaned, "png", new File(outDir, gemSlug + ".png"));
                }

                System.out.println(srcFile.getName() + " -> " + tierFileName);
                count++;
            }
        }

        System.out.println(">>> 宝石贴图全部处理完成！共处理 " + count + " 张，输出至: " + outDir.getAbsolutePath());
    }

    private static BufferedImage removeOuterBlackBackground(BufferedImage src, int threshold) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        boolean[][] visited = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < w; x++) {
            tryEnqueue(img, x, 0, threshold, visited, queue);
            tryEnqueue(img, x, h - 1, threshold, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            tryEnqueue(img, 0, y, threshold, visited, queue);
            tryEnqueue(img, w - 1, y, threshold, visited, queue);
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];
            img.setRGB(cx, cy, 0x00000000);

            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                    tryEnqueue(img, nx, ny, threshold, visited, queue);
                }
            }
        }
        return img;
    }

    private static void tryEnqueue(BufferedImage img, int x, int y, int threshold, boolean[][] visited, Queue<int[]> queue) {
        if (visited[y][x]) return;
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        if (a == 0 || (r <= threshold && g <= threshold && b <= threshold)) {
            visited[y][x] = true;
            queue.add(new int[]{x, y});
        }
    }

    private static BufferedImage padToSquare32(BufferedImage src) {
        int size = Math.max(32, Math.max(src.getWidth(), src.getHeight()));
        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        int x = (size - src.getWidth()) / 2;
        int y = (size - src.getHeight()) / 2;
        g.drawImage(src, x, y, null);
        g.dispose();
        return canvas;
    }
}