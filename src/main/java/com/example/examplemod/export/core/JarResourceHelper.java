package com.example.examplemod.export.core;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 安全扫描所有 Mod 根目录和 Classpath，避免误关闭共享的 ZipFileSystem。
 */
public final class JarResourceHelper {

    private JarResourceHelper() {
    }

    public static void visitAllModAndClasspathRoots(Consumer<Path> rootVisitor) {
        Set<Path> visited = new LinkedHashSet<>();

        if (ModList.get() != null) {
            for (IModFileInfo modFile : ModList.get().getModFiles()) {
                Path root = modFile.getFile().getSecureJar().getRootPath();
                if (visited.add(root)) {
                    rootVisitor.accept(root);
                }
            }
        }

        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry.isBlank()) continue;
            if (!entry.endsWith(".jar") && !new File(entry).isDirectory()) continue;

            try {
                Path path = Path.of(entry);
                if (!Files.exists(path)) continue;

                if (Files.isDirectory(path)) {
                    if (visited.add(path)) {
                        rootVisitor.accept(path);
                    }
                } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                    visitJarSafely(path, fs -> {
                        for (Path root : fs.getRootDirectories()) {
                            rootVisitor.accept(root);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void visitJarSafely(Path jarPath, Consumer<FileSystem> consumer) {
        URI uri = URI.create("jar:" + jarPath.toUri());
        FileSystem fs = null;
        boolean createdByUs = false;
        try {
            try {
                fs = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                createdByUs = true;
            }
            consumer.accept(fs);
        } catch (Exception ignored) {
        } finally {
            if (createdByUs && fs != null) {
                try {
                    fs.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}