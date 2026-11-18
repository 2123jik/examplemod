package com.example.examplemod.runeword;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record RunewordDefinition(
        String id,
        String nameEn,
        String nameCn,
        int socketsRequired,
        int allSpellLevels,
        List<String> validBaseTags,
        List<ResourceLocation> gemSequence,
        List<AttrMod> attributes,
        List<TagKey<Item>> cachedTags
) {
    public record AttrMod(ResourceLocation attribute, double amount, String operation) {}

    public static RunewordDefinition fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String nameEn = json.get("name_en").getAsString();
        String nameCn = json.get("name_cn").getAsString();
        int sockets = json.get("sockets_required").getAsInt();
        int allSpellLevels = json.has("all_spell_levels") ? json.get("all_spell_levels").getAsInt() : 0;

        List<String> tags = new ArrayList<>();
        List<TagKey<Item>> cachedTagKeys = new ArrayList<>();
        json.getAsJsonArray("valid_base_tags").forEach(e -> {
            String str = e.getAsString();
            tags.add(str);
            if (str.startsWith("#")) {
                cachedTagKeys.add(TagKey.create(Registries.ITEM, ResourceLocation.parse(str.substring(1))));
            }
        });

        List<ResourceLocation> seq = new ArrayList<>();
        json.getAsJsonArray("sequence").forEach(e -> seq.add(ResourceLocation.parse(e.getAsString())));

        List<AttrMod> attrs = new ArrayList<>();
        json.getAsJsonArray("attributes").forEach(e -> {
            JsonObject o = e.getAsJsonObject();
            attrs.add(new AttrMod(
                    ResourceLocation.parse(o.get("attribute").getAsString()),
                    o.get("amount").getAsDouble(),
                    o.get("operation").getAsString()
            ));
        });

        return new RunewordDefinition(id, nameEn, nameCn, sockets, allSpellLevels, tags, seq, attrs, cachedTagKeys);
    }

    public boolean matchesBaseItem(ItemStack stack) {
        for (TagKey<Item> tag : cachedTags) {
            if (stack.is(tag)) return true;
        }
        ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return validBaseTags.contains(itemLoc.toString());
    }
}