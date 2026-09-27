import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.File;
import java.nio.file.Files;

public class RemoveComments {
    public static void process(File root) throws Exception {
        Files.walk(root.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(p);
                        LexicalPreservingPrinter.setup(cu);

                        // 删除所有内部注释
                        cu.getAllContainedComments().forEach(Comment::remove);
                        // 删除文件头部注释（package 之前的）
                        cu.setComment(null);

                        Files.writeString(p, LexicalPreservingPrinter.print(cu));
                        System.out.println("已清理: " + p);
                    } catch (Exception e) {
                        System.err.println("失败: " + p + " -> " + e.getMessage());
                    }
                });
    }
}