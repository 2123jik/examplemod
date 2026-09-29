package main;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.Indentation.IndentType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ExportAst {

    // 剔除开发提示性注解
    static final Set<String> STRIP_ANNOTATIONS = Set.of(
            "Override", "SuppressWarnings", "Deprecated", "FunctionalInterface",
            "SafeVarargs", "Nullable", "NotNull", "Nonnull", "CheckForNull"
    );

    public static void main(String[] args) throws IOException {
        String srcDir = args.length > 0 ? args[0] : "C:\\mod\\ModernUI_Source\\icyllis\\modernui";
        String outFile = args.length > 1 ? args[1] : srcDir + "\\all_code_ast.txt";

        Path src = Paths.get(srcDir);
        Path out = Paths.get(outFile);

        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setAttributeComments(false);

        JavaParser parser = new JavaParser(config);

        DefaultPrinterConfiguration printerConfig = new DefaultPrinterConfiguration();
        printerConfig.addOption(new DefaultConfigurationOption(
                ConfigOption.INDENTATION, new Indentation(IndentType.SPACES, 2)
        ));
        printerConfig.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_COMMENTS, false));
        printerConfig.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_JAVADOC, false));
        printerConfig.addOption(new DefaultConfigurationOption(ConfigOption.END_OF_LINE_CHARACTER, "\n"));

        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(printerConfig);

        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        int ok = 0, fail = 0;

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (Path f : files) {
                String rel = src.relativize(f).toString().replace('\\', '/');
                try {
                    ParseResult<CompilationUnit> result = parser.parse(f);
                    if (!result.isSuccessful() || result.getResult().isEmpty()) {
                        fail++;
                        continue;
                    }

                    CompilationUnit cu = result.getResult().get();
                    cu.getImports().clear();
                    cu.removePackageDeclaration();

                    cu.accept(new ModifierVisitor<Void>() {

                        // 1. 静态块/初始化块全部干掉（大量常量表、原生绑定）
                        @Override
                        public Visitable visit(InitializerDeclaration id, Void a) {
                            return null;
                        }

                        // 2. 方法抽骨架
                        @Override
                        public Visitable visit(MethodDeclaration md, Void a) {
                            if (md.isPrivate()) return null; // 私有方法干掉
                            stripAnn(md);
                            md.removeBody(); // 核心：移除方法体，直接变为 public void foo(); 形式
                            return super.visit(md, a);
                        }

                        // 3. 构造器抽骨架
                        @Override
                        public Visitable visit(ConstructorDeclaration cd, Void a) {
                            if (cd.isPrivate()) return null; // 私有构造器不关心
                            stripAnn(cd);
                            // 构造器保留为 {} 空实现
                            cd.setBody(new BlockStmt());
                            return super.visit(cd, a);
                        }

                        // 4. 字段脱水
                        @Override
                        public Visitable visit(FieldDeclaration fd, Void a) {
                            if (fd.isPrivate()) return null; // 私有字段干掉
                            stripAnn(fd);
                            // 清空初始值（例如巨型数组初始化、new 实例等），只保留类型和名字
                            fd.getVariables().forEach(VariableDeclarator::removeInitializer);
                            return super.visit(fd, a);
                        }

                        // 5. 类与接口过滤
                        @Override
                        public Visitable visit(ClassOrInterfaceDeclaration cd, Void a) {
                            if (cd.isPrivate()) return null; // 私有内部类干掉
                            stripAnn(cd);
                            if (cd.isInterface()) {
                                cd.getMethods().forEach(m -> {
                                    m.removeModifier(Modifier.Keyword.PUBLIC);
                                    m.removeModifier(Modifier.Keyword.ABSTRACT);
                                });
                            }
                            return super.visit(cd, a);
                        }

                        // 6. 枚举项精简
                        @Override
                        public Visitable visit(EnumConstantDeclaration ecd, Void a) {
                            stripAnn(ecd);
                            ecd.getArguments().clear(); // 去除例如 RED(255, 0, 0) 的参数 -> RED
                            ecd.getClassBody().clear(); // 去除枚举单项里的匿名类覆写
                            return super.visit(ecd, a);
                        }

                        @Override
                        public Visitable visit(RecordDeclaration rd, Void a) {
                            if (rd.isPrivate()) return null;
                            stripAnn(rd);
                            return super.visit(rd, a);
                        }

                        // 7. 压扁类型限定名
                        @Override
                        public Visitable visit(ClassOrInterfaceType ct, Void a) {
                            ct.removeScope();
                            return super.visit(ct, a);
                        }
                    }, null);

                    String code = prettyPrinter.print(cu);
                    code = postProcessWhitespace(code);

                    if (!code.isBlank()) {
                        w.write("// === " + rel + " ===\n");
                        w.write(code);
                        w.write("\n\n");
                        ok++;
                    }
                } catch (Exception ignored) {
                    fail++;
                }
            }
        }
        System.out.println("纯 API 骨架导出完成: " + out.toAbsolutePath());
        System.out.println("成功: " + ok + ", 失败: " + fail);
    }

    static void stripAnn(Node node) {
        if (!(node instanceof BodyDeclaration<?> bd)) return;
        bd.getAnnotations().removeIf(a -> {
            String name = a.getNameAsString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) name = name.substring(dot + 1);
            return STRIP_ANNOTATIONS.contains(name);
        });
    }

    static String postProcessWhitespace(String code) {
        return code
                // 移除空行
                .replaceAll("(?m)^[ \\t]*\\R", "")
                // 把空构造函数压成单行 "MyClass() {}"
                .replaceAll("\\)\\s*\\{\\s*\\}", ") {}")
                // 清理行尾空白
                .replaceAll("(?m)[ \\t]+$", "");
    }
}