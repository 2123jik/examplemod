package com.example.examplemod.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;

public class RuneBgRemover {

    // 黑色容差（0~255）：纯黑写 10~15；如果有轻微黑边杂色，写 25~35
    private static final int BLACK_THRESHOLD = 28;

    public static void main(String[] args) throws Exception {
        File inputDir = new File("src/main/resources/assets/examplemod/textures/item/runes");
        File outputDir = new File("runes_transparent_out"); // 先输出到这里预览，满意后全选覆盖回原目录
        outputDir.mkdirs();

        File[] files = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("未找到 PNG 文件，请检查路径: " + inputDir.getAbsolutePath());
            return;
        }

        for (File file : files) {
            BufferedImage src = ImageIO.read(file);
            if (src == null) continue;

            BufferedImage dst = removeOuterBlackBackground(src, BLACK_THRESHOLD);
            ImageIO.write(dst, "png", new File(outputDir, file.getName()));
        }

        System.out.println(">>> 处理完成，共处理 " + files.length + " 张贴图，输出至: " + outputDir.getAbsolutePath());
    }

    private static BufferedImage removeOuterBlackBackground(BufferedImage src, int threshold) {
        int w = src.getWidth();
        int h = src.getHeight();

        // 统一转为带 Alpha 通道的 ARGB 图片（原图可能是无透明通道的 RGB）
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        boolean[][] visited = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();

        // 只从四条最外边入队，保证只清外圈黑底，不动石头内部的暗色符文
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

            img.setRGB(cx, cy, 0x00000000); // 设为完全透明

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
}