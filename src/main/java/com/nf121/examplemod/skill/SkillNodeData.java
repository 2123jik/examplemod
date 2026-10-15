package com.nf121.examplemod.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SkillNodeData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("examplemod/skills.json");

    public String id;
    public int maxRank = 1;
    public float x;
    public float y;
    public List<String> parents = new ArrayList<>();
    public String spellId;
    public String icon;
    public List<BonusEntry> bonuses = new ArrayList<>();

    public static class BonusEntry {
        public String attribute;
        public double amount;
        public String operation = "ADD_VALUE";
    }

    public static SkillNode toNode(SkillNodeData data) {
        SkillNode node = new SkillNode(data.id, data.maxRank);
        node.setPosition(data.x, data.y);
        node.getParents().addAll(data.parents);

        if (data.spellId != null && !data.spellId.isBlank()) {
            AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(data.spellId));
            if (spell != null) {
                node.setSpell(spell);
            }
        }
        if (data.icon != null && !data.icon.isBlank()) {
            node.setIcon(ResourceLocation.parse(data.icon));
        }

        if (data.bonuses != null) {
            for (BonusEntry b : data.bonuses) {
                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(b.operation);
                node.addBonus(b.attribute, b.amount, op);
            }
        }
        return node;
    }

    public static SkillNodeData fromNode(SkillNode node) {
        SkillNodeData data = new SkillNodeData();
        data.id = node.getId();
        data.maxRank = node.getMaxRank();
        data.x = node.getX();
        data.y = node.getY();
        data.parents = new ArrayList<>(node.getParents());

        if (node.getSpell() != null) {
            data.spellId = node.getSpell().getSpellId();
        }
        if (node.getIcon() != null) {
            data.icon = node.getIcon().toString();
        }

        for (SkillNode.AttributeBonus b : node.getBonuses()) {
            BonusEntry entry = new BonusEntry();
            entry.attribute = b.attributeId().toString();
            entry.amount = b.amountPerRank();
            entry.operation = b.operation().name();
            data.bonuses.add(entry);
        }
        return data;
    }

    public static void saveAll(Collection<SkillNode> nodes) {
        List<SkillNodeData> list = new ArrayList<>();
        for (SkillNode node : nodes) {
            list.add(fromNode(node));
        }
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(list, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<SkillNode> loadAll() {
        if (!Files.exists(CONFIG_FILE)) {
            return Collections.emptyList();
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            List<SkillNodeData> list = GSON.fromJson(reader, new TypeToken<List<SkillNodeData>>(){}.getType());
            if (list == null) return Collections.emptyList();
            List<SkillNode> result = new ArrayList<>();
            for (SkillNodeData d : list) {
                result.add(toNode(d));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}