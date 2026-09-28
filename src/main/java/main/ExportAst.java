package main;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ExportAst {

    // ====== 可裁剪的注解 ======
    static final Set<String> STRIP_ANNOTATIONS = Set.of(
        "Override", "SuppressWarnings", "Deprecated", "FunctionalInterface", "SafeVarargs"
    );

    // ====== 日志识别 ======
    static final Set<String> LOG_METHODS = Set.of("trace", "debug", "info", "warn", "error");
    static final Set<String> LOG_FIELDS  = Set.of("LOGGER", "LOG", "logger", "log");

    // ====== 缩写表（顺序敏感：长组合在前） ======
    static final Map<String, String> ABBR = new LinkedHashMap<>();
    static {
        ABBR.put("public static final", "psf");
        ABBR.put("public static",       "ps");
        ABBR.put("private static",      "prs");
        ABBR.put("protected static",    "pts");
        ABBR.put("public final",        "pf");
        ABBR.put("private final",       "prf");
        ABBR.put("protected final",     "ptf");
        ABBR.put("public",              "p");
        ABBR.put("private",             "pr");
        ABBR.put("protected",           "pt");
        ABBR.put("static",              "s");
        ABBR.put("final",               "f");
        ABBR.put("abstract",            "a");
        ABBR.put("class",               "c");
        ABBR.put("interface",           "i");
        ABBR.put("enum",                "en");
        ABBR.put("record",              "rec");
        ABBR.put("extends",             "e");
        ABBR.put("implements",          "im");
        ABBR.put("void",                "v");
        ABBR.put("return",              "r");
        ABBR.put("new",                 "n");
        ABBR.put("this",                "t");
        ABBR.put("super",               "sup");
        ABBR.put("true",                "T");
        ABBR.put("false",               "F");
        ABBR.put("else",                "el");
        ABBR.put("while",               "wh");
        ABBR.put("catch",               "cat");
        ABBR.put("throw",               "thr");
        ABBR.put("throws",              "ths");
        ABBR.put("boolean",             "bool");
        ABBR.put("String",              "Str");
    }

    static final boolean COMPRESS = true;
    static final boolean DROP_BLANK_LINES = true;

    public static void main(String[] args) throws IOException {
        String srcDir = args.length > 0 ? args[0]
                : "C:\\mod\\Apotheosis-1.21\\src\\main\\java";
        String outFile = args.length > 1 ? args[1]
                : srcDir+"\\code.txt";

        Path src = Paths.get(srcDir);
        Path out = Paths.get(outFile);

        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(false); // 不保留注释

        JavaParser parser = new JavaParser(config);

        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        int ok = 0, fail = 0;

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writeDict(w);

            for (Path f : files) {
                String rel = src.relativize(f).toString().replace('\\', '/');
                try {
                    ParseResult<CompilationUnit> result = parser.parse(f);
                    if (!result.isSuccessful() || result.getResult().isEmpty()) {
                        w.write("// [PARSE FAILED] " + rel + "\n");
                        fail++;
                        continue;
                    }
                    CompilationUnit cu = result.getResult().get();

                    // 1) 移除所有 import
                    cu.getImports().clear();

                    // 2) 遍历 AST，删注解 + 删日志语句
                    cu.accept(new ModifierVisitor<Void>() {
                        @Override public Visitable visit(com.github.javaparser.ast.body.MethodDeclaration md, Void a) {
                            stripAnn(md); return super.visit(md, a);
                        }
                        @Override public Visitable visit(com.github.javaparser.ast.body.ConstructorDeclaration cd, Void a) {
                            stripAnn(cd); return super.visit(cd, a);
                        }
                        @Override public Visitable visit(com.github.javaparser.ast.body.FieldDeclaration fd, Void a) {
                            stripAnn(fd); return super.visit(fd, a);
                        }
                        @Override public Visitable visit(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration cd, Void a) {
                            stripAnn(cd); return super.visit(cd, a);
                        }
                        @Override public Visitable visit(com.github.javaparser.ast.body.EnumDeclaration ed, Void a) {
                            stripAnn(ed); return super.visit(ed, a);
                        }
                        @Override public Visitable visit(com.github.javaparser.ast.body.RecordDeclaration rd, Void a) {
                            stripAnn(rd); return super.visit(rd, a);
                        }
                        @Override public Visitable visit(ExpressionStmt stmt, Void a) {
                            if (isLogStatement(stmt)) return null;
                            return super.visit(stmt, a);
                        }
                    }, null);

                    // 3) 打印
                    String printed = cu.toString();

                    // 4) 去空行 + 关键字压缩
                    if (DROP_BLANK_LINES) {
                        printed = printed.replaceAll("(?m)^[ \\t]*\\R", "");
                    }
                    if (COMPRESS) {
                        printed = compress(printed);
                    }

                    w.write("// ==================================================\n");
                    w.write("// File: " + rel + "\n");
                    w.write("// ==================================================\n");
                    w.write(printed);
                    if (!printed.endsWith("\n")) w.write("\n");
                    w.write("\n");
                    ok++;
                } catch (Exception ex) {
                    w.write("// [ERROR] " + rel + " -> " + ex + "\n");
                    fail++;
                }
            }

            w.write("\n// Done. ok=" + ok + ", fail=" + fail + "\n");
        }

        System.out.println("导出完成: " + out.toAbsolutePath());
        System.out.println("ok=" + ok + ", fail=" + fail);
    }

    // ---- 注解裁剪 ----
    static void stripAnn(Node node) {
        if (!(node instanceof BodyDeclaration<?> bd)) return;
        bd.getAnnotations().removeIf(a -> {
            String name = a.getNameAsString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) name = name.substring(dot + 1);
            return STRIP_ANNOTATIONS.contains(name);
        });
    }

    // ---- 日志识别 ----
    static boolean isLogStatement(ExpressionStmt stmt) {
        if (!(stmt.getExpression() instanceof MethodCallExpr call)) return false;
        if (!LOG_METHODS.contains(call.getNameAsString())) return false;

        Optional<Expression> scope = call.getScope();
        if (scope.isEmpty()) return false;

        Expression s = scope.get();
        if (s instanceof NameExpr ne)  return LOG_FIELDS.contains(ne.getNameAsString());
        if (s instanceof FieldAccessExpr fa) return LOG_FIELDS.contains(fa.getNameAsString());
        return false;
    }

    // ---- 关键字压缩（保护字符串字面量） ----
    static String compress(String printed) {
        // 保护字符串
        Pattern strPat = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
        Matcher m = strPat.matcher(printed);
        List<String> strings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            strings.add(m.group());
            m.appendReplacement(sb, Matcher.quoteReplacement("\u0000S" + (strings.size() - 1) + "\u0000"));
        }
        m.appendTail(sb);
        String text = sb.toString();

        // 组合在前，单字在后
        for (Map.Entry<String, String> e : ABBR.entrySet()) {
            String[] parts = e.getKey().split(" ");
            String pat;
            if (parts.length == 1) {
                pat = "\\b" + Pattern.quote(parts[0]) + "\\b";
            } else {
                StringBuilder pb = new StringBuilder("\\b");
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) pb.append("\\s+");
                    pb.append(Pattern.quote(parts[i]));
                }
                pb.append("\\b");
                pat = pb.toString();
            }
            text = text.replaceAll(pat, Matcher.quoteReplacement(e.getValue()));
        }

        // 还原字符串
        for (int i = 0; i < strings.size(); i++) {
            text = text.replace("\u0000S" + i + "\u0000", strings.get(i));
        }
        return text;
    }

    // ---- 字典头 ----
    static void writeDict(BufferedWriter w) throws IOException {
        w.write("// === AST 压缩导出 ===\n");
        w.write("// 缩写: p=public pr=private pt=protected s=static f=final a=abstract\n");
        w.write("//       c=class i=interface en=enum rec=record e=extends im=implements\n");
        w.write("//       v=void r=return n=new t=this sup=super T=true F=false\n");
        w.write("//       el=else wh=while cat=catch thr=throw ths=throws\n");
        w.write("//       bool=boolean Str=String\n");
        w.write("// 组合: ps=public static  prs=private static  psf=public static final\n");
        w.write("//       pf=public final  prf=private final  ptf=protected final\n");
        w.write("//       pts=protected static\n");
        w.write("// 已移除: import、注释、@Override、@SuppressWarnings、@Deprecated、\n");
        w.write("//         LOGGER.debug/info/warn/error 等日志语句。\n");
        w.write("// package 保留。\n\n");
    }
}