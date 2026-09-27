import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class ExportToSingleFile {

    public static void main(String[] args) {
        // 1. 使用相对路径（IDEA 默认运行目录是项目根目录）
        String sourceDirectory = "src/main/java/com/example/examplemod";
        String outputFile = "examplemod_all_code.txt";

        Path sourcePath = Paths.get(sourceDirectory);
        Path outputPath = Paths.get(outputFile);

        // 确保输出文件不在源码目录内，避免被自己读取
        if (!Files.exists(sourcePath)) {
            System.err.println("找不到源码目录: " + sourcePath.toAbsolutePath());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            try (Stream<Path> paths = Files.walk(sourcePath)) {
                paths.filter(Files::isRegularFile)
                        // 2. 补全过滤条件：只提取 .java 和 .json 文件
                        .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".json"))
                        // 3. 排序，保证每次导出的文件顺序一致
                        .sorted()
                        .forEach(file -> {
                            try {
                                Path relativePath = sourcePath.relativize(file);
                                writer.write("\n// ==================================================\n");
                                writer.write("// File: " + relativePath.toString() + "\n");
                                writer.write("// ==================================================\n\n");

                                // 4. 使用 Files.readString 替代 readAllBytes（Java 11+ 写法更简洁）
                                String content = Files.readString(file, StandardCharsets.UTF_8);
                                writer.write(content);
                                writer.write("\n");
                            } catch (IOException e) {
                                System.err.println("读取文件失败: " + file + " -> " + e.getMessage());
                            }
                        });
            }
            System.out.println("导出成功！文件已保存至: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("导出失败: " + e.getMessage());
        }
    }
}