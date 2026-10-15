package com.nf121.examplemod.command.data;

import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class EnumScanner {
    private EnumScanner() {
    }

    public static Map<Class<?>, Set<String>> scanEnums() {
        Map<Class<?>, Set<String>> result = new LinkedHashMap<>();
        Set<String> classNames = new HashSet<>();

        // 收集来自所有模组 Jar 及当前运行环境的类路径
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 1. 扫描所有已加载的 Mod 文件
        if (ModList.get() != null) {
            for (IModFileInfo modFile : ModList.get().getModFiles()) {
                Path filePath = modFile.getFile().getSecureJar().getRootPath();
                scanClassNamesInPath(filePath, classNames);
            }
        }

        // 2. 补充扫描原版/运行时类路径 (java.class.path)
        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry.endsWith(".jar") || new File(entry).isDirectory()) {
                try {
                    Path path = Path.of(entry);
                    if (Files.exists(path)) {
                        scanClassNamesInPath(path, classNames);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 3. 反射加载并验证枚举
        for (String className : classNames) {
            // 过滤明显无关的底层类，加速扫描
            if (isExcluded(className)) {
                continue;
            }

            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                if (clazz.isEnum() && !clazz.isAnonymousClass() && Modifier.isPublic(clazz.getModifiers())) {
                    Set<String> values = extractEnumValues(clazz);
                    if (!values.isEmpty()) {
                        result.put(clazz, values);
                    }
                }
            } catch (Throwable ignored) {
                // 忽略缺少可选依赖类导致的 NoClassDefFoundError
            }
        }

        return result;
    }

    private static boolean isExcluded(String className) {
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("jdk.") ||
               className.startsWith("sun.") ||
               className.startsWith("org.objectweb.asm.") ||
               className.startsWith("com.google.gson.") ||
               className.startsWith("org.apache.") ||
               className.startsWith("io.netty.");
    }

    private static void scanClassNamesInPath(Path path, Set<String> output) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.filter(p -> p.toString().endsWith(".class"))
                          .forEach(p -> {
                              String rel = path.relativize(p).toString();
                              output.add(toClassName(rel));
                          });
                }
            } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                URI uri = URI.create("jar:" + path.toUri());
                try (FileSystem fs = getOrCreateFileSystem(uri)) {
                    for (Path root : fs.getRootDirectories()) {
                        try (Stream<Path> stream = Files.walk(root)) {
                            stream.filter(p -> p.toString().endsWith(".class"))
                                  .forEach(p -> {
                                      String rel = p.toString();
                                      if (rel.startsWith("/")) {
                                          rel = rel.substring(1);
                                      }
                                      output.add(toClassName(rel));
                                  });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static FileSystem getOrCreateFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (Exception e) {
            return FileSystems.newFileSystem(uri, Collections.emptyMap());
        }
    }

    private static String toClassName(String pathString) {
        return pathString.replace('/', '.')
                         .replace('\\', '.')
                         .replaceAll("\\.class$", "");
    }

    private static Set<String> extractEnumValues(Class<?> clazz) {
        Set<String> values = new LinkedHashSet<>();
        Object[] enumConstants = clazz.getEnumConstants();
        if (enumConstants == null) {
            return values;
        }

        Method serializedNameMethod = null;
        try {
            serializedNameMethod = clazz.getMethod("getSerializedName");
        } catch (NoSuchMethodException ignored) {
        }

        for (Object constant : enumConstants) {
            String value = null;
            // 优先 1：如果实现了 StringRepresentable 接口
            if (constant instanceof StringRepresentable sr) {
                value = sr.getSerializedName();
            }
            // 优先 2：如果有公有的 getSerializedName() 方法
            if (value == null && serializedNameMethod != null) {
                try {
                    Object res = serializedNameMethod.invoke(constant);
                    if (res != null) {
                        value = res.toString();
                    }
                } catch (Exception ignored) {
                }
            }
            // 兜底 3：小写下划线名称
            if (value == null && constant instanceof Enum<?> e) {
                value = e.name().toLowerCase(Locale.ROOT);
            }

            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }
}