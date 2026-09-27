package com.example.examplemod.compat.l2hostility;

import com.example.examplemod.ExampleMod;
import dev.xkmc.l2hostility.init.registrate.LHBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class SpellTraitItemRegistrar {
    private static final boolean DEBUG = Boolean.getBoolean("examplemod.spelltrait.debug");

    private static final Map<ResourceLocation, Item> REGISTERED = new LinkedHashMap<>();

    private static final Path DEBUG_FILE =
            Paths.get("spelltrait_debug.txt").toAbsolutePath();

    private static final List<String> DEBUG_LINES = new ArrayList<>();


    private static void dbg(String line) {
        if (!DEBUG) return;                         DEBUG_LINES.add(line);
    }

    private static void flushDebug() {
        try (BufferedWriter w = Files.newBufferedWriter(DEBUG_FILE)) {
            w.write("=== SpellTraitItemRegistrar DEBUG ===\n");
            w.write("时间: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            w.write("文件: " + DEBUG_FILE + "\n\n");
            for (String l : DEBUG_LINES) {
                w.write(l);
                w.write("\n");
            }
        } catch (IOException e) {
            System.err.println("[SpellTraitItem] 写调试文件失败: " + e);
        }
    }

    @SubscribeEvent
    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;

        dbg("=== onRegisterItems 被调用 ===");
        dbg("WHITELIST 大小 = " + SpellTraitRegistrar.WHITELIST.size());

        int ok = 0, fail = 0, nullId = 0;
        List<String> okIds = new ArrayList<>();
        List<String> failIds = new ArrayList<>();

        for (String idStr : SpellTraitRegistrar.WHITELIST) {
            ResourceLocation itemId = SpellTraitRegistrar.traitIdOf(idStr);
            if (itemId == null) {
                dbg("  [NULL] traitIdOf 返回 null: " + idStr);
                nullId++;
                continue;
            }

            if (REGISTERED.containsKey(itemId)) {
                dbg("  [DUP] itemId 已被注册过: " + itemId);
                fail++;
                failIds.add(itemId + "  <-- DUP (来自 " + idStr + ")");
                continue;
            }

            try {
                final String capturedId = idStr;
                event.register(Registries.ITEM, itemId, () -> {
                    SpellTraitSymbol symbol = new SpellTraitSymbol(new Item.Properties());
                    REGISTERED.put(itemId, symbol);
                    dbg("  [OK-SUPPLIER] supplier 执行: " + itemId + "  (来自 " + capturedId + ")");
                    return symbol;
                });
                dbg("  [OK-REG] 注册成功: " + itemId);
                ok++;
                okIds.add(String.valueOf(itemId));
            } catch (Throwable t) {
                dbg("  [FAIL] 注册异常: " + itemId + " -> " + t);
                fail++;
                failIds.add(itemId + "  <-- " + t);
            }
        }

        dbg("");
        dbg("=== 汇总 ===");
        dbg("WHITELIST 总数 = " + SpellTraitRegistrar.WHITELIST.size());
        dbg("成功 ok = " + ok);
        dbg("失败 fail = " + fail);
        dbg("id 为 null = " + nullId);
        dbg("REGISTERED map 大小 = " + REGISTERED.size());
        dbg("");
        dbg("--- 成功的 id ---");
        for (String s : okIds) dbg("  " + s);
        dbg("");
        dbg("--- 失败的 id ---");
        for (String s : failIds) dbg("  " + s);

        flushDebug();
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(LHBlocks.TAB.key())) return;

        dbg("");
        dbg("=== onBuildCreativeTab 被调用 ===");
        dbg("REGISTERED map 大小 = " + REGISTERED.size());

        int added = 0;
        for (Item item : REGISTERED.values()) {
            event.accept(new ItemStack(item));
            added++;
        }
        dbg("加入创造栏数量 = " + added);

        flushDebug();
    }
}