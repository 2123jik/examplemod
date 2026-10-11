package com.example.examplemod.command.data;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class NameUtil {
    private NameUtil() {
    }

    static String snakeCase(String input) {
        return input
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .replace('$', '_')
            .replace('-', '_')
            .toLowerCase(Locale.ROOT);
    }

    static Set<String> registryAliases(String registryId) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();

        String path = registryId.substring(
            registryId.indexOf(':') + 1
        );

        aliases.add(snakeCase(path));

        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            aliases.add(snakeCase(path.substring(slash + 1)));
        }

        return aliases;
    }

    static Set<String> enumAliases(Class<?> type) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();

        Class<?> current = type;
        while (current != null) {
            aliases.add(snakeCase(current.getSimpleName()));
            current = current.getEnclosingClass();
        }

        return aliases;
    }
}
