package com.example.examplemod.export.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExportIO {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ExportIO() {
    }

    public static Path exportDir() {
        return FMLPaths.GAMEDIR.get().resolve("exports");
    }

    public static Path resolve(String... subPaths) {
        Path path = exportDir();
        for (String sub : subPaths) {
            path = path.resolve(sub);
        }
        return path.toAbsolutePath().normalize();
    }

    public static Path writeText(Path target, String content) throws IOException {
        Path abs = target.toAbsolutePath().normalize();
        if (abs.getParent() != null) {
            Files.createDirectories(abs.getParent());
        }
        Files.writeString(abs, content, StandardCharsets.UTF_8);
        return abs;
    }

    public static Path writeJson(Path target, Object jsonElement) throws IOException {
        return writeText(target, GSON.toJson(jsonElement));
    }

    public static String stripColorAndCleanCell(String text) {
        if (text == null) return "";
        String stripped = ChatFormatting.stripFormatting(text);
        if (stripped == null) return "";
        return stripped.replace("\r", " ")
                .replace("\n", " ")
                .replace("|", "\\|")
                .trim();
    }

    public static String stripColor(String text) {
        if (text == null) return "";
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped != null ? stripped : "";
    }
}