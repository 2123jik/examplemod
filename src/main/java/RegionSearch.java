import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class RegionSearch {

    // ==================== 配置 ====================
    static final String TARGET = "bosses_of_mass_destruction:soul_star";
    static Path SEARCH_DIR = Paths.get("C:\\Users\\lyj\\AppData\\Roaming\\PrismLauncher\\instances\\hostility\\minecraft\\saves\\0X1\\region");
    static final Path OUTPUT = Paths.get("search_result.txt");
    // ===============================================

    static final byte[] NEEDLE = TARGET.getBytes(StandardCharsets.UTF_8);
    static final int THREADS = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8")); // 修乱码

        if (args.length > 0) SEARCH_DIR = Paths.get(args[0]);

        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SEARCH_DIR, "*.mca")) {
            for (Path p : ds) files.add(p);
        }
        System.out.println("扫描 " + files.size() + " 个 region 文件，线程数 " + THREADS);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<List<String>>> futures = new ArrayList<>();
        for (Path f : files) futures.add(pool.submit(() -> scanFile(f)));

        List<String> all = new ArrayList<>();
        for (Future<List<String>> f : futures) all.addAll(f.get());
        pool.shutdown();

        System.out.println("共找到 " + all.size() + " 处匹配");
        for (String s : all) System.out.println(s);
        Files.write(OUTPUT, all, StandardCharsets.UTF_8);
        System.out.println("结果已写入 " + OUTPUT.toAbsolutePath());
    }

    static List<String> scanFile(Path file) {
        List<String> results = new ArrayList<>();
        try {
            byte[] data = Files.readAllBytes(file);

            // 头部至少要有 8KB（位置表 + 时间戳表）
            if (data.length < 8192) {
                System.err.println("跳过（文件过小）: " + file);
                return results;
            }

            // 解析文件名里的 region 坐标：r.X.Z.mca
            String name = file.getFileName().toString();
            String[] parts = name.substring(0, name.length() - 4).split("\\.");
            int regionX = Integer.parseInt(parts[1]);
            int regionZ = Integer.parseInt(parts[2]);

            for (int i = 0; i < 1024; i++) {
                int base = i * 4;
                int offset = ((data[base] & 0xFF) << 16)
                        | ((data[base + 1] & 0xFF) << 8)
                        |  (data[base + 2] & 0xFF);
                int sectors = data[base + 3] & 0xFF;
                if (offset == 0 || sectors == 0) continue;

                // ★ 关键修复：用 long 防止溢出
                long byteOffset = ((long) offset) * 4096L;
                if (byteOffset + 5 > data.length) continue;

                int bo = (int) byteOffset;
                int length = ((data[bo] & 0xFF) << 24)
                        | ((data[bo + 1] & 0xFF) << 16)
                        | ((data[bo + 2] & 0xFF) << 8)
                        |  (data[bo + 3] & 0xFF);
                int compression = data[bo + 4] & 0xFF;
                if (length <= 1) continue;

                int compLen = length - 1;
                long compStartL = byteOffset + 5;
                if (compStartL + compLen > data.length) continue;
                int compStart = (int) compStartL;

                byte[] decompressed;
                try {
                    if (compression == 2) {
                        decompressed = inflateZlib(data, compStart, compLen);
                    } else if (compression == 1) {
                        decompressed = inflateGzip(data, compStart, compLen);
                    } else if (compression == 3) {
                        decompressed = Arrays.copyOfRange(data, compStart, compStart + compLen);
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                if (decompressed != null && indexOf(decompressed, NEEDLE) >= 0) {
                    int chunkLocalX = i % 32;
                    int chunkLocalZ = i / 32;
                    int worldChunkX = regionX * 32 + chunkLocalX;
                    int worldChunkZ = regionZ * 32 + chunkLocalZ;
                    int worldBlockX = worldChunkX * 16;
                    int worldBlockZ = worldChunkZ * 16;
                    results.add(String.format(
                            "%s  chunk(%d,%d)  世界区块(%d,%d)  大约方块坐标 (%d,?,%d)",
                            name, chunkLocalX, chunkLocalZ,
                            worldChunkX, worldChunkZ,
                            worldBlockX, worldBlockZ));
                }
            }
        } catch (Exception e) {
            System.err.println("处理 " + file + " 出错: " + e);
        }
        return results;
    }

    static byte[] inflateZlib(byte[] src, int off, int len) throws DataFormatException {
        Inflater inf = new Inflater();
        inf.setInput(src, off, len);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(len * 3, 8192));
        byte[] buf = new byte[16384];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0) {
                    if (inf.needsInput() || inf.needsDictionary()) break;
                }
                out.write(buf, 0, n);
            }
        } finally {
            inf.end();
        }
        return out.toByteArray();
    }

    static byte[] inflateGzip(byte[] src, int off, int len) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(src, off, len), 16384)) {
            return gz.readAllBytes();
        }
    }

    static int indexOf(byte[] hay, byte[] needle) {
        int n = hay.length, m = needle.length;
        if (m == 0) return 0;
        outer:
        for (int i = 0; i <= n - m; i++) {
            for (int j = 0; j < m; j++) {
                if (hay[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}