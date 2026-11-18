package com.example.examplemod.command.data;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class NameUtil {
    private NameUtil() {
    }

    public static String snakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('$', '_')
                .replace('-', '_')
                .replace('.', '_')
                .toLowerCase(Locale.ROOT);
    }

    public static Set<String> registryAliases(String registryId) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String path = registryId.substring(registryId.indexOf(':') + 1);

        aliases.add(snakeCase(path));

        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            aliases.add(snakeCase(path.substring(slash + 1)));
        }

        return aliases;
    }

    public static Set<String> enumAliases(Class<?> type) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();

        // 1. 短类名蛇形化 (例如: EquipmentSlot -> equipment_slot)
        aliases.add(snakeCase(type.getSimpleName()));

        // 2. 如果是内部枚举 (如 AttributeModifier$Operation -> operation, attribute_modifier_operation)
        Class<?> current = type;
        StringBuilder compositeName = new StringBuilder();
        while (current != null && current != Object.class) {
            if (compositeName.length() > 0) {
                compositeName.insert(0, "_");
            }
            compositeName.insert(0, current.getSimpleName());
            aliases.add(snakeCase(current.getSimpleName()));
            current = current.getEnclosingClass();
        }
        aliases.add(snakeCase(compositeName.toString()));

        return aliases;
    }
}