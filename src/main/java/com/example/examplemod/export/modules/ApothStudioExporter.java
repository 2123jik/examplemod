package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public final class ApothStudioExporter {

    private ApothStudioExporter() {
    }

    public static Path export(HolderLookup.Provider registries) {
        JsonObject root = new JsonObject();
        DynamicOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        JsonArray categoriesArr = new JsonArray();
        for (LootCategory cat : Apoth.BuiltInRegs.LOOT_CATEGORY) {
            if (!cat.isNone()) {
                JsonObject catObj = new JsonObject();
                catObj.addProperty("id", cat.getKey().toString());
                catObj.addProperty("desc", cat.getDescId());
                categoriesArr.add(catObj);
            }
        }
        root.add("categories", categoriesArr);

        JsonArray raritiesArr = new JsonArray();
        for (LootRarity rarity : RarityRegistry.INSTANCE.getValues()) {
            ResourceLocation key = RarityRegistry.INSTANCE.getKey(rarity);
            if (key != null) {
                raritiesArr.add(key.toString());
            }
        }
        root.add("rarities", raritiesArr);

        JsonObject affixesObj = new JsonObject();
        for (Affix affix : AffixRegistry.INSTANCE.getValues()) {
            try {
                @SuppressWarnings("unchecked")
                Codec<Affix> codec = (Codec<Affix>) affix.getCodec();
                DataResult<JsonElement> result = codec.encodeStart(ops, affix);
                result.ifSuccess(elem -> {
                    if (elem.isJsonObject()) {
                        elem.getAsJsonObject().addProperty("_affix_type", String.valueOf(affix.definition().type()));
                    }
                    affixesObj.add(affix.id().toString(), elem);
                });
            } catch (Exception ignored) {
            }
        }
        root.add("affixes", affixesObj);

        JsonObject gemsObj = new JsonObject();
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            try {
                DataResult<JsonElement> result = Gem.CODEC.encodeStart(ops, gem);
                result.ifSuccess(elem -> gemsObj.add(gem.getId().toString(), elem));
            } catch (Exception ignored) {
            }
        }
        root.add("gems", gemsObj);

        try {
            return ExportIO.writeJson(ExportIO.resolve("apoth_studio_data.json"), root);
        } catch (Exception e) {
            throw new RuntimeException("导出 ApothStudio 数据失败: " + e.getMessage(), e);
        }
    }
}