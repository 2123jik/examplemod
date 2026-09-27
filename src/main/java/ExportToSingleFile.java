import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class ExportToSingleFile {

    public static void main(String[] args) {
        // 1. 这里替换成你截图中包的本地真实路径
        // 例如：C:\\你的项目\\src\\main\\java\\com\\example\\examplemod
        String sourceDirectory = "C:\\mod\\examplemod-template-1.21.1 (2)\\src\\main\\java\\com\\example\\examplemod";
        
        // 2. 导出的目标文件路径
        String outputFile = "examplemod_all_code.txt";

        Path sourcePath = Paths.get(sourceDirectory);
        Path outputPath = Paths.get(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            // 使用 Files.walk 遍历目录
            try (Stream<Path> paths = Files.walk(sourcePath)) {
                paths.filter(Files::isRegularFile) // 只要文件，不要目录
                     .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".json")) // 只提取 java 和 json 文件（可根据需要修改）
                     .forEach(file -> {
                         try {
                             // 写入分隔符和相对路径
                             Path relativePath = sourcePath.relativize(file);
                             writer.write("\n// ==================================================\n");
                             writer.write("// File: " + relativePath.toString() + "\n");
                             writer.write("// ==================================================\n\n");
                             
                             // 读取文件内容并写入
                             String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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