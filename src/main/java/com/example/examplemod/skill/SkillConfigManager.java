package com.example.examplemod.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

public class SkillConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("examplemod/skills.json");

    public static class BonusData {
        public String attribute;
        public double amount;
        public String operation = "ADD_VALUE"; // ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL
    }

    public static class NodeData {
        public String id;
        public int maxRank = 1;
        public float x;
        public float y;
        public List<String> parents = new ArrayList<>();
        public String spellId; // 可选法术 ID
        public List<BonusData> bonuses = new ArrayList<>();
    }

    public static void saveToFile(Collection<SkillNode> nodes) {
        List<NodeData> list = new ArrayList<>();
        for (SkillNode n : nodes) {
            NodeData data = new NodeData();
            data.id = n.getId();
            data.maxRank = n.getMaxRank();
            data.x = n.getX();
            data.y = n.getY();
            data.parents = new ArrayList<>(n.getParents());
            if (n.getSpell() != null) {
                data.spellId = n.getSpell().getSpellId();
            }
            for (SkillNode.AttributeBonus b : n.getBonuses()) {
                BonusData bd = new BonusData();
                bd.attribute = b.attributeName();
                bd.amount = b.amountPerRank();
                bd.operation = b.operation().name();
                data.bonuses.add(bd);
            }
            list.add(data);
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(list, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<NodeData> loadFromFile() {
        if (!Files.exists(CONFIG_PATH)) return Collections.emptyList();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            return GSON.fromJson(reader, new TypeToken<List<NodeData>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}