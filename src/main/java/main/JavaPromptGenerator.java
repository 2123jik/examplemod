package main;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.LiteralExpr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaPromptGenerator {

    // 剔除编译器内部注解，保留业务/架构注解（Spring, JPA, Lombok, Validation等）
    private static final Set<String> IGNORE_ANNOTATIONS = Set.of(
            "Override", "SuppressWarnings", "Generated", "Nullable", "NotNull", "Nonnull"
    );

    private static final Pattern CLEAN_DOC_PATTERN = Pattern.compile("(?m)^\\s*\\*\\s*(@.*)?");

    public static void main(String[] args) throws IOException {
        String srcDir = args.length > 0 ? args[0] : ".";
        String outFile = args.length > 1 ? args[1] : "project_context.prompt.md";

        Path src = Paths.get(srcDir).toAbsolutePath().normalize();
        Path out = Paths.get(outFile).toAbsolutePath().normalize();

        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setAttributeComments(true);

        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.endsWith("package-info.java") && !p.endsWith("module-info.java"))
                    .sorted()
                    .toList();
        }

        System.out.printf("扫描到 %d 个文件，正在直接生成 Prompt 上下文...\n", files.size());

        AtomicInteger okCount = new AtomicInteger();

        // 并行提取精简 Blueprint
        List<String> blueprints = files.parallelStream().map(file -> {
            try {
                JavaParser parser = new JavaParser(config);
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (!result.isSuccessful() || result.getResult().isEmpty()) return null;

                CompilationUnit cu = result.getResult().get();
                String relPath = src.relativize(file).toString().replace('\\', '/');

                String content = extractCompactBlueprint(cu);
                if (content.isBlank()) return null;

                okCount.incrementAndGet();
                return "// --- " + relPath + " ---\n" + content;
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).sorted().toList();

        // 直接拼装成 LLM 最易吸收的结构化 Prompt 格式
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("<project_context>\n");
            w.write("这是当前 Java 项目的核心架构与 API 蓝图上下文（实现代码已折叠，保留完整实体属性、方法签名、业务注解与接口契约）：\n\n");
            w.write("```java\n");
            for (String bp : blueprints) {
                w.write(bp);
                w.write("\n\n");
            }
            w.write("```\n");
            w.write("</project_context>\n\n");
            w.write("请结合上述项目架构信息，回答接下来的问题。\n");
        }

        System.out.printf("Prompt 生成完毕 -> %s (成功汇聚 %d 个类)\n", out, okCount.get());
    }

    /**
     * 将单个编译单元提炼为极紧凑的 Blueprint 表达
     */
    private static String extractCompactBlueprint(CompilationUnit cu) {
        StringBuilder sb = new StringBuilder();

        // 1. 保留 Package 声明
        cu.getPackageDeclaration().ifPresent(p -> sb.append(p.toString().trim()).append("\n"));

        // 2. 遍历顶层类型（Class, Interface, Enum, Record, @interface）
        for (TypeDeclaration<?> type : cu.getTypes()) {
            appendType(sb, type, "");
        }
        return sb.toString().trim();
    }

    private static void appendType(StringBuilder sb, TypeDeclaration<?> type, String indent) {
        // 注释提取为单行功能摘要
        getCompactDoc(type).ifPresent(doc -> sb.append(indent).append("// ").append(doc).append("\n"));

        // 关键注解（如 @RestController, @Data）
        appendAnnotations(sb, type, indent);

        // 声明头（省略冗余的 public，只标 abstract/interface/enum）
        sb.append(indent);
        String kind = "class";
        if (type.isClassOrInterfaceDeclaration()) {
            kind = type.asClassOrInterfaceDeclaration().isInterface() ? "interface" : "class";
        } else if (type.isEnumDeclaration()) {
            kind = "enum";
        } else if (type.isRecordDeclaration()) {
            kind = "record";
        } else if (type.isAnnotationDeclaration()) {
            kind = "@interface";
        }

        if (type.hasModifier(Modifier.Keyword.ABSTRACT) && !kind.equals("interface")) {
            sb.append("abstract ");
        }

        sb.append(kind).append(" ").append(type.getNameAsString());

        // 继承与实现关系（完全保留）
        if (type.isClassOrInterfaceDeclaration()) {
            var cid = type.asClassOrInterfaceDeclaration();
            if (!cid.getExtendedTypes().isEmpty()) {
                sb.append(" extends ").append(cid.getExtendedTypes().stream().map(Node::toString).collect(Collectors.joining(", ")));
            }
            if (!cid.getImplementedTypes().isEmpty()) {
                sb.append(" implements ").append(cid.getImplementedTypes().stream().map(Node::toString).collect(Collectors.joining(", ")));
            }
        } else if (type.isRecordDeclaration()) {
            var rd = type.asRecordDeclaration();
            sb.append("(").append(rd.getParameters().stream().map(p -> p.getTypeAsString() + " " + p.getNameAsString()).collect(Collectors.joining(", "))).append(")");
        }

        sb.append(" {\n");

        // 内部成员
        String subIndent = indent + "  ";

        // 枚举常量紧凑单行输出
        if (type.isEnumDeclaration()) {
            List<String> constants = type.asEnumDeclaration().getEntries().stream()
                    .map(EnumConstantDeclaration::getNameAsString)
                    .toList();
            if (!constants.isEmpty()) {
                sb.append(subIndent).append("ENUMS: ").append(String.join(", ", constants)).append(";\n");
            }
        }

        // 字段（完整保留，包含 private）
        for (FieldDeclaration field : type.getFields()) {
            appendField(sb, field, subIndent);
        }

        // 构造方法
        for (ConstructorDeclaration cd : type.getConstructors()) {
            getCompactDoc(cd).ifPresent(doc -> sb.append(subIndent).append("// ").append(doc).append("\n"));
            appendAnnotations(sb, cd, subIndent);
            sb.append(subIndent).append(formatModifiers(cd.getModifiers(), false))
                    .append(cd.getNameAsString()).append("(")
                    .append(formatParams(cd.getParameters())).append(");\n");
        }

        // 普通方法
        for (MethodDeclaration md : type.getMethods()) {
            appendMethod(sb, md, subIndent);
        }

        // 递归嵌套类
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nestedType) {
                appendType(sb, nestedType, subIndent);
            }
        }

        sb.append(indent).append("}\n");
    }

    private static void appendField(StringBuilder sb, FieldDeclaration field, String indent) {
        getCompactDoc(field).ifPresent(doc -> sb.append(indent).append("// ").append(doc).append("\n"));
        appendAnnotations(sb, field, indent);

        sb.append(indent).append(formatModifiers(field.getModifiers(), false))
                .append(field.getElementType().asString()).append(" ");

        List<String> vars = new ArrayList<>();
        field.getVariables().forEach(v -> {
            String expr = v.getNameAsString();
            // 常量保留简短字面量
            if (field.isStatic() && field.isFinal() && v.getInitializer().isPresent()) {
                var init = v.getInitializer().get();
                if (init instanceof LiteralExpr && init.toString().length() < 40) {
                    expr += " = " + init;
                }
            }
            vars.add(expr);
        });

        sb.append(String.join(", ", vars)).append(";\n");
    }

    private static void appendMethod(StringBuilder sb, MethodDeclaration md, String indent) {
        String doc = getCompactDoc(md).orElse("");
        appendAnnotations(sb, md, indent);

        sb.append(indent).append(formatModifiers(md.getModifiers(), false));
        if (!md.getTypeParameters().isEmpty()) {
            sb.append("<").append(md.getTypeParameters().stream().map(Node::toString).collect(Collectors.joining(", "))).append("> ");
        }

        sb.append(md.getTypeAsString()).append(" ")
                .append(md.getNameAsString()).append("(")
                .append(formatParams(md.getParameters())).append(")");

        if (!md.getThrownExceptions().isEmpty()) {
            sb.append(" throws ").append(md.getThrownExceptions().stream().map(Node::toString).collect(Collectors.joining(", ")));
        }
        sb.append(";");

        // 将 Javadoc 浓缩为行尾单行注释，极大节省纵向 Token
        if (!doc.isBlank()) {
            sb.append(" // ").append(doc);
        }
        sb.append("\n");
    }

    /**
     * 关键优化：默认就是 public，因此去掉 public，只显式标记 private / protected / static / final
     */
    private static String formatModifiers(List<Modifier> modifiers, boolean isInterface) {
        StringBuilder sb = new StringBuilder();
        for (Modifier m : modifiers) {
            String k = m.getKeyword().asString();
            if (k.equals("public")) continue; // 省掉 public！
            sb.append(k).append(" ");
        }
        return sb.toString();
    }

    private static String formatParams(List<Parameter> parameters) {
        return parameters.stream()
                .map(p -> p.getTypeAsString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", "));
    }

    private static void appendAnnotations(StringBuilder sb, BodyDeclaration<?> decl, String indent) {
        for (AnnotationExpr ann : decl.getAnnotations()) {
            String name = ann.getNameAsString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) name = name.substring(dot + 1);
            if (!IGNORE_ANNOTATIONS.contains(name)) {
                sb.append(indent).append(ann.toString().trim()).append("\n");
            }
        }
    }

    /**
     * 提取 Javadoc 第一行有效业务描述，其余废话参数全部剪除
     */
    private static Optional<String> getCompactDoc(Node node) {
        return node.getComment().flatMap(c -> {
            if (c instanceof JavadocComment jc) {
                String clean = CLEAN_DOC_PATTERN.matcher(jc.getContent()).replaceAll("").trim();
                for (String line : clean.split("\\R")) {
                    String t = line.replaceAll("[/*]", "").trim();
                    if (!t.isBlank() && !t.startsWith("@")) {
                        return Optional.of(t);
                    }
                }
            }
            return Optional.empty();
        });
    }
}