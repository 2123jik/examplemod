package com.example.examplemod.compat.l2hostility.traits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@EventBusSubscriber(modid = "examplemod")
public class SpellTraitGenCommand {

    /** 开发环境默认路径：项目根/src/main/resources/data/examplemod/data_maps/l2hostility/trait */
    private static final String DEFAULT_OUT =
            "../src/main/resources/data/l2hostility/data_maps/l2hostility/trait";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("spelltrait")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("gen")
                        .executes(ctx -> gen(ctx.getSource(), DEFAULT_OUT))
                        .then(Commands.argument("outDir", StringArgumentType.greedyString())
                                .executes(ctx -> gen(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "outDir"))))));
    }

    private static int gen(CommandSourceStack src, String outDirStr) {
        Path outDir = Paths.get(outDirStr).toAbsolutePath();
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            src.sendFailure(Component.literal("创建目录失败: " + e));
            return 0;
        }

        JsonObject values = new JsonObject();
        int ok = 0, fail = 0;

        for (String idStr : SpellTraitRegistrar.WHITELIST) {
            ResourceLocation spellId = ResourceLocation.tryParse(idStr);
            if (spellId == null) { fail++; continue; }

            AbstractSpell spell = SpellRegistry.REGISTRY.get(spellId);
            if (spell == null || !spell.isEnabled()) { fail++; continue; }

            ResourceLocation traitId = SpellTraitRegistrar.traitIdOf(idStr);
            if (traitId == null) { fail++; continue; }

            try {
                values.add(traitId.toString(), buildConfig(spell));
                ok++;
            } catch (Throwable t) {
                fail++;
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        root.add("values", values);

        Path file = outDir.resolve("trait_data.json");
        try {
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            src.sendFailure(Component.literal("写文件失败: " + e));
            return 0;
        }

        int fOk = ok, fFail = fail;
        src.sendSuccess(() -> Component.literal(
                "生成完成: 成功 " + fOk + "，失败 " + fFail + "\n文件: " + file), true);
        return ok;
    }

    private static JsonObject buildConfig(AbstractSpell spell) {
        int rarity = spell.getRarity(1).getValue();

        // —— 数值算法：想改平衡就改这里 ——
        int minLevel = switch (rarity) {
            case 0 -> 20;
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 200;
            default -> 400;
        };
        int weight  = Math.max(10, 100 - rarity * 15);
        int maxRank = Math.max(1, spell.getMaxLevel());

        int rawMana = spell.getManaCost(1);
        boolean continuous = spell.getCastType() == CastType.CONTINUOUS;
        double effectiveMana = continuous ? rawMana * 20.0 : Math.max(10, rawMana);
        int cost = (int) Math.round(
                Math.sqrt(effectiveMana)
                        * (1.0 + rarity * 0.4)
                        * (1.0 + Math.sqrt(maxRank) * 0.4));
        if (cost < 1) cost = 1;

        JsonObject o = new JsonObject();
        o.addProperty("cost", cost);
        o.addProperty("weight", weight);
        o.addProperty("max_rank", maxRank);
        o.addProperty("min_level", minLevel);
        return o;
    }
}